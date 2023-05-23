package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@Embeddable
public class ProviderRoleIdClass implements Serializable {

    @Column(name = "role_id")
    private Long role;

    @Column(name = "provider")
    private String provider;

    public ProviderRoleIdClass() {
    }

    public ProviderRoleIdClass(Long role, String provider) {
        this.role = role;
        this.provider = provider;
    }

	public Long getRole() {
		return role;
	}

	public void setRole(Long role) {
		this.role = role;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}
    
}
