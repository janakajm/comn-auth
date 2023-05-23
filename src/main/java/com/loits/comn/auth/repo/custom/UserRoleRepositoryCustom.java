package com.loits.comn.auth.repo.custom;

import com.loits.comn.auth.domain.*;
import com.querydsl.jpa.impl.JPAQuery;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

public interface UserRoleRepositoryCustom {

    //existsByUserAndRoleAndRoleGroupAndUserGroupIdIsNull
    boolean isDuplicate(UserProfile user, Role role, RoleGroup roleGroup);

    //findByUserAndRoleAndRoleGroupAndUserGroupIdIsNull
    JPAQuery<UserRole> getUniqueUserRole(UserProfile user, Role role, RoleGroup roleGroup);

    //existsByUserAndRoleAndRoleGroupAndUserGroupIdIsNullNot
    boolean isDuplicateNot(UserProfile user, Role role, RoleGroup roleGroup);

    boolean checkForDuplicatesByRoleGroups(UserProfile user, RoleGroup roleGroup);

    boolean checkForDuplicateRolesByRole(UserProfile user, Role role);

    JPAQuery<UserRole> getRolesByKey(UserProfile user, String key);

    JPAQuery<UserRole> getDelegatableRoles(UserProfile user);


}
