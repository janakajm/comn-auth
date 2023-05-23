package com.loits.comn.auth.services.impl;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.loits.comn.auth.commons.AsyncTaskDef;
import com.loits.comn.auth.commons.NullAwareBeanUtilsBean;
import com.loits.comn.auth.commons.RestResponsePage;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.core.RestTemplateResponseErrorHandler;
import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.dto.*;
import com.loits.comn.auth.helper.BulkUploadResponse;
import com.loits.comn.auth.mt.TenantHolder;
import com.loits.comn.auth.repo.*;
import com.loits.comn.auth.services.*;
import com.loits.comn.auth.services.projections.BasicRoleProjection;
import com.loits.comn.auth.services.projections.PermissionProjection;
import com.loits.comn.auth.services.projections.RoleProjectionWithPermissions;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.transaction.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class SyncServiceImpl implements SyncService {

    Logger logger = LogManager.getLogger(SyncServiceImpl.class);

    @Autowired
    RestTemplateBuilder builder;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    UserGroupRepository userGroupRepository;

    @Autowired
    UserProfileRepository userProfileRepository;

    @Autowired
    Executor executor;

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    RoleGroupRepository roleGroupRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserGroupProfileRepository userGroupProfileRepository;

    @Autowired
    RolePermissionRepository rolePermissionRepository;

    @Value("${loits.fusionx.comn.user.all}")
   // @Value("http://132.145.228.83/comn-user/%s/%s/all")
    private String COMN_USER_ALL_URL;
    
    @Value("${loits.fusionx.comn.user.id}")
    //@Value("http://132.145.228.83/comn-user/%s/%s/%s")
    private String COMN_USER_ID_URL;

    @Value("${loits.fusionx.comn.branch.all}")
    private String COMN_BRANCH_ALL_URL;

    @Value("${loits.fusionx.comn.user.by-group}")
    private String COMN_USER_BY_GROUP;

    @Value("${loits.fusionx.comn.user.identity-server}")
    //@Value("http://132.145.228.83/comn-user/user-identity-server/%s/userId/%s")
    private String COMN_USER_IDENTITY_SERVER;

    @Value("${auth.provider}")
    private String PROVIDER;

    @Value("${loits.branch.permission.name}")
    private String BRANCH_PERM_NAME;

    @Value("${loits.branch.permission.desc}")
    private String BRANCH_PERM_DESC;

    @Autowired
    UserGroupRoleGroupRepository userGroupRoleGroupRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    RoleGroupUserRepository roleGroupUserRepository;

    @Autowired
    ProviderRoleRepository providerRoleRepository;

    @Autowired
    UserService userService;

    @Autowired
    RoleService roleService;

    @Autowired
    AsyncTaskService asyncTaskService;

    @Autowired
    ProviderPermissionRepository providerPermissionRepository;

    @Autowired
    AuthService authService;

    @Override
    public Object synchBranches(String tenent) {
    	LoggerRequest.getInstance().logInfo("synchBranches : " + new Date());
    	
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        RestTemplate restTemplate = new RestTemplate();

        //sync response object
        SyncResponse syncResponse = new SyncResponse();
        syncResponse.setNoOfObjectsBeforeSync(branchRepository.count());

        //build url to get all temporary branches from LOITS comn-user
        //COMN_BRANCH_ALL_URL
        UriComponents builder = UriComponentsBuilder.fromHttpUrl(String.format(COMN_BRANCH_ALL_URL, tenent))
                .queryParam("size", Integer.MAX_VALUE).build();
        String url = builder.toUriString();

        //Send GET request for url
        Object object =
                restTemplate.
                          getForObject(url,
                        Object.class);

        //convert response entity to branch and get the
        if (object != null) {

            //convert page to list to object
            JavaType itemType = objectMapper.getTypeFactory().constructCollectionType(List.class, Branch.class);
            List<Branch> branchList = objectMapper.convertValue(object, itemType);

            //get returned list's size
            syncResponse.setNoOfObjectsRetrieved((long) branchList.size());

            //save to db
            Iterable<Branch> branchIterable = branchRepository.saveAll(branchList);

            //save synced branch objects
            syncResponse.setNoOfObjectsSynced((long) Iterables.size(branchIterable));

            //async operation to create permissions locally and in auth0
            createPermissions(branchList, tenent);

        } else {
            syncResponse.setNoOfObjectsRetrieved(0l);
        }
        return syncResponse;
    }

    @Async
    CompletableFuture<?> createPermissions(List<Branch> branchList, String tenent) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenent);
            String jobId = new Timestamp(new Date().getTime()).toString().concat(" branch permissions");
//            AsyncTask asyncTask =
//                    this.asyncTaskService.saveTask(new AsyncTask(), jobId,
//                            AsyncTaskDef.TASK_INITIATED,
//                            "Branch Permission Creation", null);

            List<Permission> permissionList = new ArrayList<>();
            List<Role> roleList = new ArrayList<>();
            for (Branch branch : branchList) {
                //Create permission
                Permission permission = null;
                if (!permissionRepository.existsByName(String.format(BRANCH_PERM_NAME, branch.getBranchCode()))) {
                    permission = new Permission();
                    permission.setName(String.format(BRANCH_PERM_NAME, branch.getBranchCode()));
                    permission.setDescription(String.format(BRANCH_PERM_DESC, branch.getBranchCode(), branch.getBranchName()));
                    permission.setCreatedBy("SYSTEM");
                    permission.setCreatedOn(new Timestamp(new Date().getTime()));
                    permissionRepository.save(permission);
                } else {
                    permission = permissionRepository.findByName(String.format(BRANCH_PERM_NAME, branch.getBranchCode()));
                }
                Role role = null;
                if (!roleRepository.existsByName(String.format(BRANCH_PERM_NAME, branch.getBranchCode()))) {
                    role = new Role();
                    role.setName(String.format(BRANCH_PERM_NAME, branch.getBranchCode()));
                    role.setDescription(String.format(BRANCH_PERM_DESC, branch.getBranchCode(), branch.getBranchName()));
                    role.setCreatedBy("SYSTEM");
                    role.setCreatedOn(new Timestamp(new Date().getTime()));
                    roleRepository.save(role);

                } else {
                    role = roleRepository.findByName(String.format(BRANCH_PERM_NAME, branch.getBranchCode())).get();
                }

                List<RolePermission> rolePermissionList = new ArrayList<>();
                //create role permission
                RolePermission rolePermission = new RolePermission();
                rolePermission.setPermission(permission);
                rolePermission.setRole(role);
                rolePermission.setCreatedOn(new Timestamp(new Date().getTime()));
                rolePermission.setCreatedBy("SYSTEM");
                RolePermissionId rolePermissionId = new RolePermissionId();
                rolePermissionId.setRoleId(role.getId());
                rolePermissionId.setPermissionId(permission.getId());
                rolePermission.setRolePermissionId(rolePermissionId);
                rolePermissionRepository.save(rolePermission);

                rolePermissionList.add(rolePermission);

                role.setRolePermissions(rolePermissionList);

                permissionList.add(permission);
                roleList.add(role);
            }
            //TODO call bulk async operation
//            authService.updateAuth0BulkPermissions(permissionList,HttpMethod.POST,"permissions", tenent).whenComplete((o, throwable) -> {
//                authService.updateAuth0BulkRoles(roleList, HttpMethod.POST, "roles", tenent).whenComplete((b, err) -> {
//                    assignPermissionsToRoles(roleList,tenent);
//                });
//            });
            authService.updateAuth0BulkPermissions(permissionList,HttpMethod.POST,"permissions", tenent);
            authService.updateAuth0BulkRoles(roleList, HttpMethod.POST, "roles", tenent);
            assignPermissionsToRoles(roleList,tenent);

            TenantHolder.clear();


        }, executor);
    }

    public void assignPermissionsToRoles(List<Role> roleList, String tenent){
        for(Role role: roleList){
            List<RolePermission> rolePermission = rolePermissionRepository.findByRoleId(role.getId());
            ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
            providerRoleIdClass.setRole(role.getId());
            providerRoleIdClass.setProvider(PROVIDER);
            if (providerRoleRepository.existsById(providerRoleIdClass)) {
                ProviderRole providerRole = providerRoleRepository.findById(providerRoleIdClass).get();

                String providerRoleId1 = providerRole.getProviderRoleId();

                String subPath = "roles" + "/" + providerRoleId1;
                //send async request to update auth0
                if (rolePermission.get(0).getPermission() != null) {
                    roleService.asyncUpdateOperations(role, rolePermission.get(0).getPermission(), HttpMethod.POST, subPath, tenent, true, "assign", true);
                }
            }
        }
    }

    @Override
    public Object syncUserGroups(String tenent) {
    	LoggerRequest.getInstance().logInfo("syncUserGroups : " + new Date());
    	
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        SyncResponse syncResponse = new SyncResponse();

        RestTemplate restTemplate = new RestTemplate();


        //get all user-groups from LOITS comn-user
        UriComponents builder = UriComponentsBuilder.fromHttpUrl(String.format(COMN_USER_ALL_URL, "user-group", tenent))
                .queryParam("size", Integer.MAX_VALUE).build();

        String url = builder.toUriString();

        //Send GET request with entity
        RestResponsePage page =
                restTemplate.getForObject(url,
                        RestResponsePage.class);

        if (page != null) {
            //convert page to list to object
            JavaType itemType = objectMapper.getTypeFactory().constructCollectionType(List.class, UserGroup.class);
            List<UserGroup> userGroupList = objectMapper.convertValue(page.getContent(), itemType);

            //get returned list's size
            syncResponse.setNoOfObjectsRetrieved((long) userGroupList.size());

            //save to db
            userGroupRepository.saveAll(userGroupList);
        } else {
            syncResponse.setNoOfObjectsRetrieved(0l);
        }
        return syncResponse;
    }

    @Override
    public Object syncUsers(String tenent) {
    	LoggerRequest.getInstance().logInfo("syncUsers : " + new Date());
    	
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        SyncResponse syncResponse = new SyncResponse();

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        //get all user-profiles from LOITS comn-user
        UriComponents builder = UriComponentsBuilder.fromHttpUrl(String.format(COMN_USER_ALL_URL, "user-profile", tenent))
                .queryParam("size", Integer.MAX_VALUE).build();

        String url = builder.toUriString();

        //Send GET request with entity
        RestResponsePage page =
                restTemplate.getForObject(url,
                        RestResponsePage.class);

        if (page != null) {
            //convert page to list to object
            JavaType itemType = objectMapper.getTypeFactory().constructCollectionType(List.class, UserProfile.class);
            List<UserProfile> userProfileList = objectMapper.convertValue(page.getContent(), itemType);
            syncResponse.setNoOfObjectsRetrieved((long) userProfileList.size());

            List<UserProfileIdentityServer> userProfileIdentityServerList = null;

            //get authentication providers for each user
            for (UserProfile userProfile : userProfileList) {
                //COMN_USER_IDENTITY_SERVER
                String userIdentityServerUrl = String.format(COMN_USER_IDENTITY_SERVER, tenent, userProfile.getId());
                //Send GET request with entity
                Object objectList =
                        restTemplate.getForObject(userIdentityServerUrl,
                                Object.class);
                if (objectList != null) {
                    //convert to list
                    JavaType identityServerListType =
                            objectMapper.getTypeFactory().constructCollectionType(List.class, UserProfileIdentityServer.class);

                    userProfileIdentityServerList =
                            objectMapper.convertValue(objectList, identityServerListType);

                    //set the identity servers for user
                    for(UserProfileIdentityServer us : userProfileIdentityServerList){
                        if(!us.getIdentityServer().contains("auth0")){
                            us.setNickName(null);
                        }
                    }

                    userProfileIdentityServerList.forEach(userProfileIdentityServer -> userProfileIdentityServer.setUserProfile(userProfile));
                }
                //if userprofile exists, copy properties and update, else save
                if (userProfileRepository.existsById(userProfile.getId())) {
                    UserProfile newUserProfile = userProfileRepository.findById(userProfile.getId()).get();
                    try {
                        NullAwareBeanUtilsBean beanUtilsBean = new NullAwareBeanUtilsBean();
                        HashSet<String> ignoreFieldsSet = new HashSet<>();
                        ignoreFieldsSet.add("userIdentityServerList");
                        ignoreFieldsSet.add("user");

                        beanUtilsBean.setIgnoreFields(ignoreFieldsSet);
                        beanUtilsBean.copyProperties(newUserProfile, userProfile);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    newUserProfile.getUserIdentityServerList().clear();

                    if (userProfileIdentityServerList != null) {
                        newUserProfile.getUserIdentityServerList().addAll(userProfileIdentityServerList);
                    }
                    userProfileRepository.save(newUserProfile);
                } else {
                    userProfile.setUserIdentityServerList(userProfileIdentityServerList);
                    userProfileRepository.save(userProfile);
                }

            }

            //get returned list's size
            syncResponse.setNoOfObjectsSynced((long) userProfileList.size());
        } else {
            syncResponse.setNoOfObjectsRetrieved(0l);
            syncResponse.setNoOfObjectsSynced(0l);
        }
        return syncResponse;
    }


    @Override
    public Object syncUserGroupUsers(String tenent) {
    	LoggerRequest.getInstance().logInfo("syncUserGroupUsers : " + new Date());
    	
        Iterable<UserGroup> userGroupsList = userGroupRepository.findAll();

        List<SyncResponse> syncResponseList = new ArrayList<>();

        RestTemplate restTemplate = new RestTemplate();

        for (UserGroup userGroup : userGroupsList) {

            //sample response after syncing
            SyncResponse syncResponse = new SyncResponse();
            syncResponse.setId(userGroup.getId());

            //get all temporary branches from LOITS comn-user
            //COMN_USER_BY_GROUP
            String url = String.format(COMN_USER_BY_GROUP, tenent, userGroup.getId());

            //Send GET request with entity
            UserGroup userGroupResponse =
                    restTemplate.getForObject(url,
                            UserGroup.class);


            //new usergroup profiles needs to be checked to see if users are removed from groups
            List<UserGroupProfile> newUserGroupProfileList = new ArrayList<>();

            //old usergroup profiles needs to be taken to verify if users have been removed
            Iterable<UserGroupProfile> oldUserGroupProfileList = userGroupProfileRepository.findAllByUserGroup(userGroup);

            //add new users to mapping
            if(userGroupResponse != null) {
                for (UserProfile userProfileResponse : userGroupResponse.getUsers()) {
                    if (userProfileRepository.existsById(userProfileResponse.getId())) {
                        //save new mapping
                        UserProfile userProfile = userProfileRepository.findById(userProfileResponse.getId()).get();
                        UserGroupProfile userGroupProfile = new UserGroupProfile();
                        userGroupProfile.setUserGroup(userGroup);
                        userGroupProfile.setUserProfile(userProfile);
                        newUserGroupProfileList.add(userGroupProfile);
                    }
                }
            //get returned list's size
                syncResponse.setNoOfObjectsRetrieved((long) userGroupResponse.getUsers().size());

                syncResponseList.add(syncResponse);

                //save usergroups with user profiles
                userGroupProfileRepository.saveAll(newUserGroupProfileList);

                //async operation to add/remove user-role mappings
                updateUserPermissions(oldUserGroupProfileList, newUserGroupProfileList, tenent, userGroup);

            }
        }
        return syncResponseList;
    }

    @Async
    CompletableFuture<?> updateUserPermissions(Iterable<UserGroupProfile> oldUserGroupProfiles,
                                               List<UserGroupProfile> newUserGroupProfiles, String tenant,UserGroup userGroup) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Updating user permissions with latest synced usergroup-user mappings");
            TenantHolder.setTenantId(tenant);

            for (UserGroupProfile newUserGroupProfile : newUserGroupProfiles) {
                boolean alreadyExisting = false;
                for (UserGroupProfile oldUserGroupProfile : oldUserGroupProfiles) {
                    if (newUserGroupProfile.getUserProfile() != null && oldUserGroupProfile.getUserProfile() != null
                            && newUserGroupProfile.getUserProfile().getId().equals(oldUserGroupProfile.getUserProfile().getId())) {
                        alreadyExisting = true;
                    }
                }

                if (!alreadyExisting) {
                    //get userprofile
                    UserProfile userProfile = newUserGroupProfile.getUserProfile();
                    //Create RoleGroup user record/ assign role-group to user

                    Iterable<UserGroupRoleGroup> userGroupRoleGroupIterable =
                            userGroupRoleGroupRepository.findByUserGroup(newUserGroupProfile.getUserGroup());

                    HashSet<Role> roleSet = new HashSet<>();

                    userGroupRoleGroupIterable.forEach(userGroupRoleGroup -> {
                        RoleGroup roleGroup = userGroupRoleGroup.getRoleGroup();


                        RoleGroupUser roleGroupUser = new RoleGroupUser();
                        RoleGroupUserId roleGroupUserId = new RoleGroupUserId();
                        roleGroupUserId.setUserId(userProfile.getId());
                        roleGroupUserId.setRoleGroupId(roleGroup.getId());
                        roleGroupUser.setRoleGroupUserId(roleGroupUserId);
                        roleGroupUser.setRoleGroup(roleGroup);
                        roleGroupUser.setUser(userProfile);
                        roleGroupUser.setExpires(userGroupRoleGroup.getExpires());
                        roleGroupUser.setDelegatable(userGroupRoleGroup.getDelegatable());

                        //audit log
                        roleGroupUser.setCreatedBy("SYSTEM");
                        roleGroupUser.setCreatedOn(new Timestamp(new Date().getTime()));
                        roleGroupUser.setUserGroupId(userGroup.getId());
                        // add rolegroup to list
                        roleGroupUserRepository.save(roleGroupUser);

                        //get rolegrouproles from rolegroup
                        List<RoleGroupRole> roleGroupRoleList = roleGroup.getRoleGroupRoles();

                        //Assign roles to user
                        for (RoleGroupRole roleGroupRole : roleGroupRoleList) {
                            Role role = roleGroupRole.getRole();
                            if (!userRoleRepository.existsByUserAndRoleAndRoleGroupAndUserGroupId(userProfile, role, roleGroup,userGroup.getId())) {
                                UserRole userRole = new UserRole();
                                UserRoleId userRoleId = new UserRoleId();

                                //basic properties
                                userRole.setUserRoleId(userRoleId);
                                userRoleId.setRoleId(role.getId());
                                userRoleId.setUserId(userProfile.getId());
                                userRoleId.setRoleGroupId(roleGroup.getId());
                                userRole.setRole(role);
                                userRole.setUser(userProfile);
                                userRole.setRoleGroup(roleGroup);

                                //expiretime and delegability
                                userRole.setExpires(userGroupRoleGroup.getExpires());
                                userRole.setDelegatable(userGroupRoleGroup.getDelegatable());

                                //audit log
                                userRole.setCreatedBy("SYSTEM");
                                userRole.setCreatedOn(new Timestamp(new Date().getTime()));

                                userRole.setStatus((byte) 1);

                                // add userrole to list
                                userRoleRepository.save(userRole);

                                roleSet.add(role);
                            }
                        }
                    });
                    if(roleSet.size() !=0) {
                        userService.updateAuthUser(userProfile.getId(), roleSet, HttpMethod.PATCH, tenant);
                    }
                }
            }

            for (UserGroupProfile oldUserGroupProfile : oldUserGroupProfiles) {
                boolean alreadyExisting = false;
                for (UserGroupProfile newUserGroupProfile : newUserGroupProfiles) {
                    if (newUserGroupProfile.getUserProfile() != null && oldUserGroupProfile.getUserProfile() != null
                            && newUserGroupProfile.getUserProfile().getId().equals(oldUserGroupProfile.getUserProfile().getId())) {
                        alreadyExisting = true;
                    }
                }

                //removed user
                if (!alreadyExisting) {
                    //get userprofile
                    UserProfile userProfile = oldUserGroupProfile.getUserProfile();
                    //Create RoleGroup user record/ assign role-group to user

                    Iterable<UserGroupRoleGroup> userGroupRoleGroupIterable =
                            userGroupRoleGroupRepository.findByUserGroup(oldUserGroupProfile.getUserGroup());

                    HashSet<Role> roleSet = new HashSet<>();

                    userGroupRoleGroupIterable.forEach(userGroupRoleGroup -> {
                        RoleGroup roleGroup = userGroupRoleGroup.getRoleGroup();
                        RoleGroupUser roleGroupUser =
                                roleGroupUserRepository.getByRoleGroupIdAndUserId(roleGroup.getId(), userProfile.getId());
                        if (roleGroupUserRepository.isDuplicatebyUserGroupId(userProfile, roleGroup, userGroup.getId())) {
                            roleGroupUserRepository.delete(roleGroupUser);
                        }

                        List<UserRole> userRoles = userRoleRepository.findAllByUserAndRoleGroupAndUserGroupId(userProfile, roleGroup, userGroup.getId());
                        userRoleRepository.deleteAll(userRoles);
                        for (UserRole userRole : userRoles) {
                            if (!userRoleRepository.isDuplicateNot(userRole.getUser(), userRole.getRole(), roleGroup)) {
                                if (!userRoleRepository.checkForDuplicateRolesByRole(userProfile, userRole.getRole())) {
                                    roleSet.add(userRole.getRole());
                                }
                            }
                        }
                    });
                    if (roleSet.size() != 0) {
                        userService.updateAuthUser(userProfile.getId(), roleSet, HttpMethod.DELETE, tenant);
                    }
                }
            }

            TenantHolder.clear();
        }, executor);
    }

	@Override
	public Object syncUsersByProfileId(String tenent, Long profileId) {
		
    	LoggerRequest.getInstance().logInfo("syncUsers : " + new Date());
    	
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        SyncResponse syncResponse = new SyncResponse();

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        //get all user-profiles from LOITS comn-user
        UriComponents builder = UriComponentsBuilder.fromHttpUrl(String.format(COMN_USER_ID_URL, "user-profile", tenent,profileId)).queryParam("size", 1).build();

        String url = builder.toUriString();

        //Send GET request with entity
        UserProfile userProfile = restTemplate.getForObject(url,UserProfile.class);

        if (userProfile != null) {
            //convert page to list to object
            JavaType itemType = objectMapper.getTypeFactory().constructCollectionType(List.class, UserProfile.class);
            //List<UserProfile> userProfileList = objectMapper.convertValue(page.getContent(), itemType);
            syncResponse.setNoOfObjectsRetrieved((long) 1L);

            List<UserProfileIdentityServer> userProfileIdentityServerList = null;

            //get authentication providers for each user
            
            
            //for (UserProfile userProfile : userProfileList) {
                //COMN_USER_IDENTITY_SERVER
                String userIdentityServerUrl = String.format(COMN_USER_IDENTITY_SERVER, tenent, userProfile.getId());
                //Send GET request with entity
                Object objectList =restTemplate.getForObject(userIdentityServerUrl,Object.class);
                if (objectList != null) {
                    //convert to list
                    JavaType identityServerListType =objectMapper.getTypeFactory().constructCollectionType(List.class, UserProfileIdentityServer.class);

                    userProfileIdentityServerList =objectMapper.convertValue(objectList, identityServerListType);

                    //set the identity servers for user
                    for(UserProfileIdentityServer us : userProfileIdentityServerList){
                        if(!us.getIdentityServer().contains("auth0")){
                            us.setNickName(null);
                        }
                    }

                    userProfileIdentityServerList.forEach(userProfileIdentityServer -> userProfileIdentityServer.setUserProfile(userProfile));
                }
                //if userprofile exists, copy properties and update, else save
                if (userProfileRepository.existsById(userProfile.getId())) {
                    UserProfile newUserProfile = userProfileRepository.findById(userProfile.getId()).get();
                    try {
                        NullAwareBeanUtilsBean beanUtilsBean = new NullAwareBeanUtilsBean();
                        HashSet<String> ignoreFieldsSet = new HashSet<>();
                        ignoreFieldsSet.add("userIdentityServerList");
                        ignoreFieldsSet.add("user");

                        beanUtilsBean.setIgnoreFields(ignoreFieldsSet);
                        beanUtilsBean.copyProperties(newUserProfile, userProfile);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    newUserProfile.getUserIdentityServerList().clear();

                    if (userProfileIdentityServerList != null) {
                        newUserProfile.getUserIdentityServerList().addAll(userProfileIdentityServerList);
                    }
                    userProfileRepository.save(newUserProfile);
                } else {
                    userProfile.setUserIdentityServerList(userProfileIdentityServerList);
                    userProfileRepository.save(userProfile);
                }

            //}

            //get returned list's size
            syncResponse.setNoOfObjectsSynced((long) 1l);
            
        } else {
            syncResponse.setNoOfObjectsRetrieved(0l);
            syncResponse.setNoOfObjectsSynced(0l);
        }
        return syncResponse;
	}
}
