package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateRole {

    private String name;

    @NotBlank(message = "description-Empty")
    private String description;

    private String[] permissions;

    @NotNull(message = "version-Null")
    private Long version;
}
