package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.RoleGroup;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;
import java.util.List;

@Projection(types = {RoleGroup.class}, name = "roleGroup")
public interface BasicRoleGroupProjection {
    Long getId();
    String getName();
    String getDescription();
    String getCreatedBy();
    Timestamp getCreatedOn();
    Long getVersion();

}