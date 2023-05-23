package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "role_permission")
public class RolePermission {

    @EmbeddedId
    private RolePermissionId rolePermissionId;

    @ManyToOne
    @MapsId("role_id")
    @JoinColumn(name = "role_id")
    Role role;

    @ManyToOne
    @MapsId("permission_id")
    @JoinColumn(name = "permission_id")
    Permission permission;

    @Basic
    @Column(name = "created_by", nullable = true, length = 100)
    private String createdBy;

    @Basic
    @Column(name = "created_on", nullable = true, length = 45)
    private Timestamp createdOn;

    @Basic
    @Column(name = "status", nullable = true)
    private Byte status;
    
    @Column(name = "auth0", nullable = true)
    private String auth0;

    @Transient
    private String tenant;

    @Override
    public String toString() {
        return "RolePermission{" +
                "rolePermissionId=" + rolePermissionId +
                ", permission=" + permission +
                ", createdBy='" + createdBy + '\'' +
                ", createdOn=" + createdOn +
                ", status=" + status +
                ", tenant='" + tenant + '\'' +
                '}';
    }

	public Permission getPermission() {
		return permission;
	}

	public void setPermission(Permission permission) {
		this.permission = permission;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getAuth0() {
		return auth0;
	}

	public void setAuth0(String auth0) {
		this.auth0 = auth0;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	
}
