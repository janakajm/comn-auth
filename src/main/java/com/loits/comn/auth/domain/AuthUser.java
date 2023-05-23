package com.loits.comn.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loits.comn.auth.dto.AppMetaData;
import com.loits.comn.auth.dto.Identity;
import lombok.Data;

import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthUser {
    private String user_id;
    private String name;
    private String email;
    private String last_login;
    private Long logins_count;
    private String picture;
    private Identity[] identities;
    private Date created_at;
    private Boolean email_verified;
    private String nickname;
    private Date updated_at;
    private AppMetaData app_metadata;
    private String last_ip;

}
