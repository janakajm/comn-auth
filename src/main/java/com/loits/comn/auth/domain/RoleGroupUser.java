package com.loits.comn.auth.domain;

import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "role_group_user")
public class RoleGroupUser extends BaseEntity {

    private RoleGroupUserId roleGroupUserId;

    @ManyToOne
    @MapsId("user_id")
    @JoinColumn(name = "user_id")
    UserProfile user;

    @ManyToOne
    @MapsId("role_group_id")
    @JoinColumn(name = "role_group_id")
    RoleGroup roleGroup;

    @Basic
    @Column(name = "created_by", nullable = true, length = 45)
    private String createdBy;

    @Basic
    @Column(name = "user_group_id", nullable = true, length = 45)
    private Long userGroupId;

    @Basic
    @Column(name = "created_on", nullable = true)
    private Timestamp createdOn;

    @Basic
    @Column(name = "status", nullable = true)
    private Byte status;

    @Transient
    private String tenant;

    @Basic
    @Column(name = "expires", nullable = true)
    private Timestamp expires;

    @Basic
    @Column(name = "delegatable", nullable = true)
    private Byte delegatable;

	public RoleGroupUserId getRoleGroupUserId() {
		return roleGroupUserId;
	}

	public void setRoleGroupUserId(RoleGroupUserId roleGroupUserId) {
		this.roleGroupUserId = roleGroupUserId;
	}

	public UserProfile getUser() {
		return user;
	}

	public void setUser(UserProfile user) {
		this.user = user;
	}

	public RoleGroup getRoleGroup() {
		return roleGroup;
	}

	public void setRoleGroup(RoleGroup roleGroup) {
		this.roleGroup = roleGroup;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Long getUserGroupId() {
		return userGroupId;
	}

	public void setUserGroupId(Long userGroupId) {
		this.userGroupId = userGroupId;
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

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public Timestamp getExpires() {
		return expires;
	}

	public void setExpires(Timestamp expires) {
		this.expires = expires;
	}

	public Byte getDelegatable() {
		return delegatable;
	}

	public void setDelegatable(Byte delegatable) {
		this.delegatable = delegatable;
	}
    

}
