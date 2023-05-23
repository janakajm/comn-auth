package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loits.comn.auth.domain.ModuleMeta;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddModule {
    private String code;
    private String label;
    private List<ModuleMeta> moduleMetaList;
}
