package com.itmowork.user_service.repository;

import com.itmowork.user_service.model.Role;
import com.itmowork.user_service.model.RoleName;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RoleRepository extends CrudRepository<Role, Long> {
    Optional<Role> findRoleByRoleName(RoleName roleName);
}
