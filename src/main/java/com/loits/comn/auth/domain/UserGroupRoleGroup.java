package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "user_group_role_group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserGroupRoleGroup {

    @JsonIgnore
    @EmbeddedId
    private UserGroupRoleGroupId userGroupRoleGroupId;

    @JsonIgnore
    @ManyToOne
    @MapsId("user_group")
    @JoinColumn(name = "user_group")
    private UserGroup userGroup;

    @ManyToOne
    @MapsId("role_group")
    @JoinColumn(name = "role_group")
    private RoleGroup roleGroup;

    private Timestamp expires;

    private Byte delegatable;

}
