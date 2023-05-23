package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.RolePermission;
import com.loits.comn.auth.domain.RolePermissionId;
import com.loits.comn.auth.dto.PermissionResponse;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Projection(types = {Role.class}, name = "role")
public interface RoleProjectionWithPermissions {
    Long getId();
    String getName();
    String getDescription();
    String getCreatedBy();
    Timestamp getCreatedOn();
    Long getVersion();
    List<RolePermission> getRolePermissions();

    interface RolePermission{
        Permission getPermission();

        interface Permission{
            Long getId();
            String getName();
            String getDescription();
            String getCreatedBy();
            Timestamp getCreatedOn();
        }
    }
}