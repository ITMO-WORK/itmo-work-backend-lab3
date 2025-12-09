package org.ilestegor.applicationservice.repository;


import org.ilestegor.applicationservice.model.ApplicationStatus;
import org.ilestegor.applicationservice.model.ApplicationStatusName;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ApplicationStatusRepository extends ReactiveCrudRepository<ApplicationStatus, Long> {
    Mono<ApplicationStatus> findApplicationStatusById(Long id);

    Mono<ApplicationStatus> findByApplicationStatusName(ApplicationStatusName applicationStatusName);
}
