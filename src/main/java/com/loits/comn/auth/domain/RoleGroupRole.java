package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "role_group_role")
public class RoleGroupRole {

    @EmbeddedId
    private RoleGroupRoleId roleGroupRoleId;

    @ManyToOne
    @MapsId("role_id")
    @JoinColumn(name = "role_id")
    Role role;

    @ManyToOne
    @MapsId("role_group_id")
    @JoinColumn(name = "role_group_id")
    RoleGroup roleGroup;

    @Basic
    @Column(name = "created_by", nullable = true, length = 45)
    private String createdBy;

    @Basic
    @Column(name = "created_on", nullable = true)
    private Timestamp createdOn;

    @Basic
    @Column(name = "status", nullable = true)
    private Byte status;

    @Transient
    private String tenant;
    
    @Column(name = "auth0", nullable = true)
    private String auth0;

	public String getAuth0() {
		return auth0;
	}

	public void setAuth0(String auth0) {
		this.auth0 = auth0;
	}

	public RoleGroup getRoleGroup() {
		return roleGroup;
	}

	public void setRoleGroup(RoleGroup roleGroup) {
		this.roleGroup = roleGroup;
	}

}
