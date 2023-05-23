package com.loits.comn.auth.domain;

import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "role_group_role_history")
public class RoleGroupRoleHistory extends BaseEntity {

    @Basic
    @Column(name = "role_group_id", nullable = false, length = 100)
    private Long roleTemplateId;

    @Basic
    @Column(name = "role_id", nullable = false, length = 100)
    private Long roleId;

    @Basic
    @Column(name = "status", nullable = true)
    private Byte status;

    @Basic
    @Column(name = "created_by", nullable = true, length = 45)
    private String createdBy;


    @Basic
    @Column(name = "created_on", nullable = true)
    private Timestamp createdOn;


    @Basic
    @Column(name = "record_type", nullable = true, length = 45)
    private String recordType;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        RoleGroupRoleHistory that = (RoleGroupRoleHistory) object;
        return Objects.equals(id, that.id) &&
                Objects.equals(roleTemplateId, that.roleTemplateId) &&
                Objects.equals(roleId, that.roleId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(recordType, that.recordType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleTemplateId, roleId, status, createdBy, createdOn, recordType);
    }
}
