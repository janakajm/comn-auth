package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncObject {
    private String type;
    private String name;

    public SyncObject(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
