package com.loits.comn.auth.domain;

import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Date;

@Data
@Entity
@Table(name = "scheduled_jobs")
public class ScheduledJobs extends BaseEntity {

    public enum JobType {
        SYNC_BRANCHES,
        SYNC_USER_PROFILES,
        SYNC_USER_GROUPS,
        SYNC_USER_GROUP_USERS,
        VERIFY_PERMISSION,
        VERIFY_ROLE_PERMISSIONS,
        VERIFY_GROUP_ROLES,
        VERIFY_USER_ROLES
    }

    public enum StatusType {
        SUCCESS,
        FAILED,
        PENDING
    }

    @Basic
    @Column(name = "description", nullable = true, length = 5000)
    public String description;

    @Basic
    @Column(name = "job", nullable = false, length = 45)
    public JobType job;

    @Basic
    @Column(name = "status", length = 15)
    public StatusType status;

    @Basic
    @Column(name = "started_on", nullable = true)
    public Timestamp startedTime;

    @Basic
    @Column(name = "created_by", nullable = true, length = 45)
    private String createdBy;

    @Basic
    @Column(name = "ended_on", nullable = true)
    public Timestamp endedTime;

}