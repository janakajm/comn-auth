package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.dto.PermissionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;
import java.util.List;

@Projection(types = {UserProfile.class}, name = "userProjection")
public interface UserProjection {
    Long getId();

    @Value("#{target.getUserName()}")
    String getName();
    String getEmail();
    String getUserStatus();
    String getProfileStatus();

    @Value("#{target.getUserRoleGroups()}")
    List<RoleGroupUser> getGroups();

    interface RoleGroupUser{

        @Value("#{target.getRoleGroup().getId()}")
        Long getId();
        @Value("#{target.getRoleGroup().getName()}")
        String getName();
        @Value("#{target.getRoleGroup().getDescription()}")
        String getDescription();
        Timestamp getExpires();
        Byte getDelegatable();
    }
}