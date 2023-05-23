package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
public class UserGroupProfileId implements Serializable {

    private Long userGroup;
    private Long userProfile;

    public UserGroupProfileId() {
    }

    public UserGroupProfileId(Long userGroup, Long userProfile) {
        this.userGroup = userGroup;
        this.userProfile = userProfile;
    }
}
