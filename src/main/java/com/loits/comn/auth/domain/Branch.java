package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "branch")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Branch {

    @Id
    @Column(name = "id")
    @JsonAlias("id")
    private Long branchId;

    @JsonAlias("brhTenantId")
    private String tenantId;

    @JsonAlias("brhCode")
    private String branchCode;

    @JsonAlias("name")
    private String branchName;

    private Long organizationLevelId;

    @JsonAlias("organizationLevelName")
    private String organizationLevel;

    @JsonAlias("brhStatus")
    private String status;

    @JsonAlias("brhCreatedDate")
    private Timestamp createdDate;

    @JsonAlias("brhCreatedUser")
    private String createdBy;

    @JsonAlias("brhModifiedDate")
    private Timestamp modifiedDate;

    @JsonAlias("brhModifiedUser")
    private String modifiedUser;

    private Long version;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "role")
    private Role role;

}
