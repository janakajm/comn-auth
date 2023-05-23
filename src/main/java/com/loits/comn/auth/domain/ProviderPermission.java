package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "provider_permission")
public class ProviderPermission {

    @EmbeddedId
    private ProviderPermissionIdClass providerPermissionIdClass;
//
//    @ManyToOne
//    @MapsId("permission_id")
//    @JoinColumn(name = "permission_id")
//    Permission permission;

    @Basic
    @Column(name = "provider_permission_id", nullable = false, length = 100)
    private String providerPermissionId;

	public String getProviderPermissionId() {
		return providerPermissionId;
	}

	public void setProviderPermissionId(String providerPermissionId) {
		this.providerPermissionId = providerPermissionId;
	}

}
