package org.ilestegor.applicationservice.service;

import lombok.RequiredArgsConstructor;
import org.ilestegor.applicationservice.model.ApplicationStatus;
import org.ilestegor.applicationservice.model.ApplicationStatusName;
import org.ilestegor.applicationservice.repository.ApplicationStatusRepository;
import org.ilestegor.applicationservice.service.interfaces.ApplicationStatusService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ApplicationStatusServiceImpl implements ApplicationStatusService {
    private final ApplicationStatusRepository applicationStatusRepository;


    @Override
    public Mono<ApplicationStatus> findApplicationStatusByApplicationStatusId(Long applicationStatusId) {
        return applicationStatusRepository.findApplicationStatusById(applicationStatusId);
    }

    @Override
    public Mono<ApplicationStatus> findApplicationStatusByApplicationStatusName(ApplicationStatusName name) {
        return applicationStatusRepository.findByApplicationStatusName(name);
    }
}
