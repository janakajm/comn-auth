package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "provider_user")
public class ProviderUser {

    @EmbeddedId
    private ProviderUserIdClass providerUserIdClass;

    @Basic
    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

}
