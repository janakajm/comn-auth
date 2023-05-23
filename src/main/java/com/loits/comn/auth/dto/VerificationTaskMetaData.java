package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loits.comn.auth.domain.ModuleMeta;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerificationTaskMetaData {
    private Integer remoteObjects;
    private Integer localObjects;
}
