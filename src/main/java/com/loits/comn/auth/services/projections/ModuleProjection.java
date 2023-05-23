package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.Module;
import com.loits.comn.auth.domain.ModuleMeta;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.RolePermission;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;
import java.util.List;

@Projection(types = {Module.class}, name = "module")
public interface ModuleProjection {
    String getCode();
    String getLabel();
    String getCreatedBy();
    Timestamp getCreatedOn();
    List<ModuleMeta> getModuleMetaList();

}