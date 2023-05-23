package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.RolePermission;
import com.loits.comn.auth.domain.RolePermissionId;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;
import java.util.List;

@Projection(types = {RolePermission.class}, name = "rolePermission")
public interface RolePermissionProjection {
    Role getRole();
    Permission getPermission();

    interface Role{
        Long getId();
        String getName();
        String getDescription();
        String getCreatedBy();
        Timestamp getCreatedOn();
    }

    interface Permission{
        Long getId();
        String getName();
        String getDescription();
        String getCreatedBy();
        Timestamp getCreatedOn();
    }
}