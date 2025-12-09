package com.itmowork.company_service.service;

import com.itmowork.company_service.model.UserCompany;
import com.itmowork.company_service.repository.UserCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserCompanyServiceImplTest {

    @Mock
    private UserCompanyRepository userCompanyRepository;

    @InjectMocks
    private UserCompanyServiceImpl userCompanyService;



    private UUID userId;
    private UUID companyId;
    private UserCompany userCompany;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        userCompany = new UserCompany();
        userCompany.setId(1L);
        userCompany.setUserId(userId);
        userCompany.setCompanyId(companyId);
    }

    @Test
    void saveUserCompanySuccess() {
        UserCompany savedUserCompany = new UserCompany();
        savedUserCompany.setId(1L);
        savedUserCompany.setUserId(userId);
        savedUserCompany.setCompanyId(companyId);

        when(userCompanyRepository.save(any(UserCompany.class)))
                .thenReturn(Mono.just(savedUserCompany));

        StepVerifier.create(userCompanyService.saveUserCompany(userCompany))
                .expectNextMatches(result ->
                        result.getId() == 1L &&
                                result.getUserId().equals(userId) &&
                                result.getCompanyId().equals(companyId)
                )
                .verifyComplete();
    }

    @Test
    void validateCompanyOwnershipUserIsOwner() {
        when(userCompanyRepository.existsByCompanyIdAndUserId(companyId, userId))
                .thenReturn(Mono.just(true));

        StepVerifier.create(userCompanyService.validateCompanyOwnership(companyId, userId))
                .expectNext(true)
                .verifyComplete();
    }
}
