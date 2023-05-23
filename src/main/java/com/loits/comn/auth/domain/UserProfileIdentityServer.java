package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "user_profile_identity_server")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileIdentityServer {

    @Id
    private Long id;
    private String identityServer;
    private String userIdentityServersId;
    private Long version;
    private String nickName;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_profile", referencedColumnName = "id")
    private UserProfile userProfile;

}
