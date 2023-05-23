package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.drools.core.factmodel.traits.Alias;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRoleResponse {
    private String _id;
    private String name;
    private String description;
    private String applicationId;
}
