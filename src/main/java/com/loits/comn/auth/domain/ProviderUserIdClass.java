package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@Embeddable
public class ProviderUserIdClass implements Serializable {

    @Column(name = "user_id")
    private Long user;

    @Column(name = "provider")
    private String provider;

    public ProviderUserIdClass() {
    }

    public ProviderUserIdClass(Long user, String provider) {
        this.user = user;
        this.provider = provider;
    }
}
