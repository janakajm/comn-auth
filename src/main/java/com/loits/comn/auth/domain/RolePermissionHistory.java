package com.loits.comn.auth.domain;

import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.util.Objects;

@Data
@Entity
@Table(name = "role_permission_history")
public class RolePermissionHistory extends BaseEntity {

    @Basic
    @Column(name = "role_id", nullable = false, length = 100)
    private Long roleId;

    @Basic
    @Column(name = "permission_id", nullable = false, length = 100)
    private Long permissionId;

    @Basic
    @Column(name = "status", nullable = true)
    private Byte status;

    @Basic
    @Column(name = "created_by", nullable = true, length = 45)
    private String createdBy;

    @Basic
    @Column(name = "created_on", nullable = true, length = 45)
    private String createdOn;

    @Basic
    @Column(name = "record_type", nullable = true, length = 45)
    private String recordType;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        RolePermissionHistory that = (RolePermissionHistory) object;
        return Objects.equals(id, that.id) &&
                Objects.equals(roleId, that.roleId) &&
                Objects.equals(permissionId, that.permissionId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(recordType, that.recordType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleId, permissionId, status, createdBy, createdOn, recordType);
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

	public Byte getStatus() {
		return status;
	}

	public void setStatus(Byte status) {
		this.status = status;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(String createdOn) {
		this.createdOn = createdOn;
	}

	public String getRecordType() {
		return recordType;
	}

	public void setRecordType(String recordType) {
		this.recordType = recordType;
	}
    
}
