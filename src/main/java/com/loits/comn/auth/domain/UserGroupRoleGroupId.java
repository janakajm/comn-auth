package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserGroupRoleGroupId implements Serializable {

    @Column(name = "user_group")
    private Long userGroupId;

    @Column(name = "role_group")
    private Long roleGroupId;




}
