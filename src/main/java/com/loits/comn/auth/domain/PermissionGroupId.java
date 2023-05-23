package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Embeddable
public class PermissionGroupId implements Serializable {

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "permission_id")
    private Long permissionId;

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public Long getPermissionId() {
		return permissionId;
	}

	public void setPermissionId(Long permissionId) {
		this.permissionId = permissionId;
	}

}
