package org.ilestegor.applicationservice.service;

import org.ilestegor.applicationservice.model.ApplicationStatus;
import org.ilestegor.applicationservice.model.ApplicationStatusName;
import org.ilestegor.applicationservice.repository.ApplicationStatusRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class ApplicationStatusServiceUnitTest {
    @Mock
    private ApplicationStatusRepository applicationStatusRepository;

    @InjectMocks
    private ApplicationStatusServiceImpl applicationStatusService;

    @Nested
    class FindByIdTests {

        @Test
        void shouldReturnStatusWhenFoundById() {
            
            Long statusId = 1L;
            ApplicationStatus status = ApplicationStatus.builder()
                    .id(statusId)
                    .applicationStatusName(ApplicationStatusName.NEW)
                    .build();

            when(applicationStatusRepository.findApplicationStatusById(statusId))
                    .thenReturn(Mono.just(status));

            
            Mono<ApplicationStatus> result =
                    applicationStatusService.findApplicationStatusByApplicationStatusId(statusId);

            
            StepVerifier.create(result)
                    .expectNext(status)
                    .verifyComplete();
        }

        @Test
        void shouldReturnEmptyWhenStatusNotFoundById() {
            
            Long statusId = 999L;

            when(applicationStatusRepository.findApplicationStatusById(statusId))
                    .thenReturn(Mono.empty());

            
            Mono<ApplicationStatus> result =
                    applicationStatusService.findApplicationStatusByApplicationStatusId(statusId);

            
            StepVerifier.create(result)
                    .verifyComplete(); 
        }
    }

    @Nested
    class FindByNameTests {

        @Test
        void shouldReturnStatusWhenFoundByName() {
            
            ApplicationStatusName name = ApplicationStatusName.NEW;
            ApplicationStatus status = ApplicationStatus.builder()
                    .id(1L)
                    .applicationStatusName(name)
                    .build();

            when(applicationStatusRepository.findByApplicationStatusName(name))
                    .thenReturn(Mono.just(status));

            
            Mono<ApplicationStatus> result =
                    applicationStatusService.findApplicationStatusByApplicationStatusName(name);

            
            StepVerifier.create(result)
                    .expectNext(status)
                    .verifyComplete();
        }

        @Test
        void shouldReturnEmptyWhenStatusNotFoundByName() {
            
            ApplicationStatusName name = ApplicationStatusName.REJECTED;

            when(applicationStatusRepository.findByApplicationStatusName(name))
                    .thenReturn(Mono.empty());

            
            Mono<ApplicationStatus> result =
                    applicationStatusService.findApplicationStatusByApplicationStatusName(name);

            
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }
}
