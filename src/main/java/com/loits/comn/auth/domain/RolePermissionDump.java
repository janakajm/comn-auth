package com.loits.comn.auth.domain;

import java.sql.Timestamp;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;

@Data
@Entity
@Table(name = "role_permission_dump")
public class RolePermissionDump {
	
	@EmbeddedId
    private PermissionRoleId permissionRoleId;
	
	@Basic
	@Column(name = "role_id")
    private Long roleId;

	@Basic
    @Column(name = "permission_id")
    private Long permissionId;
	
    @Basic
    @Column(name = "created_by", nullable = true, length = 100)
    private String createdBy;

    @Basic
    @Column(name = "created_on", nullable = true, length = 45)
    private Timestamp createdOn;

    @Transient
    private String tenant;

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

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public PermissionRoleId getPermissionRoleId() {
		return permissionRoleId;
	}

	public void setPermissionRoleId(PermissionRoleId permissionRoleId) {
		this.permissionRoleId = permissionRoleId;
	}

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	public Long getPermissionId() {
		return permissionId;
	}

	public void setPermissionId(Long permissionId) {
		this.permissionId = permissionId;
	}
 
}
