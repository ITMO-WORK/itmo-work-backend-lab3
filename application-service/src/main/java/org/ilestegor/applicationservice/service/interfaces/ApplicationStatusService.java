package org.ilestegor.applicationservice.service.interfaces;

import org.ilestegor.applicationservice.model.ApplicationStatus;
import org.ilestegor.applicationservice.model.ApplicationStatusName;
import reactor.core.publisher.Mono;

public interface ApplicationStatusService {
    Mono<ApplicationStatus> findApplicationStatusByApplicationStatusId(Long applicationStatusId);

    Mono<ApplicationStatus> findApplicationStatusByApplicationStatusName(ApplicationStatusName name);


}
