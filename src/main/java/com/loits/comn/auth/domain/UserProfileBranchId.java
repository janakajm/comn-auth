package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileBranchId implements Serializable {

//    @Column(name = "branch")
//    private Long branchId;
//
//    @Column(name = "user_profile")
//    private Long userProfileId;
//
//


}
