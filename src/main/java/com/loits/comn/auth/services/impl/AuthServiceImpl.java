package com.loits.comn.auth.services.impl;


import com.loits.comn.auth.commons.NullAwareBeanUtilsBean;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.dto.*;
import com.loits.comn.auth.mt.TenantHolder;
import com.loits.comn.auth.repo.*;
import com.loits.comn.auth.services.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class AuthServiceImpl implements AuthService {

    Logger logger = LogManager.getLogger(AuthServiceImpl.class);

    @Autowired
    HttpService httpService;

    @Autowired
    Executor executor;

    @Value("${auth.provider}")
    private String PROVIDER;

    @Value("${auth0.authorization.extension.api.identifier}")
    private String EXTENSION_API_AUDIENCE;

    @Autowired
    ProviderPermissionRepository providerPermissionRepository;

    @Autowired
    ProviderRoleRepository providerRoleRepository;

    @Override
    @Async
    public CompletableFuture<?> updateAuth0BulkPermissions(List<Permission> permissionList, HttpMethod httpMethod, String subpath, String tenant) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenant);
            permissionList.forEach(permission -> {
                logger.debug("Starting to update auth0 for permission " + permission.getId());

                HashMap<String, Object> meta = new HashMap<>();
//            AsyncSubTask asyncSubTask =
//                    this.asyncTaskService.saveSubTask(new AsyncSubTask(), at,
//                            AsyncTaskDef.TASK_INITIATED,
//                            AsyncTaskDef.Task.NEW_UPDATE_PROVIDER, null);

                PermissionRequest permissionRequest = null;
                ResponseEntity responseEntity = null;
                PermissionResponse permissionResponse = null;

                //Get clientId/ secret for default tenant from comn-system-info
                TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);

                //Create Request Entity for POST/PUT requests
                permissionRequest = new PermissionRequest();
                NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
                try {
                    nullAwareBeanUtilsBean.copyProperties(permissionRequest, permission);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Set app id and app type
                permissionRequest.setApplicationId(tenantAuthProvider.getClientId());
                permissionRequest.setApplicationType("client");


                //get Auth0 response
                try {
                    responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, permissionRequest, PermissionResponse.class);

                    ProviderPermissionIdClass providerPermissionIdClass = new ProviderPermissionIdClass();
                    providerPermissionIdClass.setProvider(PROVIDER);
                    providerPermissionIdClass.setPermission(permission.getId());

                    //Create ProviderPermission record from POST response
                    permissionResponse = (PermissionResponse) responseEntity.getBody();
                    ProviderPermission providerPermission = new ProviderPermission();
                    providerPermission.setProviderPermissionIdClass(providerPermissionIdClass);
                    providerPermission.setProviderPermissionId(permissionResponse.get_id());
                    providerPermissionRepository.save(providerPermission);

                    logger.debug("Permission with id " + permission.getId() + " was successfully updated in " + PROVIDER);
                    meta.put("permission", permission);
                    meta.put("permissionId", permission.getId());
                    logger.debug("Data posted to Auth0");
//                this.asyncTaskService.saveSubTask(asyncSubTask,
//                        null,
//                        AsyncTaskDef.TASK_COMPLETED, null, meta);
                } catch (Exception ex) {
                    logger.debug("Permission with id " + permission.getId() + " was unable to be updated in " + PROVIDER);
                    ex.printStackTrace();
//                this.asyncTaskService.saveSubTask(asyncSubTask,
//                        null,
//                        AsyncTaskDef.TASK_ERROR, null, null);
//                this.asyncTaskService.saveSubTaskLog(asyncSubTask, "Auth0 update error",
//                        ex.getMessage(), "",
//                        ex.getMessage(), "Auth0 update error",

//                    new Exception(ex.getMessage()));
                }
            });
            TenantHolder.clear();
        }, executor);
    }

    @Override
    public CompletableFuture<?> updateAuth0BulkRoles(List<Role> roleList, HttpMethod httpMethod, String subpath, String tenant) {
        return CompletableFuture.runAsync(() -> {

            TenantHolder.setTenantId(tenant);
            roleList.forEach(role -> {


                        logger.debug("Starting to update auth0 for role " + role.getId());

//        HashMap<String, Object> meta = new HashMap<>();
//        AsyncSubTask asyncSubTask =
//                this.asyncTaskService.saveSubTask(new AsyncSubTask(), at,
//                        AsyncTaskDef.TASK_INITIATED,
//                        AsyncTaskDef.Task.NEW_UPDATE_PROVIDER, null);

                        RoleRequest roleRequest = null;
                        ResponseEntity responseEntity = null;
                        RoleResponse roleResponse = null;

                        //Get clientId/ secret for default tenant from comn-system-info
                        TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);

                        //Create Request Entity for POST/PUT requests
                        if (httpMethod != HttpMethod.DELETE) {
                            roleRequest = new RoleRequest();
                            NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
                            try {
                                nullAwareBeanUtilsBean.copyProperties(roleRequest, role);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            //Set app id and app type
                            roleRequest.setApplicationId(tenantAuthProvider.getClientId());
                            roleRequest.setApplicationType("client");
                            if (role.getRolePermissions() == null) {
                                roleRequest.setPermissions(new String[0]);
                            } else {
                                List<RolePermission> rolePermissionList = role.getRolePermissions();
                                List<String> permissionList = new ArrayList<>();
                                for (RolePermission rolePermission : rolePermissionList) {
                                    Permission permission = rolePermission.getPermission();
                                    if (providerPermissionRepository.existsByProviderPermissionIdClass_Permission(permission.getId())) {
                                        ProviderPermission providerPermission =
                                                providerPermissionRepository.findByProviderPermissionIdClass_Permission(permission.getId()).get();
                                        permissionList.add(providerPermission.getProviderPermissionId());
                                    }
                                }
                                String[] permissions = new String[permissionList.size()];
                                for (int i = 0; i < permissionList.size(); i++) {
                                    permissions[i] = permissionList.get(i);
                                }
                                roleRequest.setPermissions(permissions);
                            }
                            if (httpMethod.equals(HttpMethod.PUT)) {
                                List<RolePermission> rolePermissionList = role.getRolePermissions();
                                String[] permissionsArray = new String[rolePermissionList.size()];
                                logger.debug("list size " + rolePermissionList.size());
                                for (int i = 0; i < rolePermissionList.size(); i++) {
                                    Long apiPermissionId = rolePermissionList.get(i).getPermission().getId();
                                    logger.debug(apiPermissionId);
                                    if (providerPermissionRepository.existsByProviderPermissionIdClass_Permission(apiPermissionId)) {
                                        ProviderPermission providerPermission = providerPermissionRepository.findByProviderPermissionIdClass_Permission(apiPermissionId).get();
                                        permissionsArray[i] = providerPermission.getProviderPermissionId();
                                    }
                                }
                                roleRequest.setPermissions(permissionsArray);
                                logger.debug("after");
                            }
                        }

                        //get Auth0 response
                        try {
                            responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, roleRequest, RoleResponse.class);

                            ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
                            providerRoleIdClass.setProvider(PROVIDER);
                            providerRoleIdClass.setRole(role.getId());

                            //Create ProviderRole record from POST response
                            if (httpMethod.equals(HttpMethod.POST)) {
                                roleResponse = (RoleResponse) responseEntity.getBody();
                                ProviderRole providerRole = new ProviderRole();
                                providerRole.setProviderRoleIdClass(providerRoleIdClass);
                                providerRole.setProviderRoleId(roleResponse.get_id());
                                providerRoleRepository.save(providerRole);
                            }
//            meta.put("role", role);
//            meta.put("roleId", role.getId());
//            logger.debug("Data posted to Auth0");
//            this.asyncTaskService.saveSubTask(asyncSubTask,
//                    null,
//                    AsyncTaskDef.TASK_COMPLETED, null, meta);

                        } catch (Exception ex) {
                            logger.debug("Role with id " + role.getId() + " was unable to be updated in " + PROVIDER);
                            ex.printStackTrace();
//            this.asyncTaskService.saveSubTask(asyncSubTask,
//                    null,
//                    AsyncTaskDef.TASK_ERROR, null, null);
//            this.asyncTaskService.saveSubTaskLog(asyncSubTask, "Auth0 update error",
//                    ex.getMessage(), "",
//                    ex.getMessage(), "Auth0 update error",
//
//                    new Exception(ex.getMessage()));

                        }
                    });
                TenantHolder.clear();
            }, executor);
    }

    @Override
    public void updateAuth0BultRolesAndPermissions(Role roleList, Permission permission, HttpMethod httpMethod, String subpath, String tenant) {
            List<AddPermissionRequest> addPermissionRequests = new ArrayList<>();
            AddPermissionRequest addPermissionRequest;

            addPermissionRequest = new AddPermissionRequest();
            addPermissionRequest.setPermission_name(permission.getName());
            addPermissionRequest.setResource_server_identifier(EXTENSION_API_AUDIENCE);
            addPermissionRequests.add(addPermissionRequest);

           PermissionRoleRequest permissionRoleRequest = new PermissionRoleRequest();
            permissionRoleRequest.setPermissions(addPermissionRequests);
            ResponseEntity responseEntity = null;

            try {
                responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, permissionRoleRequest, PermissionResponse.class);
                System.out.println(responseEntity);

            } catch (FXDefaultException e) {
                e.printStackTrace();
            }

    }

}
