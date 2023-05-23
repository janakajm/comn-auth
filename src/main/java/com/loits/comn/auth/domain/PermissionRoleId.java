package com.loits.comn.auth.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.Data;

@Data
@Embeddable
public class PermissionRoleId implements Serializable{
	
	@Column(name = "provider_role_id")
    private String providerRoleId;

    @Column(name = "provider_permission_id")
    private String providerPermissionId;

	public String getProviderRoleId() {
		return providerRoleId;
	}

	public void setProviderRoleId(String providerRoleId) {
		this.providerRoleId = providerRoleId;
	}

	public String getProviderPermissionId() {
		return providerPermissionId;
	}

	public void setProviderPermissionId(String providerPermissionId) {
		this.providerPermissionId = providerPermissionId;
	}
	
    
}
