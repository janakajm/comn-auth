package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.AsyncTask;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.dto.AddUserGroups;
import com.loits.comn.auth.dto.BranchAssignment;
import com.loits.comn.auth.dto.RemoveUser;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AuthService {
//
//    @Async
//    CompletableFuture<?> updateAuth0(List<Permission> permissionList, HttpMethod httpMethod, String subpath, String tenant, AsyncTask at);

    @Async
    CompletableFuture<?> updateAuth0BulkPermissions(List<Permission> permissionList, HttpMethod httpMethod, String subpath, String tenant);

    @Async
    CompletableFuture<?> updateAuth0BulkRoles(List<Role> roleList, HttpMethod httpMethod, String subpath, String tenant);

    void updateAuth0BultRolesAndPermissions(Role roleList, Permission permissionList, HttpMethod httpMethod, String subpath, String tenant);
}
