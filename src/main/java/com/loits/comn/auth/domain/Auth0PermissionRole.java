package com.loits.comn.auth.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "auth0_permission_role")
public class Auth0PermissionRole {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auth_generator")
	@SequenceGenerator(name = "auth_generator", sequenceName = "auth_seq", allocationSize = 1)
	@Column(name = "id", nullable = false)
	private Long id;

    @Column(name = "role_name")
    private String roleName;
    
    @Column(name = "role_provider")
    private String roleProvider;
    
    @Column(name = "permission_name")
    private String permissionName;

    @Column(name = "permission_provider")
    private String permissionProvider;

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getRoleProvider() {
		return roleProvider;
	}

	public void setRoleProvider(String roleProvider) {
		this.roleProvider = roleProvider;
	}

	public String getPermissionName() {
		return permissionName;
	}

	public void setPermissionName(String permissionName) {
		this.permissionName = permissionName;
	}

	public String getPermissionProvider() {
		return permissionProvider;
	}

	public void setPermissionProvider(String permissionProvider) {
		this.permissionProvider = permissionProvider;
	}
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
