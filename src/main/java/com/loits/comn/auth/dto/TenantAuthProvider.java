package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.sql.Timestamp;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantAuthProvider {

    AppTenant appTenant;
    AuthProvider authProvider;
    private String appId;
    private String clientId;
    private String secret;
    private String audience;
    private Byte status;
    private String createdBy;
    private Timestamp createdOn;
    private boolean isPrimary;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class AuthProvider{
        String name;
        String description;
        Byte status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class AppTenant{
        String tenantId;
        String name;
        String description;
    }
}
