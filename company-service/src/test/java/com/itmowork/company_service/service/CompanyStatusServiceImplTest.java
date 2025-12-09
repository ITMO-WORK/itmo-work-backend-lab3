package com.itmowork.company_service.service;

import com.itmowork.company_service.exception.exceptions.CompanyStatusNotFoundException;
import com.itmowork.company_service.model.CompanyStatus;
import com.itmowork.company_service.model.CompanyStatusName;
import com.itmowork.company_service.repository.CompanyStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyStatusServiceImplTest {

    @Mock
    private CompanyStatusRepository companyStatusRepository;

    @InjectMocks
    private CompanyStatusServiceImpl companyStatusService;

    @Test
    void findCompanyStatusByCompanyStatusNameSuccess(){
        CompanyStatusName statusName = CompanyStatusName.PENDING_VERIFICATION;
        CompanyStatus expectedStatus = new CompanyStatus(1L, statusName);
        when(companyStatusRepository.findByStatus(statusName))
                .thenReturn(Mono.just(expectedStatus));

        StepVerifier.create(companyStatusService.findCompanyStatusByCompanyStatusName(statusName))
                .expectNextMatches(companyStatus ->
                        companyStatus.getStatus() == statusName &&
                                companyStatus.getId() == 1L
                )
                .verifyComplete();
    }

    @Test
    void findCompanyStatusByCompanyStatusNameNotFound() {
        CompanyStatusName statusName = CompanyStatusName.PENDING_VERIFICATION;

        when(companyStatusRepository.findByStatus(statusName))
                .thenReturn(Mono.empty());

        StepVerifier.create(companyStatusService.findCompanyStatusByCompanyStatusName(statusName))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(CompanyStatusNotFoundException.class);

                    CompanyStatusNotFoundException ex = (CompanyStatusNotFoundException) error;
                    assertThat(ex.getMessage())
                            .isEqualTo("Компания с таким статусом не найдена");

                })
                .verify();
    }
}
