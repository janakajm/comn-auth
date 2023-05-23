package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.Role;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;
import java.util.List;

@Projection(types = {Role.class}, name = "role")
public interface BasicRoleProjection {
    Long getId();
    String getName();
    String getDescription();
    String getCreatedBy();
    Timestamp getCreatedOn();
    Long getVersion();
}