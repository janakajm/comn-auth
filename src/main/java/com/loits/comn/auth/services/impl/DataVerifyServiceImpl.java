package com.loits.comn.auth.services.impl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loits.comn.auth.commons.AsyncTaskDef;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.dto.*;
import com.loits.comn.auth.mt.TenantHolder;
import com.loits.comn.auth.repo.*;
import com.loits.comn.auth.services.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

@Service
public class DataVerifyServiceImpl implements DataVerifyService {

    Logger logger = LogManager.getLogger(DataVerifyServiceImpl.class);

    @Autowired
    UserProfileRepository userProfileRepository;

    @Autowired
    PermissionService permissionService;

    @Autowired
    RoleService roleService;

    @Autowired
    HttpService httpService;

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    RoleGroupRepository roleGroupRepository;

    @Autowired
    RoleGroupService roleGroupService;

    @Value("${auth.provider}")
    private String PROVIDER;

    @Value("${loits.identity.server.name.auth0}")
    private String IDENTITY_SERVER_NAME_AUTH0;

    @Autowired
    ProviderPermissionRepository providerPermissionRepository;

    @Autowired
    ProviderRoleRepository providerRoleRepository;

    @Autowired
    AsyncTaskService asyncTaskService;

    @Autowired
    RolePermissionRepository rolePermissionRepository;

    @Autowired
    RoleGroupRoleRepository roleGroupRoleRepository;

    @Autowired
    UserRoleRepository userRoleRepository;
    
    
    @Override
    public Object syncPermissions(String tenent) throws FXDefaultException {
    	LoggerRequest.getInstance().logInfo("syncPermissions : " + new Date());
    	
        //get all permissions from auth0
        Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
        Page<?> permissionResponsePage = permissionService.getAllFromExtension(pageable, null, null, null, tenent);
        List<PermissionResponse> authPermissions =
                new ObjectMapper().convertValue(permissionResponsePage.getContent(),
                        new TypeReference<List<PermissionResponse>>() {
                        });


        //Get clientId/ secret for default tenant from comn-system-info
        TenantAuthProvider tenantAuthProvider;
        try {
            tenantAuthProvider = httpService.getTenantAuthProvider(tenent, PROVIDER);
        }catch(Exception e){
            e.printStackTrace();
        }
        tenantAuthProvider = httpService.getTenantAuthProvider(tenent, PROVIDER);

        String clienId = tenantAuthProvider.getClientId();

        //Get all permissions in tenant db
        Iterable<Permission> permssionList = permissionRepository.findAll();

        //get remote objects for clientid (for tenant)
        int remoteObjectsCount = 0;
        for (PermissionResponse permissionResponse : authPermissions) {
            if (permissionResponse.getApplicationId().equalsIgnoreCase(clienId)) {
                remoteObjectsCount++;
            }
        }
        //Create meta data for async task
        VerificationTaskMetaData verificationTaskMetaData = new VerificationTaskMetaData();
        verificationTaskMetaData.setLocalObjects(((Collection<?>) permssionList).size());
        verificationTaskMetaData.setRemoteObjects(remoteObjectsCount);
        HashMap<String, Object> metaMap = new HashMap<>();
        metaMap.put("stats", verificationTaskMetaData);

        //Save AsyncTask on start of the process
        // Start new Async task
        String jobId = new Timestamp(new Date().getTime()).toString().concat(" permission");
        AsyncTask asyncTask =
                this.asyncTaskService.saveTask(new AsyncTask(), jobId,
                        AsyncTaskDef.TASK_INITIATED,
                        AsyncTaskDef.Task.NEW_AUTH0_PERM_VERIFICATION, metaMap);
        HashMap<String, Object> subMetaMap = new HashMap<>();


        //Permissionnames map is created with key name and value availability in auth0
        Map<String, Boolean> permissionNames = new HashMap();
        permssionList.forEach(permission -> permissionNames.put(permission.getName(), false));

        //iterate through authpermissions
        for (PermissionResponse permissionResponse : authPermissions) {
            //consider only authpermissions of tenant
            if (permissionResponse.getApplicationId().equalsIgnoreCase(clienId)) {
                if (!permissionNames.containsKey(permissionResponse.getName())) {
                    //if auth0 permission is not available locally
                    //this cannot be retried from API as local permission is not available
                    subMetaMap.clear();
                    SyncObject syncObject = new SyncObject("Permission", permissionResponse.getName());
                    subMetaMap.put("auth0 permission", syncObject);
                    AsyncSubTask asyncSubTask =
                            this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                    AsyncTaskDef.TASK_INITIATED,
                                    AsyncTaskDef.Task.AUTH0_PERM_LOCAL_UNAVAILABILITY, subMetaMap);
                } else {
                    //indicate that the local permission is available on auth0
                    permissionNames.replace(permissionResponse.getName(), true);
                }
            }
        }

        if (permissionNames.containsValue(false)) {
            logger.info("LOCAL_PERM_AUTH0_UNAVAILABILITY");
            for (Map.Entry<String, Boolean> entry : permissionNames.entrySet()) {
                if (entry.getValue().equals(false)) {
                    //local has a permission not available on auth0
                    subMetaMap.clear();
                    SyncObject syncObject = new SyncObject("Permission", entry.getKey());
                    subMetaMap.put("object", syncObject);
                    AsyncSubTask asyncSubTask =
                            this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                    AsyncTaskDef.TASK_INITIATED,
                                    AsyncTaskDef.Task.LOCAL_PERM_AUTH0_UNAVAILABILITY, subMetaMap);
                    for(Permission permission:permssionList){
                        if(permission.getName().equals(entry.getKey())){
                           // permissionService.asyncUpdateOperations(permission,HttpMethod.POST,"permissions",tenent,true,true);
                         
                            try {/*amal*/
                            	 permissionService.updateOperations(permission,"SYS", HttpMethod.POST, "permissions", tenent, true, true);
                             } catch (Exception exception) {
                                 exception.printStackTrace();
                             }

                            System.out.println("updated Auth 0" + permission.getName());
                            break;
                        }
                    }


                }
            }
        }
        SyncResponse syncResponse = new SyncResponse();
        //get returned list's size
        syncResponse.setNoOfObjectsRetrieved((long) authPermissions.size());
        return syncResponse;
    }

    @Override
    public Object syncRoles(String tenent) throws FXDefaultException {
    	LoggerRequest.getInstance().logInfo("syncRoles : " + new Date());
    	
        //get all roles from auth0
        Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
        Page<?> roleResponsePage = roleService.getAllFromExtension(pageable, null, null, null);
        List<RoleResponse> authRoles =
                new ObjectMapper().convertValue(roleResponsePage.getContent(),
                        new TypeReference<List<RoleResponse>>() {
                        });


        //Get clientId/ secret for default tenant from comn-system-info
        TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenent, PROVIDER);
        String clienId = tenantAuthProvider.getClientId();

        //Get all roles in tenant db
        Iterable<Role> roleList = roleRepository.findAll();

        //get remote objects for clientid (for tenant)
        int remoteObjectsCount = 0;
        for (RoleResponse roleResponse : authRoles) {
            if (roleResponse.getApplicationId().equalsIgnoreCase(clienId)) {
                remoteObjectsCount++;
            }
        }

        //Create meta data for async task
        VerificationTaskMetaData verificationTaskMetaData = new VerificationTaskMetaData();
        verificationTaskMetaData.setLocalObjects(((Collection<?>) roleList).size());
        verificationTaskMetaData.setRemoteObjects(remoteObjectsCount);
        HashMap<String, Object> metaMap = new HashMap<>();
        metaMap.put("stats", verificationTaskMetaData);

        //Save AsyncTask on start of the process
        // Start new Async task
        String jobId = new Timestamp(new Date().getTime()).toString().concat(" role");
        AsyncTask asyncTask =
                this.asyncTaskService.saveTask(new AsyncTask(), jobId,
                        AsyncTaskDef.TASK_INITIATED,
                        AsyncTaskDef.Task.NEW_AUTH0_ROLE_VERIFICATION, metaMap);
        HashMap<String, Object> subMetaMap = new HashMap<>();


        //Rolenames map is created with key name and value availability in auth0
        Map<String, Boolean> roleNames = new HashMap();
        roleList.forEach(role -> roleNames.put(role.getName(), false));

        //iterate through authroles
        for (RoleResponse roleResponse : authRoles) {
            //consider only authroles of tenant
            if (roleResponse.getApplicationId().equalsIgnoreCase(clienId)) {
                if (!roleNames.containsKey(roleResponse.getName())) {
                    //if auth0 role is not available locally
                    //this cannot be retried from API as local role is not available
                    subMetaMap.clear();
                    SyncObject syncObject = new SyncObject("Role", roleResponse.getName());
                    subMetaMap.put("auth0 role", syncObject);
                    AsyncSubTask asyncSubTask =
                            this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                    AsyncTaskDef.TASK_INITIATED,
                                    AsyncTaskDef.Task.AUTH0_ROLE_LOCAL_UNAVAILABILITY, subMetaMap);
                } else {
                    //indicate that the local role is available on auth0
                    roleNames.replace(roleResponse.getName(), true);

                    //check validity of permission mappings
                    String[] authPermissionsForRole = roleResponse.getPermissions();
                    Role role = roleRepository.findByName(roleResponse.getName()).get();
                    //check if all authpermissions for role is available locally
                    for (int j = 0; j < authPermissionsForRole.length; j++) {
                        if (providerPermissionRepository.existsByProviderPermissionId(authPermissionsForRole[j])) {
                            ProviderPermission providerPermission =
                                    providerPermissionRepository.findByProviderPermissionId(authPermissionsForRole[j]).get();

                            Long permissionId = providerPermission.getProviderPermissionIdClass().getPermission();
                            Permission permission = permissionRepository.findById(permissionId).get();

                            //role permission mapping doesn't exist locally
                            if (!rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permissionId)) {
                                subMetaMap.clear();

                                SyncObject syncObject = new SyncObject("Role", roleResponse.getName());
                                SyncObject syncObject2 = new SyncObject("Permission", permission.getName());

                                subMetaMap.put("auth0 role", syncObject);
                                subMetaMap.put("auth0 permission", syncObject2);

                                AsyncSubTask asyncSubTask =
                                        this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                                AsyncTaskDef.TASK_INITIATED,
                                                AsyncTaskDef.Task.AUTH0_ROLE_PERMISSION_LOCAL_UNAVAILABILITY, subMetaMap);
                            }
                        }
                    }

                    List<RolePermission> rolePermissionList = rolePermissionRepository.findByRoleId(role.getId());
                    //check if local permissions for role is available on auth0
                    for (RolePermission rolePermission : rolePermissionList) {
                        Permission permission = rolePermission.getPermission();

                        if (providerPermissionRepository.existsByProviderPermissionIdClass_Permission(permission.getId())) {
                            ProviderPermission providerPermission = providerPermissionRepository.findByProviderPermissionIdClass_Permission(permission.getId()).get();
                            boolean available = false;
                            for (int j = 0; j < authPermissionsForRole.length; j++) {
                                if (authPermissionsForRole[j].equalsIgnoreCase(providerPermission.getProviderPermissionId())) {
                                    available = true;
                                }
                            }
                            if (!available) {

                                subMetaMap.clear();

                                SyncObject syncObject = new SyncObject("Role", roleResponse.getName());
                                SyncObject syncObject2 = new SyncObject("Permission", permission.getName());

                                subMetaMap.put("Role", syncObject);
                                subMetaMap.put("Permission", syncObject2);

                                AsyncSubTask asyncSubTask =
                                        this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                                AsyncTaskDef.TASK_INITIATED,
                                                AsyncTaskDef.Task.LOCAL_ROLE_PERMISSION_AUTH0_UNAVAILABILITY, subMetaMap);
                            }
                        }
                    }

                }
            }
        }

        if (roleNames.containsValue(false)) {
            for (Map.Entry<String, Boolean> entry : roleNames.entrySet()) {
                if (entry.getValue().equals(false)) {
                    //local has a role not available on auth0
                    subMetaMap.clear();
                    SyncObject syncObject = new SyncObject("Role", entry.getKey());
                    subMetaMap.put("object", syncObject);
                    AsyncSubTask asyncSubTask =
                            this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                    AsyncTaskDef.TASK_INITIATED,
                                    AsyncTaskDef.Task.LOCAL_ROLE_AUTH0_UNAVAILABILITY, subMetaMap);
                }
            }
        }
        SyncResponse syncResponse = new SyncResponse();
        //get returned list's size
        syncResponse.setNoOfObjectsRetrieved((long) authRoles.size());
        return syncResponse;
    }

    @Override
    public Object syncGroups(String tenent) throws FXDefaultException {
    	LoggerRequest.getInstance().logInfo("syncGroups : " + new Date());
    	
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        //get all groups from auth0
        Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
        Page<?> groupResponsePage = roleGroupService.getAllFromExtension(pageable, null, null, null);
        List<GroupResponse> authGroups =
                objectMapper.convertValue(groupResponsePage.getContent(),
                        new TypeReference<List<GroupResponse>>() {
                        });

        //Get all groups in tenant db
        Iterable<RoleGroup> localGroupList = roleGroupRepository.findAll();

        int remoteObjectsCount = 0;
        //verify if only remote groups for tenant
        for (GroupResponse groupResponse : authGroups) {
            //consider only authgroups of tenant
            if (groupResponse.getName().toLowerCase().contains(tenent.toLowerCase())) {
                remoteObjectsCount++;
            }
        }
        //Create meta data for async task
        VerificationTaskMetaData verificationTaskMetaData = new VerificationTaskMetaData();
        verificationTaskMetaData.setLocalObjects(((Collection<?>) localGroupList).size());
        verificationTaskMetaData.setRemoteObjects(remoteObjectsCount);
        HashMap<String, Object> metaMap = new HashMap<>();
        metaMap.put("stats", verificationTaskMetaData);

        //Save AsyncTask on start of the process
        // Start new Async task
        String jobId = new Timestamp(new Date().getTime()).toString().concat(" group");
        AsyncTask asyncTask =
                this.asyncTaskService.saveTask(new AsyncTask(), jobId,
                        AsyncTaskDef.TASK_INITIATED,
                        AsyncTaskDef.Task.NEW_AUTH0_GROUP_VERIFICATION, metaMap);
        HashMap<String, Object> subMetaMap = new HashMap<>();


        //Groupnames map is created with key name and value availability in auth0
        Map<String, Boolean> localGroupNames = new HashMap();
        localGroupList.forEach(group -> localGroupNames.put(group.getName(), false));

        //iterate through authgroups
        for (GroupResponse groupResponse : authGroups) {

            //consider only authgroups of tenant
            if (groupResponse.getName().toLowerCase().contains(tenent.toLowerCase())) {
                //split to remove tenant
                String[] authRolesForGroup = groupResponse.getRoles();
                String groupNameWithTenant = groupResponse.getName();
                String authGroupNameArray[] = groupNameWithTenant.split(":", 2);

                if(authGroupNameArray.length==2) {
                    if (!localGroupNames.containsKey(authGroupNameArray[1])) {
                        //if auth0 group is not available locally
                        //this cannot be retried from API as local group is not available
                        subMetaMap.clear();
                        SyncObject syncObject = new SyncObject("Group", groupResponse.getName());
                        subMetaMap.put("Auth0 Group", syncObject);
                        AsyncSubTask asyncSubTask =
                                this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                        AsyncTaskDef.TASK_INITIATED,
                                        AsyncTaskDef.Task.AUTH0_GROUP_LOCAL_UNAVAILABILITY, subMetaMap);
                    } else {
                        //indicate that the local group is available on auth0
                        localGroupNames.replace(authGroupNameArray[1], true);

                        //check validity of permission mappings
                        RoleGroup roleGroup = roleGroupRepository.findByName(authGroupNameArray[1]).get();

                        //check if all auth permissions for role is available locally
                        if (authRolesForGroup != null) {
                            for (int j = 0; j < authRolesForGroup.length; j++) {
                                if (providerRoleRepository.existsByProviderRoleId(authRolesForGroup[j])) {
                                    ProviderRole providerRole =
                                            providerRoleRepository.findByProviderRoleId(authRolesForGroup[j]).get();

                                    Long roleId = providerRole.getProviderRoleIdClass().getRole();
                                    Role role = providerRole.getRole();

                                    //role permission mapping doesn't exist locally
                                    if (!roleGroupRoleRepository.existsByRoleGroupIdAndRoleId(roleGroup.getId(), roleId)) {
                                        subMetaMap.clear();

                                        SyncObject syncObject = new SyncObject("Group", roleGroup.getName());
                                        SyncObject syncObject2 = new SyncObject("Role", role.getName());

                                        subMetaMap.put("Group", syncObject);
                                        subMetaMap.put("Role", syncObject2);
                                        AsyncSubTask asyncSubTask =
                                                this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                                        AsyncTaskDef.TASK_INITIATED,
                                                        AsyncTaskDef.Task.AUTH0_GROUP_ROLE_LOCAL_UNAVAILABILITY, subMetaMap);
                                    }
                                }
                            }

                            List<RoleGroupRole> roleGroupRoles = roleGroupRoleRepository.findByRoleGroupId(roleGroup.getId());
                            //check if local permissions for role is available on auth0
                            for (RoleGroupRole roleGroupRole : roleGroupRoles) {
                                Role role = roleGroupRole.getRole();

                                if (providerRoleRepository.existsByProviderRoleIdClass_Role(role.getId())) {
                                    ProviderRole providerRole = providerRoleRepository.findByProviderRoleIdClass_Role(role.getId()).get();
                                    boolean available = false;
                                    for (int j = 0; j < authRolesForGroup.length; j++) {
                                        if (authRolesForGroup[j].equalsIgnoreCase(providerRole.getProviderRoleId())) {
                                            available = true;
                                        }
                                    }
                                    if (!available) {
                                        subMetaMap.clear();

                                        SyncObject syncObject = new SyncObject("Group", roleGroup.getName());
                                        SyncObject syncObject2 = new SyncObject("Role", role.getName());

                                        subMetaMap.put("Group", syncObject);
                                        subMetaMap.put("Role", syncObject2);

                                        AsyncSubTask asyncSubTask =
                                                this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                                        AsyncTaskDef.TASK_INITIATED,
                                                        AsyncTaskDef.Task.LOCAL_GROUP_ROLE_AUTH0_UNAVAILABILITY, subMetaMap);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

        if (localGroupNames.containsValue(false)) {
            for (Map.Entry<String, Boolean> entry : localGroupNames.entrySet()) {
                if (entry.getValue().equals(false)) {
                    //local has a group not available on auth0
                    subMetaMap.clear();
                    SyncObject syncObject = new SyncObject("Group", entry.getKey());
                    subMetaMap.put("Group", syncObject);
                    AsyncSubTask asyncSubTask =
                            this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                    AsyncTaskDef.TASK_INITIATED,
                                    AsyncTaskDef.Task.LOCAL_GROUP_AUTH0_UNAVAILABILITY, subMetaMap);
                }
            }
        }
        SyncResponse syncResponse = new SyncResponse();
        //get returned list's size
        syncResponse.setNoOfObjectsRetrieved((long) authGroups.size());
        return syncResponse;
    }

    @Override
    public Object syncUserRoles(String tenent) throws FXDefaultException {
    	LoggerRequest.getInstance().logInfo("syncUserRoles : " + new Date());
    	
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        HashMap<String, Object> subMetaMap = new HashMap<>();

        Iterable<UserProfile> userProfileList = userProfileRepository.findAll();

        //Get clientId/ secret for default tenant from comn-system-info
        TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenent, PROVIDER);
        String clienId = tenantAuthProvider.getClientId();

        HashMap<String, Object> metaMap = new HashMap<>();
        metaMap.put("stats", null);

        //Save AsyncTask on start of the process
        // Start new Async task
        String jobId = new Timestamp(new Date().getTime()).toString().concat(" user-role");
        AsyncTask asyncTask =
                this.asyncTaskService.saveTask(new AsyncTask(), jobId,
                        AsyncTaskDef.TASK_INITIATED,
                        AsyncTaskDef.Task.NEW_AUTH0_USER_ROLE_VERIFICATION, metaMap);

        //iterate through all user profiles
        userProfileList.forEach(userProfile -> {
            List<UserProfileIdentityServer> identityServers = userProfile.getUserIdentityServerList();

            UserProfileIdentityServer authIdentityServerObj = null;

            //find auth0 identity server
            for (UserProfileIdentityServer userProfileIdentityServer : identityServers) {
                if (userProfileIdentityServer.getIdentityServer().equalsIgnoreCase(IDENTITY_SERVER_NAME_AUTH0)) {
                    authIdentityServerObj = userProfileIdentityServer;
                    break;
                }
            }

            //check if user has identityserver record
            if (authIdentityServerObj != null) {
                //get all userroles and extract role's authid to add to hashmap for comparison
                List<UserRole> userRoleList = userRoleRepository.findAllByUser(userProfile);
                HashMap<String, Role> localUserRoleAuthIds = new HashMap<>();
                userRoleList.forEach(
                        userRole -> {
                            Role role = userRole.getRole();
                            if (providerRoleRepository.existsByProviderRoleIdClass_Role(role.getId())) {
                                ProviderRole providerRole = providerRoleRepository.findByProviderRoleIdClass_Role(role.getId()).get();
                                localUserRoleAuthIds.put(providerRole.getProviderRoleId(), providerRole.getRole());
                            }

                        }
                );

                //get identity server user id
                String providerUserId = authIdentityServerObj.getUserIdentityServersId();

                String subpath = "/users/" + providerUserId.toLowerCase() + "/roles/calculate";
                logger.debug(subpath);

                //send request to update provider
                try {
                    ResponseEntity<Object> responseEntity = httpService.sendProviderRestRequest(HttpMethod.GET, subpath, null, Object.class);

                    List<UserRoleResponse> userRoleResponseList = objectMapper.convertValue(responseEntity.getBody(), new TypeReference<ArrayList<UserRoleResponse>>() {
                    });


                    for (UserRoleResponse authRoleResponse : userRoleResponseList) {
                        if (authRoleResponse.getApplicationId().equalsIgnoreCase(clienId)) {
                            //auth0 user role mapping not available locally
                            if (!localUserRoleAuthIds.containsKey(authRoleResponse.get_id())) {
                                subMetaMap.clear();
                                subMetaMap.put("Role", authRoleResponse);
                                AsyncSubTask asyncSubTask =
                                        this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                                AsyncTaskDef.TASK_INITIATED,
                                                AsyncTaskDef.Task.AUTH0_USER_ROLE_LOCAL_UNAVAILABILITY, subMetaMap);
                            }
                        }
                    }

                    //local userRole mapping not available in auth0
                    for (Map.Entry<String, Role> localAuthId : localUserRoleAuthIds.entrySet()) {
                        boolean available = false;
                        for (UserRoleResponse userRoleResponse : userRoleResponseList) {
                            if (localAuthId.getKey().equalsIgnoreCase(userRoleResponse.get_id())) {
                                available = true;
                            }
                        }
                        if (!available) {
                            subMetaMap.clear();
                            SyncObject syncObject = new SyncObject("Role", localAuthId.getValue().getName());
                            subMetaMap.put("Role", syncObject);
                            AsyncSubTask asyncSubTask =
                                    this.asyncTaskService.saveSubTask(new AsyncSubTask(), asyncTask,
                                            AsyncTaskDef.TASK_INITIATED,
                                            AsyncTaskDef.Task.LOCAL_USER_ROLE_AUTH0_UNAVAILABILITY, subMetaMap);
                        }
                    }

                } catch (FXDefaultException e) {
                    e.printStackTrace();
                }
            }
        });


        SyncResponse syncResponse = new SyncResponse();
        //get returned list's size
        syncResponse.setNoOfObjectsRetrieved(0l);
        return syncResponse;
    }

	
}
