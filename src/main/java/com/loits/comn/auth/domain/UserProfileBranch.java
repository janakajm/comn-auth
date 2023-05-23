package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loits.comn.auth.core.BaseEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Table(name = "user_profile_branch")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileBranch extends BaseEntity {

//    @JsonIgnore
//    @EmbeddedId
//    private UserProfileBranchId userProfileBranchId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
//    @MapsId("user_profile")
    @JoinColumn(name = "user_profile")
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.EAGER)
//    @MapsId("branch")
    @JoinColumn(name = "branch")
    private Branch branch;

    private Timestamp fromDate;

    private Timestamp toDate;

    private Byte status;

    private String createdBy;

    private Timestamp createdOn;

    private String modifiedBy;

    private Timestamp modifiedOn;


}
