package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "permission")
public class Permission extends BaseEntity {

    @Basic
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Basic
    @Column(name = "description", nullable = true, length = 140)
    private String description;

    @Basic
    @Column(name = "search", nullable = true)
    private String search;
    

    @Basic
    @Column(name = "meta_1")
    private String meta1;

    @Basic
    @Column(name = "created_by", nullable = false, length = 45)
    private String createdBy;

    @Basic
    @Column(name = "created_on", nullable = false)
    private Timestamp createdOn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    @Basic
    @Column(name = "auth0", nullable = true)
    private String auth0;
    
    @Basic
    @Column(name = "deleted", nullable = true)
    private String deleted;

    @Transient
    private String tenant;

    @JsonIgnore
    @OneToMany(mappedBy ="permission", fetch = FetchType.LAZY)
    List<RolePermission> rolePermissions;

    @JsonIgnore
    @OneToMany(mappedBy ="permission", fetch = FetchType.LAZY)
    List<PermissionGroups> permissionGroups;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Permission that = (Permission) object;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, createdBy, createdOn,version);
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

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public String getAuth0() {
		return auth0;
	}

	public void setAuth0(String auth0) {
		this.auth0 = auth0;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getDeleted() {
		return deleted;
	}

	public void setDeleted(String deleted) {
		this.deleted = deleted;
	}
    
}
