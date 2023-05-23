package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddUserGroups {
    private Long user;
    private String strategy;
    private List<AddGroup> groups;
    private String status;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddGroup {

        public AddGroup() {
        }

        private Long group;
        private Timestamp expires;
        private String assignedBy;
        private Byte delegatable;
        private String meta;
        private String status;
    }
}

