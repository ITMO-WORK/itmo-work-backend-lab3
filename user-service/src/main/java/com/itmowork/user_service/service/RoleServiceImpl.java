package com.itmowork.user_service.service;

import com.itmowork.user_service.model.Role;
import com.itmowork.user_service.model.RoleName;
import com.itmowork.user_service.repository.RoleRepository;
import com.itmowork.user_service.service.interfaces.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Optional<Role> findRoleByRoleName(RoleName roleName) {
        return roleRepository.findRoleByRoleName(roleName);
    }
}
