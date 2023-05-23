package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Embeddable
public class RoleGroupRoleId implements Serializable {

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_group_id")
    private Long roleGroupId;

}
