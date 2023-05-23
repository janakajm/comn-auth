package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "provider_role")
public class ProviderRole {

    @EmbeddedId
    private ProviderRoleIdClass providerRoleIdClass;

    @Column(name = "role_id",insertable = false,updatable = false)
    private Long roleId;

    @Column(name = "provider",insertable = false,updatable = false)
    private String provider;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("role_id")
    @JoinColumn(name = "role_id")
    Role role;

    @Basic
    @Column(name = "provider_role_id", nullable = false, length = 100)
    private String providerRoleId;

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	public String getProviderRoleId() {
		return providerRoleId;
	}

	public void setProviderRoleId(String providerRoleId) {
		this.providerRoleId = providerRoleId;
	}
    

}
