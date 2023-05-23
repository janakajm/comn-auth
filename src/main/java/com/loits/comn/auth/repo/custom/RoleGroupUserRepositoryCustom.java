package com.loits.comn.auth.repo.custom;

import com.loits.comn.auth.domain.*;
import com.querydsl.jpa.impl.JPAQuery;

public interface RoleGroupUserRepositoryCustom {

    boolean isDuplicatebyUserGroupId(UserProfile user, RoleGroup roleGroup, Long userGroupId);

    boolean isDuplicate(UserProfile user, RoleGroup roleGroup);

    RoleGroupUser getByRoleGroupIdAndUserId(Long roleGroupId, Long userId);

    JPAQuery<RoleGroupUser> getDelegatableRoles(UserProfile user);


}
