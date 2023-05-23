package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleRequest {
    private String name;
    private String description;
    private String applicationType;
    private String applicationId;
    private String[] permissions;
}
