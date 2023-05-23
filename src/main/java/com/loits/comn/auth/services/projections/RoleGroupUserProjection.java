package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.RoleGroupUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;

@Projection(types = {RoleGroupUser.class}, name = "roleGroupUser")
public interface RoleGroupUserProjection {

    @Value("#{target.getRoleGroup().getId()}")
    Long getId();
    @Value("#{target.getRoleGroup().getName()}")
    String getName();
    @Value("#{target.getRoleGroup().getDescription()}")
    String getDescription();

    Timestamp getExpires();
    Byte getDelegatable();
}