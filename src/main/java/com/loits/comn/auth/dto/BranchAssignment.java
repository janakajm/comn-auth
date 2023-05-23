package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NonNull;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchAssignment {
    @NotNull
    private long userBranchId;
    private Timestamp fromDate;
    private Timestamp toDate;
    private Byte status;
}

