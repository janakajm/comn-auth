package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "permission_group")
public class PermissionGroups {

    @EmbeddedId
    private PermissionGroupId permissionGroupId;

    @ManyToOne
    @MapsId("group_id")
    @JoinColumn(name = "group_id")
    RoleGroup roleGroup;

    @ManyToOne
    @MapsId("permission_id")
    @JoinColumn(name = "permission_id")
    Permission permission;

    @Column(name = "created_by", nullable = true, length = 100)
    private String createdBy;

    @Column(name = "created_on", nullable = true, length = 45)
    private Timestamp createdOn;

    @Column(name = "status", nullable = true)
    private Byte status;

	public PermissionGroupId getPermissionGroupId() {
		return permissionGroupId;
	}

	public void setPermissionGroupId(PermissionGroupId permissionGroupId) {
		this.permissionGroupId = permissionGroupId;
	}

	public RoleGroup getRoleGroup() {
		return roleGroup;
	}

	public void setRoleGroup(RoleGroup roleGroup) {
		this.roleGroup = roleGroup;
	}

	public Permission getPermission() {
		return permission;
	}

	public void setPermission(Permission permission) {
		this.permission = permission;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public Byte getStatus() {
		return status;
	}

	public void setStatus(Byte status) {
		this.status = status;
	}


}
