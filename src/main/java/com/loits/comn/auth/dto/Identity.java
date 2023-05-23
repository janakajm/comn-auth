package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Identity {
    private String user_id;
    private String provider;
    private String connection;
    private Boolean isSocial;
}
