package com.loits.comn.auth.dto;

import java.util.List;

public class PermissionRoleRequest {

    List<AddPermissionRequest> permissions;

    public List<AddPermissionRequest> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<AddPermissionRequest> permissions) {
        this.permissions = permissions;
    }
}
