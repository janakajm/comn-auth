package com.loits.comn.auth.dto;

public class AddPermissionRequest {

    private String resource_server_identifier;

    private String permission_name;

    public void setResource_server_identifier(String resource_server_identifier) {
        this.resource_server_identifier = resource_server_identifier;
    }

    public void setPermission_name(String permission_name) {
        this.permission_name = permission_name;
    }
}
