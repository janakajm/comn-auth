package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@Embeddable
public class ProviderGroupIdClass implements Serializable {

    @Column(name = "role_group_id")
    private Long group;


    @Column(name = "provider")
    private String provider;

    public ProviderGroupIdClass() {
    }

    public ProviderGroupIdClass(Long group, String provider) {
        this.group = group;
        this.provider = provider;
    }

	public Long getGroup() {
		return group;
	}

	public void setGroup(Long group) {
		this.group = group;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}
    
    
}
