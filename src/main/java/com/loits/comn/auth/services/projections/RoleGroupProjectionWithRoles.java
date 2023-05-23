package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.RoleGroup;
import com.loits.comn.auth.domain.RoleGroupRole;
import org.springframework.data.rest.core.config.Projection;

import java.sql.Timestamp;
import java.util.List;

@Projection(types = {RoleGroup.class}, name = "roleGroup")
public interface RoleGroupProjectionWithRoles {
    Long getId();
    String getName();
    String getDescription();
    String getCreatedBy();
    Timestamp getCreatedOn();
    Long getVersion();
    List<RoleGroupRole> getRoleGroupRoles();

    interface RoleGroupRole {
        Role getRole();

        interface Role{
            Long getId();
            String getName();
            String getDescription();
            String getCreatedBy();
            Timestamp getCreatedOn();
        }
    }

}