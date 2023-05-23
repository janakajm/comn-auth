package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.ModuleMeta;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.RolePermission;
import com.loits.comn.auth.dto.PermissionResponse;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Projection(types = {Permission.class}, name = "permission")
public interface PermissionProjection {
    Long getId();
    String getName();
    String getDescription();
    String getCreatedBy();
    Timestamp getCreatedOn();
    Long getVersion();
//    List<RolePermission> getRolePermissions();

}