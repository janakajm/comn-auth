package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Table(name = "user_group_profile")
@JsonIgnoreProperties(ignoreUnknown = true)
@IdClass(UserGroupProfileId.class)
public class UserGroupProfile {
    @Id
    @ManyToOne
    @JoinColumn(name = "user_group")
    private UserGroup userGroup;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_profile")
    private UserProfile userProfile;


}
