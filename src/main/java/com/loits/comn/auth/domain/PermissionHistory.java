package com.loits.comn.auth.domain;

import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "permission_history")
public class PermissionHistory extends BaseEntity {

    @Basic
    @Column(name = "permission_id", nullable = false, length = 100)
    private Long permissionId;

    @Basic
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Basic
    @Column(name = "description", nullable = true, length = 140)
    private String description;

    @Basic
    @Column(name = "created_by", nullable = false, length = 45)
    private String createdBy;

    @Basic
    @Column(name = "created_on", nullable = false)
    private Timestamp createdOn;

    @Basic
    @Column(name = "version", nullable = false)
    private Long version;

    @Basic
    @Column(name = "record_type", nullable = true, length = 45)
    private String recordType;
    

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PermissionHistory that = (PermissionHistory) object;
        return Objects.equals(id, that.id) &&
                Objects.equals(permissionId, that.permissionId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(version, that.version) &&
                Objects.equals(recordType, that.recordType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, permissionId, name, description, createdBy, createdOn, version, recordType);
    }

 
}
