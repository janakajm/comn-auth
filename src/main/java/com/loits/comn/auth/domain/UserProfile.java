package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "user_profile")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfile {

    @Id
    private Long id;

    private String tenantId;

    private String userId;

    private String userName;

    private String userRole;

    private String userType;

    private Long departmentId;

    private String department;

    private Long designationId;

    private String designation;

    private String employeeNumber;

    private String email;

    private String userStatus;

    private String profileStatus;

    private String functionalSupervisor;

    private String administrativeSupervisor;

    private Long version;


    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userProfile")
    private List<UserProfileIdentityServer> userIdentityServerList;

    @JsonIgnore
    @OneToMany(mappedBy ="user", fetch = FetchType.LAZY)
    List<UserRole> userRoles;

    @JsonIgnore
    @OneToMany(mappedBy ="user", fetch = FetchType.LAZY)
    List<RoleGroupUser> userRoleGroups;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
