package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@Embeddable
public class RoleGroupUserId implements Serializable {

    @Column(name = "role_group_id")
    private Long roleGroupId;

    @Column(name = "user_id")
    private Long userId;



}
