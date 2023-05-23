package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Table(name = "user_group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserGroup {

    @Id
    private Long id;

    private String tenantId;

    private String groupCode;

    private String groupName;

    private Long groupOwnerId;

    private String groupOwnerName;

    private Long groupTypeId;

    private String groupType;

    private String status;

    private Long version;

    private Timestamp createdDate;

    private String createdBy;

    private Timestamp modifiedDate;

    private String modifiedUser;

//    @JsonIgnore
//    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    @JoinTable(name = "user_group_profile",joinColumns = {
//            @JoinColumn(name = "user_profile", nullable = false, updatable = true) },
//            inverseJoinColumns = { @JoinColumn(name = "user_group",
//                    nullable = false, updatable = true) })
//    private List<UserProfile> userProfileList;

//    @JsonIgnore
//    @OneToMany(mappedBy ="userGroupId", fetch = FetchType.LAZY)
//    List<UserRole> userRoles;

    @Transient
    private List<UserProfile> users;

}
