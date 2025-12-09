package org.itmowork.vacancy_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.itmowork.vacancy_service.dto.request.VacancyCreateRequestDto;
import org.itmowork.vacancy_service.dto.request.VacancyUpdateRequestDto;
import org.itmowork.vacancy_service.dto.response.VacancyResponseDto;
import org.itmowork.vacancy_service.exception.exceptions.*;
import org.itmowork.vacancy_service.infrastructure.feign.CompanyClient;
import org.itmowork.vacancy_service.mappers.VacancyMapper;
import org.itmowork.vacancy_service.model.Currency;
import org.itmowork.vacancy_service.model.Vacancy;
import org.itmowork.vacancy_service.model.VacancyStatus;
import org.itmowork.vacancy_service.model.VacancyStatusName;
import org.itmowork.vacancy_service.repository.VacancyRepository;
import org.itmowork.vacancy_service.service.interfaces.CurrencyService;
import org.itmowork.vacancy_service.service.interfaces.VacancyService;
import org.itmowork.vacancy_service.service.interfaces.VacancyStatusService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VacancyServiceImpl implements VacancyService {

    private final VacancyRepository vacancyRepository;
    private final VacancyStatusService vacancyStatusService;
    private final CurrencyService currencyService;
    private final CompanyClient companyClient;

    private final VacancyMapper vacancyMapper;

    private static final Map<VacancyStatusName, Set<VacancyStatusName>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            VacancyStatusName.DRAFT, Set.of(VacancyStatusName.PUBLISHED, VacancyStatusName.CLOSED),
            VacancyStatusName.PUBLISHED, Set.of(VacancyStatusName.DRAFT, VacancyStatusName.CLOSED)
    );

    @Override
    public Page<VacancyResponseDto> getAllPublishedVacancies(Pageable pageable) {
        Page<Vacancy> vacancies = vacancyRepository.getAllPublished(pageable);

        return vacancies.map(v -> new VacancyResponseDto(
                v.getId(),
                v.getTitle(),
                v.getDescription(),
                v.getSalaryFrom(),
                v.getSalaryTo(),
                v.getStatus().getId(),
                v.getCompanyId(),
                v.getCurrency().getId()
        ));
    }

    @Override
    public VacancyResponseDto createVacancy(
            VacancyCreateRequestDto request,
            VacancyStatusName statusName,
            UUID userId
    ) {
        Boolean exists = companyClient.existsCompany(request.companyId());
        if (exists == null || !exists) {
            throw new CompanyNotFoundException("Company with id " + request.companyId() + " does not exist");
        }

        Boolean owns = companyClient.validateCompanyOwnership(request.companyId(), userId);
        if (owns == null || !owns) {
            throw new CompanyNotFoundException("User does not own this company");
        }

        Currency currency = currencyService.findCurrencyById(request.currencyId());
        if (currency == null) {
            throw new CurrencyNotFoundException("Currency with id " + request.currencyId() + " not found");
        }

        VacancyStatus vacancyStatus =
                vacancyStatusService.findByVacancyStatusName(statusName);

        validateSalaryBounds(request.salaryFrom(), request.salaryTo());

        Vacancy vacancy = Vacancy.builder()
                .title(request.title())
                .description(request.description())
                .salaryFrom(request.salaryFrom())
                .salaryTo(request.salaryTo())
                .createdAt(LocalDateTime.now())
                .companyId(request.companyId())
                .status(vacancyStatus)
                .currency(currency)
                .build();

        Vacancy saved = vacancyRepository.save(vacancy);
        return buildResponse(saved);
    }

    @Override
    public VacancyResponseDto changeStatus(UUID userId, UUID vacancyId, VacancyStatusName newStatus) {

        Vacancy vacancy = getAndValidateVacancy(vacancyId);
        UUID companyId = vacancy.getCompanyId();

        Boolean exists = companyClient.existsCompany(companyId);
        if (exists == null || !exists) {
            throw new CompanyNotFoundException("Company id not found: " + companyId);
        }

        Boolean owns = companyClient.validateCompanyOwnership(companyId, userId);
        if (owns == null || !owns) {
            throw new CompanyNotFoundException("User does not own this company");
        }

        VacancyStatusName currentStatus = vacancy.getStatus().getVacancyStatusName();

        Set<VacancyStatusName> allowedNextStatuses =
                ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowedNextStatuses.contains(newStatus)) {
            throw new InvalidVacancyStatusChangeException(
                    "Impossible to change status from " + currentStatus + " to " + newStatus
            );
        }

        VacancyStatus statusEntity = vacancyStatusService.findByVacancyStatusName(newStatus);
        vacancy.setStatus(statusEntity);
        validateSalaryBounds(vacancy.getSalaryFrom(), vacancy.getSalaryTo());
        Vacancy saved = vacancyRepository.save(vacancy);

        return buildResponse(saved);
    }

    @Override
    public VacancyResponseDto updateVacancy(UUID userId, UUID id, VacancyUpdateRequestDto dto) {

        Vacancy vacancy = getAndValidateVacancy(id);
        UUID companyId = vacancy.getCompanyId();

        Boolean exists = companyClient.existsCompany(companyId);
        if (exists == null || !exists) {
            throw new CompanyNotFoundException("Company not found: " + companyId);
        }

        Boolean owns = companyClient.validateCompanyOwnership(companyId, userId);
        if (!owns) {
            throw new CompanyNotFoundException("User does not own this company");
        }

        VacancyStatusName status = vacancy.getStatus().getVacancyStatusName();
        if (status != VacancyStatusName.DRAFT && status != VacancyStatusName.PUBLISHED) {
            throw new InvalidVacancyStatusException(
                    "Update allowed only for DRAFT or PUBLISHED vacancies"
            );
        }

        vacancyMapper.update(vacancy, dto);

        if (dto.currencyId() != null) {
            Currency currency = currencyService.findCurrencyById(dto.currencyId());
            if (currency == null) {
                throw new CurrencyNotFoundException("Currency with id=" + dto.currencyId() + " not found");
            }
            vacancy.setCurrency(currency);
        }

        validateSalaryBounds(vacancy.getSalaryFrom(), vacancy.getSalaryTo());
        Vacancy saved = vacancyRepository.save(vacancy);
        return buildResponse(saved);
    }

    @Override
    @Transactional
    public VacancyResponseDto updateAndChangeStatus(
            UUID userId,
            UUID vacancyId,
            VacancyUpdateRequestDto dto,
            VacancyStatusName newStatus
    ) {
        Vacancy vacancy = getAndValidateVacancy(vacancyId);
        UUID companyId = vacancyRepository.findCompanyId(vacancyId);

        Boolean companyExists = companyClient.existsCompany(companyId);
        if (companyExists == null || !companyExists) {
            throw new CompanyNotFoundException("Company id not found: " + companyId);
        }

        Boolean owns = companyClient.validateCompanyOwnership(companyId, userId);
        if (owns == null || !owns) {
            throw new CompanyNotFoundException("User does not own this company");
        }

        VacancyStatusName currentStatus = vacancy.getStatus().getVacancyStatusName();

        if (!(currentStatus == VacancyStatusName.DRAFT || currentStatus == VacancyStatusName.PUBLISHED)) {
            throw new InvalidVacancyStatusException(
                    "Update available only for DRAFT or PUBLISHED vacancies"
            );
        }

        boolean allowedTransition =
                (currentStatus == VacancyStatusName.DRAFT && newStatus == VacancyStatusName.PUBLISHED) ||
                        (currentStatus == VacancyStatusName.PUBLISHED && newStatus == VacancyStatusName.DRAFT);

        if (!allowedTransition) {
            throw new InvalidVacancyStatusChangeException(
                    "Impossible to change status from " + currentStatus + " to " + newStatus
            );
        }

        vacancyMapper.update(vacancy, dto);

        if (dto.currencyId() != null) {
            Currency currency = currencyService.findCurrencyById(dto.currencyId());
            vacancy.setCurrency(currency);
        }

        validateSalaryBounds(vacancy.getSalaryFrom(), vacancy.getSalaryTo());
        VacancyStatus statusEntity = vacancyStatusService.findByVacancyStatusName(newStatus);
        vacancy.setStatus(statusEntity);
        Vacancy saved = vacancyRepository.save(vacancy);

        return buildResponse(saved);
    }

    @Override
    public Vacancy getReferenceById(UUID vacancyId) {
        return vacancyRepository.getReferenceById(vacancyId);
    }

    @Override
    public boolean existsVacancyById(UUID id) {
        return vacancyRepository.existsVacanciesById(id);
    }

    @Override
    public VacancyStatus findCurrentVacancyStatusByVacancyId(UUID id) {
        Long vacancyStatusId = vacancyRepository.findVacancyStatusById(id);
        return vacancyStatusService.findVacancyStatusById(vacancyStatusId);
    }

    @Override
    public UUID findCompanyIdByVacancyId(UUID vacancyId) {
        UUID companyId = vacancyRepository.findCompanyId(vacancyId);

        if (companyId == null) {
            throw new VacancyNotFoundException(
                    "Vacancy with id=" + vacancyId + " not found"
            );
        }

        return companyId;
    }

    @Override
    public String getVacancyTitle(UUID vacancyId) {
        String title = vacancyRepository.findTitleById(vacancyId);
        if (title == null) {
            throw new VacancyNotFoundException("Vacancy with id=" + vacancyId + " not found");
        }
        return title;
    }

    @Override
    public boolean isVacancyPublished(UUID vacancyId) {
        Boolean published = vacancyRepository.isPublished(vacancyId);
        if (published == null) {
            throw new VacancyNotFoundException("Vacancy with id=" + vacancyId + " not found");
        }
        return published;
    }

    private void validateSalaryBounds(Integer salaryFrom, Integer salaryTo) {
        if (salaryFrom != null && salaryFrom < 0) {
            throw new InvalidVacancySalaryException("salary_from must be non-negative");
        }

        if (salaryTo != null && salaryTo < 0) {
            throw new InvalidVacancySalaryException("salary_to must be non-negative");
        }

        if (salaryFrom != null && salaryTo != null && salaryFrom > salaryTo) {
            throw new InvalidVacancySalaryException("salary_from cannot be greater than salary_to");
        }
    }

    private Vacancy getAndValidateVacancy(UUID vacancyId) {
        return vacancyRepository.findById(vacancyId)
                .orElseThrow(() ->
                        new VacancyNotFoundException("Vacancy with id=" + vacancyId + " not found"));
    }

    private VacancyResponseDto buildResponse(Vacancy saved) {
        return VacancyResponseDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .salaryFrom(saved.getSalaryFrom())
                .salaryTo(saved.getSalaryTo())
                .statusId(saved.getStatus().getId())
                .companyId(saved.getCompanyId())
                .currencyId(saved.getCurrency().getId())
                .build();
    }
}
