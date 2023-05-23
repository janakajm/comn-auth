package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "role_group")
public class RoleGroup extends BaseEntity {

    @Basic
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Basic
    @Column(name = "description", nullable = true, length = 140)
    private String description;

    @Basic
    @Column(name = "search", nullable = true, length = 255)
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
    
    @Column(name = "deleted", nullable = true)
    private String deleted;

    @Transient
    private String tenant;

    @OneToMany(mappedBy ="roleGroup", fetch = FetchType.LAZY)
    List<RoleGroupRole> roleGroupRoles;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        RoleGroup that = (RoleGroup) object;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(version, that.version);
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

	public String getDeleted() {
		return deleted;
	}

	public void setDeleted(String deleted) {
		this.deleted = deleted;
	}
	
    
}
