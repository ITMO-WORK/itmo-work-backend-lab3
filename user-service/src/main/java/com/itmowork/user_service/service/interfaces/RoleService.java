package com.itmowork.user_service.service.interfaces;

import com.itmowork.user_service.model.Role;
import com.itmowork.user_service.model.RoleName;

import java.util.Optional;

public interface RoleService {

    Optional<Role> findRoleByRoleName(RoleName roleName);
}
