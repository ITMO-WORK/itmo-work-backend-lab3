package com.itmowork.company_service.service;

import com.itmowork.company_service.model.UserCompany;
import com.itmowork.company_service.repository.UserCompanyRepository;
import com.itmowork.company_service.service.interfaces.UserCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCompanyServiceImpl implements UserCompanyService {

    private final UserCompanyRepository userCompanyRepository;

    @Override
    public Mono<UserCompany> saveUserCompany(UserCompany userCompany) {
        return userCompanyRepository.save(userCompany);
    }

    @Override
    public Mono<Boolean> validateCompanyOwnership(UUID companyId, UUID userId) {
        return userCompanyRepository.existsByCompanyIdAndUserId(companyId, userId);
    }
}
