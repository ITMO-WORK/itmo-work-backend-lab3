package org.ilestegor.applicationservice.repository;

import org.ilestegor.applicationservice.model.Application;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ApplicationRepository extends ReactiveCrudRepository<Application, Long> {
    Mono<Boolean> existsByUserIdAndVacancyId(UUID userId, UUID vacancyId);

    Mono<Application> findApplicationsByUserIdAndVacancyId(UUID userId, UUID vacancyId);

    Mono<Application> findById(UUID applicationId);

    Flux<Application> findAllByVacancyId(UUID vacancyId, Pageable pageable);

    Mono<Long> countApplicationByVacancyId(UUID vacancyId);

    @Query("select vacancy_id from applications where id =:applicationId ")
    Mono<UUID> findVacancyIdById(UUID applicationId);

    Mono<Boolean> existsById(UUID id);

}
