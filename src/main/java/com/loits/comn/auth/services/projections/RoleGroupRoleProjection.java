package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.RoleGroup;
import com.loits.comn.auth.domain.RoleGroupRole;
import com.loits.comn.auth.domain.RolePermission;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;

@Projection(types = {RoleGroupRole.class}, name = "roleGroupRole")
public interface RoleGroupRoleProjection {

    RoleGroup getRoleGroup();
    Role getRole();

    interface RoleGroup{
        Long getId();
        String getName();
        String getDescription();
        String getCreatedBy();
        Timestamp getCreatedOn();
    }

    interface Role{
        Long getId();
        String getName();
        String getDescription();
        String getCreatedBy();
        Timestamp getCreatedOn();
    }
}