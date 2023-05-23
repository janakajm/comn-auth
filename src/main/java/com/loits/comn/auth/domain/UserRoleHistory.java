package com.loits.comn.auth.domain;

import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "user_role_history")
public class UserRoleHistory extends BaseEntity {

    @Basic
    @Column(name = "user_id", nullable = false, length = 100)
    private Long userId;

    @Basic
    @Column(name = "role_id", nullable = false, length = 100)
    private Long roleId;

    @Basic
    @Column(name = "role_group_id")
    private Long roleGroup;

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
    @Column(name = "record_type", nullable = true, length = 45)
    private String recordType;

    @Basic
    @Column(name = "expires", nullable = true)
    private Timestamp expires;

    @Basic
    @Column(name = "delegatable", nullable = true)
    private Byte delegatable;


    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        UserRoleHistory that = (UserRoleHistory) object;
        return Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(roleId, that.roleId) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(status, that.status) &&
                Objects.equals(recordType, that.recordType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, roleId, createdBy, createdOn, status, recordType);
    }
}
