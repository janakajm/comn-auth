package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "provider_group")
public class ProviderGroup {

    @EmbeddedId
    private ProviderGroupIdClass providerGroupIdClass;

//    @ManyToOne
//    @MapsId("role_id")
//    @JoinColumn(name = "role_id")
//    Role role;

    @Basic
    @Column(name = "provider_group_id", nullable = false, length = 100)
    private String providerGroupId;

	public String getProviderGroupId() {
		return providerGroupId;
	}

	public void setProviderGroupId(String providerGroupId) {
		this.providerGroupId = providerGroupId;
	}

	public ProviderGroupIdClass getProviderGroupIdClass() {
		return providerGroupIdClass;
	}

	public void setProviderGroupIdClass(ProviderGroupIdClass providerGroupIdClass) {
		this.providerGroupIdClass = providerGroupIdClass;
	}
    

}
