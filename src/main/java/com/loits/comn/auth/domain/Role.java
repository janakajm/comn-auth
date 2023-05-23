package com.loits.comn.auth.domain;

import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "auth_role")
public class Role extends BaseEntity {

    @Basic
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Basic
    @Column(name = "description", nullable = true, length = 140)
    private String description;

    @Basic
    @Column(name = "search", nullable = true, length = 4000)
    private String search;

    @Basic
    @Column(name = "created_by", nullable = false, length = 45)
    private String createdBy;

    @Basic
    @Column(name = "created_on", nullable = false)
    private Timestamp createdOn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    private String tenant;
    
    @Basic
    @Column(name = "auth0", nullable = true)
    private String auth0;
    
    @Basic
    @Column(name = "deleted", nullable = true)
    private String deleted;

    @OneToMany(mappedBy ="role", fetch = FetchType.EAGER)
    List<RolePermission> rolePermissions;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Role role = (Role) object;
        return Objects.equals(id, role.id) &&
                Objects.equals(name, role.name) &&
                Objects.equals(description, role.description) &&
                Objects.equals(createdBy, role.createdBy) &&
                Objects.equals(createdOn, role.createdOn) &&
                Objects.equals(version, role.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, createdBy, createdOn, version);
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<RolePermission> getRolePermissions() {
		return rolePermissions;
	}

	public void setRolePermissions(List<RolePermission> rolePermissions) {
		this.rolePermissions = rolePermissions;
	}

	public String getAuth0() {
		return auth0;
	}

	public void setAuth0(String auth0) {
		this.auth0 = auth0;
	}

	public String getDeleted() {
		return deleted;
	}

	public void setDeleted(String deleted) {
		this.deleted = deleted;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
	
	
}
