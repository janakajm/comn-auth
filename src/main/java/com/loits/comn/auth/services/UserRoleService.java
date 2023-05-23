package com.loits.comn.auth.services;

public interface UserRoleService {

    Object delete(String projection, String userId, String roleId, String user, String tenent);

    Object update(String projection, String userId, String roleId, String user, String tenent);
}
