package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@Embeddable
public class ProviderPermissionIdClass implements Serializable {

    @Column(name = "permission_id")
    private Long permission;


    @Column(name = "provider")
    private String provider;

    public ProviderPermissionIdClass() {
    }

    public ProviderPermissionIdClass(Long permission, String provider) {
        this.permission = permission;
        this.provider = provider;
    }

	public Long getPermission() {
		return permission;
	}

	public void setPermission(Long permission) {
		this.permission = permission;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}
    
}
