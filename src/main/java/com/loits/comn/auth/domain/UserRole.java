package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;
import oracle.security.crypto.core.math.BigInt;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "user_role")
public class UserRole extends BaseEntity {
    //made for backward compatibility dont use in the future
    private UserRoleId userRoleId;

    @Basic
    @JoinColumn(name = "user_group_id" )
    private Long userGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("user_id")
    @JoinColumn(name = "user_id")
    UserProfile user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("role_id")
    @JoinColumn(name = "role_id")
    Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("role_group_id")
    @JoinColumn(name = "role_group_id")
    private RoleGroup roleGroup;

    @Basic
    @Column(name = "created_by", nullable = true, length = 45)
    private String createdBy;

    @Basic
    @Column(name = "created_on", nullable = true)
    private Timestamp createdOn;

    @Basic
    @Column(name = "status", nullable = true)
    private Byte status;

    @Basic
    @Column(name = "expires", nullable = true)
    private Timestamp expires;

    @Basic
    @Column(name = "delegatable", nullable = true)
    private Byte delegatable;



}
