package com.loits.comn.auth.services.impl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.dto.AddUserGroups;
import com.loits.comn.auth.dto.AppMetaData;
import com.loits.comn.auth.dto.BranchAssignment;
import com.loits.comn.auth.dto.RemoveUser;
import com.loits.comn.auth.dto.UserMap;
import com.loits.comn.auth.dto.Authorization;
import com.loits.comn.auth.dto.UserRoleResponse;
import com.loits.comn.auth.mt.TenantHolder;
import com.loits.comn.auth.repo.*;
import com.loits.comn.auth.services.HistoryService;
import com.loits.comn.auth.services.HttpService;
import com.loits.comn.auth.services.TokenService;
import com.loits.comn.auth.services.UserService;
import com.loits.comn.auth.services.projections.BasicRoleGroupProjection;
import com.loits.comn.auth.services.projections.RoleGroupUserProjection;
import com.loits.comn.auth.services.projections.UserProjection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.NoSuchFileException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class UserServiceImpl implements UserService {

    Logger logger = LogManager.getLogger(UserServiceImpl.class);

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    HttpService httpService;

    @Autowired
    UserProfileRepository userProfileRepository;

    @Autowired
    RoleGroupRepository roleGroupRepository;

    @Autowired
    RoleGroupUserRepository roleGroupUserRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    ProviderRoleRepository providerRoleRepository;
    @Autowired
    Executor executor;

    @Autowired
    BranchRepository branchRepository;

    @Value("${auth0.authorization.extension.api.url}")
    private String EXTENSION_API_URL;

    @Value("${default.expire.years}")
    private int DEFAULT_EXPIRE_YEARS;

    @Value("${auth.provider}")
    private String PROVIDER;

    @Value("${loits.branch.permission.name}")
    private String BRANCH_PERM_NAME;

    @Value("${loits.identity.server.name.auth0}")
    private String IDENTITY_SERVER_NAME_AUTH0;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserProfileBranchRepository userProfileBranchRepository;

    @Autowired
    UserProfileIdentityServerRepository userProfileIdentityServerRepository;

    @Autowired
    UserGroupRepository userGroupRepository;

    @Autowired
    UserGroupRoleGroupRepository userGroupRoleGroupRepository;

    @Override
    public Page<?> getAll(Pageable pageable, String bookmarks, Predicate predicate, String projection, String search) {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QUserProfile qUser = QUserProfile.userProfile;

        //split and separate ids sent as a string
        if (!StringUtils.isEmpty(bookmarks)) {
            ArrayList<Long> ids = new ArrayList<>();
            for (String id : bookmarks.split(",")) {
                ids.add(Long.parseLong(id));
            }
            bb.and(qUser.id.in(ids));
        }

        //update pageable object to ignorecase in sorting
        Sort sort = pageable.getSort();
        sort.forEach(order -> order.ignoreCase());
        PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);

        //search by fields on demand
        if(search!=null && !search.isEmpty()){
            bb.and(qUser.userName.containsIgnoreCase(search).or(qUser.email.containsIgnoreCase(search)
                    .or(qUser.userId.containsIgnoreCase(search)).or(qUser.employeeNumber.containsIgnoreCase(search))));
        }

        return userProfileRepository.findAll(bb.getValue(), pageRequest).map(
                userProfile -> projectionFactory.createProjection(UserProjection.class, userProfile)
        );
    }

    @Override
    public Page<?> getAllDelegatableRoles(Pageable pageable, String bookmarks, String projection,String search, String user, Predicate predicate, String tenent) throws FXDefaultException {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QRoleGroup qRoleGroup = QRoleGroup.roleGroup;
        ObjectMapper objectMapper = new ObjectMapper();
        boolean isAdmin = false;

        logger.debug("USER ",user);
        logger.info("USER ",user);
        logger.warn("USER ",user);
        System.out.println("USER "+ user);
        UserProfileIdentityServer userProfileIdentityServer = userProfileIdentityServerRepository.findByNickName(user).fetchFirst();


        if (userProfileIdentityServer == null) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_IDENTITY_SERVER_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (!userProfileRepository.findById(userProfileIdentityServer.getUserProfile().getId()).isPresent()) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_USER_PROFILE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }


        String subpath = "users/" + userProfileIdentityServer.getUserIdentityServersId() + "/roles/calculate";

        JavaType itemType = objectMapper.getTypeFactory().constructCollectionType(List.class, UserRoleResponse.class);

        ResponseEntity<Object> responseEntity = httpService.sendProviderRestRequest(HttpMethod.GET, subpath, null, Object.class);

        List<UserRoleResponse> userRoleResponseList = objectMapper.convertValue(responseEntity.getBody(),itemType);

        for(UserRoleResponse userRoleResponse: userRoleResponseList){
            if (userRoleResponse.getName().equals("AllPermission")) {
                isAdmin = true;
                break;
            }
        }
        if(isAdmin){
            Sort sort = pageable.getSort();
            sort.forEach(order -> order.ignoreCase());
            PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);
            return roleGroupRepository.findAll(bb.getValue(), pageRequest).map(
                    roleGroup -> projectionFactory.createProjection(BasicRoleGroupProjection.class, roleGroup)
            );
        }else {

            UserProfile usr = userProfileRepository.findById(userProfileIdentityServer.getUserProfile().getId()).get();

            //List<UserRole>  userRoles = userRoleRepository.getDelegatableRoles(usr).fetch();

            List<RoleGroupUser> roleGroupUserList = roleGroupUserRepository.getDelegatableRoles(usr).fetch();
            List<RoleGroup> createdRoleGroupList = roleGroupRepository.findByCreatedBy(user);

            List<Long> roleGroupIds = new ArrayList<>();

            for(RoleGroup roleGroup: createdRoleGroupList){
                roleGroupIds.add(roleGroup.getId());
            }

            for (RoleGroupUser roleGroupUser : roleGroupUserList) {
                RoleGroup roleGroup = roleGroupUser.getRoleGroup();
                if (!roleGroupIds.contains(roleGroup.getId())) {
                    roleGroupIds.add(roleGroup.getId());
                }
            }

            bb.and(qRoleGroup.id.in(roleGroupIds));

            if (search != null && !search.isEmpty()) {
                bb.and(qRoleGroup.name.containsIgnoreCase(search));
            }

            Sort sort = pageable.getSort();
            sort.forEach(order -> order.ignoreCase());
            PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);

            return roleGroupRepository.findAll(bb.getValue(), pageRequest).map(
                    roleGroup -> projectionFactory.createProjection(BasicRoleGroupProjection.class, roleGroup)
            );
        }

        }

    @Override
    public Iterable<?> assign(String projection, List<AddUserGroups> addUserGroupsList, String user, String tenent) throws FXDefaultException {
        //Role list for auth0 update
        System.out.println("CALLED");
        
        List<RoleGroupUser> roleGroupUserList = new ArrayList<>();
        HashMap<UserProfile, HashSet<Role>> profileRoleMap = new HashMap<>();

        //Iterate through users
        for (AddUserGroups addUserGroups : addUserGroupsList) {
            //Check if user is available in db
            if (!userProfileRepository.existsById(addUserGroups.getUser())) {
                throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_USER_PROFILE_ID"), new Date(), HttpStatus.BAD_REQUEST);
            }
            UserProfile userProfile = userProfileRepository.findById(addUserGroups.getUser()).get();

            //HashSet<Role> roleSet = new HashSet<>();

            //iterate through groups
            for (AddUserGroups.AddGroup group : addUserGroups.getGroups()) {
                //Check group availability
                if (!roleGroupRepository.existsById(group.getGroup())) {
                    throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
                }
                RoleGroup roleGroup = roleGroupRepository.findById(group.getGroup()).get();
                Timestamp expireTime = null;

                //determine expire time
                if (group.getExpires() != null) {
                    expireTime = group.getExpires();
                } else {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new Date());
                    calendar.add(Calendar.YEAR, DEFAULT_EXPIRE_YEARS);
                    expireTime = new Timestamp(calendar.getTime().getTime());
                }

                List<RoleGroupRole> roleGroupRoleList = roleGroup.getRoleGroupRoles();

                //Create RoleGroup user record/ assign role-group to user
                RoleGroupUser roleGroupUser = new RoleGroupUser();
                RoleGroupUserId roleGroupUserId = new RoleGroupUserId();
                roleGroupUserId.setUserId(userProfile.getId());
                roleGroupUserId.setRoleGroupId(roleGroup.getId());
                roleGroupUser.setRoleGroupUserId(roleGroupUserId);
                roleGroupUser.setRoleGroup(roleGroup);
                roleGroupUser.setUser(userProfile);
                roleGroupUser.setExpires(expireTime);
                roleGroupUser.setDelegatable(group.getDelegatable());

                //audit log
                roleGroupUser.setCreatedBy(user);
                roleGroupUser.setCreatedOn(new Timestamp(new Date().getTime()));
                roleGroupUser.setUserGroupId(null);
                // add rolegroup to list
                roleGroupUserList.add(roleGroupUser);
                System.out.println("CALLED2");

                //Assign roles to user
                for (RoleGroupRole roleGroupRole : roleGroupRoleList) {
                	HashSet<Role> roleSet = new HashSet<>();
                	List<UserRole> userRoleList = new ArrayList<>();
                	
                    Role role = roleGroupRole.getRole();
                    if (!userRoleRepository.isDuplicate(userProfile, role, roleGroup)) {
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
                        userRole.setExpires(expireTime);
                        userRole.setDelegatable(group.getDelegatable());

                        //audit log
                        userRole.setCreatedBy(user);
                        userRole.setCreatedOn(new Timestamp(new Date().getTime()));

                        //TODO status?
                        userRole.setStatus((byte) 1);

                        // add userrole to list
                        userRoleList.add(userRole);
                        System.out.println("CALLED3");
                        roleSet.add(role);
                        
                        try {
                        	updateAuthUser(userProfile.getId(), roleSet, HttpMethod.PATCH, tenent,userRoleList);
                        	userRoleRepository.saveAll(userRoleList);
						} catch (Exception e) {
							System.out.println("EXCEPTION "+e);
						}
                        
                    }
                }
            }
            //profileRoleMap.put(userProfile, roleSet);
        }

        //updateAuth0 for all valid records
        /*for (Map.Entry<UserProfile, HashSet<Role>> entry : profileRoleMap.entrySet()) {
            //send request to update auth0 roles in user
            System.out.println("CALLED4");
            if (entry.getValue().size() != 0) {
                System.out.println("CALLED5");
                updateAuthUser(entry.getKey() != null ? entry.getKey().getId() : null, entry.getValue(), HttpMethod.PATCH, tenent);
            }
        }*/

        //save all mappings
        roleGroupUserRepository.saveAll(roleGroupUserList);
        
        //userRoleRepository.saveAll(userRoleList);
        

        return addUserGroupsList;
    }

    @Override
    public Iterable<?> removeBulk(String projection, Long roleGroupId, List<RemoveUser> removeUserList, String user, String tenent) throws FXDefaultException {
        if (!roleGroupRepository.existsById(roleGroupId)) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        HashMap<UserProfile, HashSet<Role>> profileRoleMap = new HashMap<>();

        RoleGroup roleGroup = roleGroupRepository.findById(roleGroupId).get();
        List<RoleGroupUser> roleGroupUserList = new ArrayList<>();
        List<UserRole> userRoleList = new ArrayList<>();
        for (RemoveUser removeUser : removeUserList) {
            if (!userProfileRepository.existsById(removeUser.getUser())) {
                throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_USER_PROFILE_ID"), new Date(), HttpStatus.BAD_REQUEST);
            }
            //removed roles to send to auth0
            HashSet<Role> roleSet = new HashSet<>();
            UserProfile authUser = userProfileRepository.findById(removeUser.getUser()).get();
            List<RoleGroupRole> roleGroupRoleList = roleGroup.getRoleGroupRoles();

            if(!roleGroupUserRepository.isDuplicate(authUser, roleGroup)){
                throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_USER"), new Date(), HttpStatus.BAD_REQUEST);
            }


            for (RoleGroupRole roleGroupRole : roleGroupRoleList) {
                Role role = roleGroupRole.getRole();
                UserRole userRole = userRoleRepository.getUniqueUserRole(authUser, role, roleGroup).fetchFirst();
                userRoleList.add(userRole);
                if(!userRoleRepository.isDuplicateNot(authUser, role, roleGroup)) {
                    if(!userRoleRepository.checkForDuplicateRolesByRole(authUser,role)) {
                        roleSet.add(role);
                    }
                }
            }
            profileRoleMap.put(authUser, roleSet);

            RoleGroupUser roleGroupUser = roleGroupUserRepository.getByRoleGroupIdAndUserId(roleGroupId, authUser.getId());
            if(roleGroupUserRepository.isDuplicate(authUser,roleGroup)) {
                roleGroupUserList.add(roleGroupUser);
            }

        }
        roleGroupUserRepository.deleteAll(roleGroupUserList);
        userRoleRepository.deleteAll(userRoleList);


        //for(UserRole role : )

        //updateAuth0 for all valid records
        for (Map.Entry<UserProfile, HashSet<Role>> entry : profileRoleMap.entrySet()) {
            //send request to update auth0 roles in user
            if(entry.getValue().size() != 0) {
                updateAuthUser(entry.getKey() != null ? entry.getKey().getId() : null, entry.getValue(), HttpMethod.DELETE, tenent);
            }
        }

        return removeUserList;
    }

    @Override
    public Iterable<?> update(String projection, List<AddUserGroups> addUserGroupsList, String user, String tenent) throws FXDefaultException {
        List<UserRole> userRoleList = new ArrayList<>();
        List<RoleGroupUser> roleGroupUserList = new ArrayList<>();
        for (AddUserGroups addUserGroups : addUserGroupsList) {
            if (!userProfileRepository.existsById(addUserGroups.getUser())) {
                throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_USER_PROFILE_ID"), new Date(), HttpStatus.BAD_REQUEST);
            }
            UserProfile authUser = userProfileRepository.findById(addUserGroups.getUser()).get();

            for (AddUserGroups.AddGroup group : addUserGroups.getGroups()) {
                if (!roleGroupRepository.existsById(group.getGroup())) {
                    throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
                }
                RoleGroup roleGroup = roleGroupRepository.findById(group.getGroup()).get();

                Timestamp expireTime;

                //determine expire time
                if (group.getExpires() != null) {
                    expireTime = group.getExpires();
                } else {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new Date());
                    calendar.add(Calendar.YEAR, DEFAULT_EXPIRE_YEARS);
                    expireTime = new Timestamp(calendar.getTime().getTime());
                }

                List<RoleGroupRole> roleGroupRoleList = roleGroup.getRoleGroupRoles();

                //update rolegroup
                if (roleGroupUserRepository.isDuplicate(authUser,roleGroup)) {
                    RoleGroupUser roleGroupUser = roleGroupUserRepository.getByRoleGroupIdAndUserId(roleGroup.getId(), authUser.getId());
                    roleGroupUser.setExpires(expireTime);
                    roleGroupUser.setDelegatable(group.getDelegatable());
                    roleGroupUserList.add(roleGroupUser);
                }

                //update userroles
                for (RoleGroupRole roleGroupRole : roleGroupRoleList) {
                    Role role = roleGroupRole.getRole();
                    if (userRoleRepository.isDuplicate(authUser, role, roleGroup)) {
                        UserRole userRole = userRoleRepository.getUniqueUserRole(authUser, role, roleGroup).fetchFirst();
                        userRole.setExpires(expireTime);
                        userRole.setDelegatable(group.getDelegatable());
                        userRoleList.add(userRole);
                    }
                }
            }

        }

        //save all mappings
        roleGroupUserRepository.saveAll(roleGroupUserList);
        userRoleRepository.saveAll(userRoleList);

        return addUserGroupsList;
    }

    @Override
    public Object removeExpired(String projection, String user, String tenent) {

        Iterable<UserGroup> userGroupsList = userGroupRepository.findAll();
        for(UserGroup userGroup:userGroupsList){
            Iterable<UserGroupRoleGroup> userGroupRoleGroupList = userGroupRoleGroupRepository.findByUserGroup(userGroup);
            for(UserGroupRoleGroup userGroupRoleGroup: userGroupRoleGroupList){
                if(userGroupRoleGroup.getExpires()!=null && userGroupRoleGroup.getExpires().getTime() <= new Date().getTime()){
                    userGroupRoleGroupRepository.delete(userGroupRoleGroup);
                }
            }
        }


        Iterable<UserProfile> userList = userProfileRepository.findAll();

        for (UserProfile authUser : userList) {
            List<RoleGroupUser> roleGroupUsersList = authUser.getUserRoleGroups();
            for (RoleGroupUser roleGroupUser : roleGroupUsersList) {
                if (roleGroupUser.getExpires() != null && roleGroupUser.getExpires().getTime() <= new Date().getTime()) {
                    if(roleGroupUser.getUserGroupId() != null) {
                        if (roleGroupUserRepository.isDuplicatebyUserGroupId(authUser, roleGroupUser.getRoleGroup(), roleGroupUser.getUserGroupId())) {
                            roleGroupUserRepository.delete(roleGroupUser);
                        }
                    }else {
                        if (roleGroupUserRepository.isDuplicate(authUser, roleGroupUser.getRoleGroup())) {
                            roleGroupUserRepository.delete(roleGroupUser);
                        }
                    }
                }
            }

            List<UserRole> userRoleList = authUser.getUserRoles();
            for (UserRole userRole : userRoleList) {
                if (userRole.getExpires() != null && userRole.getExpires().getTime() <= new Date().getTime()) {
                    HashSet<Role> hashSet = new HashSet<>();
                    if(userRole.getUserGroupId() != null) {
                        if(!roleGroupUserRepository.isDuplicate(authUser,userRole.getRoleGroup())) {
                            hashSet.add(userRole.getRole());
                        }
                        userRoleRepository.delete(userRole);
                    }
                    if(hashSet.size() > 0){
                        updateAuthUser(userRole.getUser().getId(),hashSet, HttpMethod.DELETE, tenent);
                    }
                }
            }
            List<UserProfileBranch> userProfileBranchList= userProfileBranchRepository.findAllByUserProfile(authUser);
            for(UserProfileBranch userProfileBranch: userProfileBranchList) {
                if(userProfileBranch.getToDate() != null) {
                    if (userProfileBranch.getToDate().getTime() <= new Date().getTime()) {
                        userProfileBranchRepository.delete(userProfileBranch);
                        if (roleRepository.existsByName(String.format(BRANCH_PERM_NAME, userProfileBranch.getBranch().getBranchCode()))) {
                            //get role with branchcode name
                            Role role = roleRepository.findByName(String.format(BRANCH_PERM_NAME, userProfileBranch.getBranch().getBranchCode())).get();
                            HashSet<Role> hashSet = new HashSet<>();
                            hashSet.add(role);
                            if(hashSet.size() >0) {
                                updateAuthUser(userProfileBranch.getUserProfile().getId(), hashSet, HttpMethod.DELETE, tenent);
                            }
                        }

                    }
                }

            }
        }
        return true;
    }

    @Override
    public Object getLoggedInUser(String bookmarks, String user, Pageable pageable, String projection) throws FXDefaultException {
        if (!userProfileRepository.existsByEmail(user)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_USER"), new Date(), HttpStatus.BAD_REQUEST);
        }

        UserProfile authUser = userProfileRepository.findByEmail(user).get();


        return roleGroupUserRepository.findAllByDelegatableAndUser((byte) 1, authUser, pageable).map(
                roleGroupUser -> projectionFactory.createProjection(RoleGroupUserProjection.class, roleGroupUser)
        );
    }

    @Override
    //@Async
    public void updateAuthUser(Long userId, HashSet<Role> roleHashSet, HttpMethod httpMethod, String tenant,List<UserRole> userRoleList) {
            logger.debug("Starting to update auth0 with user roles for user "+userId);
            System.out.println("ROLESHASH "+ roleHashSet.size());
            TenantHolder.setTenantId(tenant);
            //get user-profile's identity server list
            List<UserProfileIdentityServer> userIdentityServerList = userProfileIdentityServerRepository.findAllByUserProfile_Id(userId);

            UserProfileIdentityServer authIdentityServerObj = null;
            //check availability of auth0 identity server record
        System.out.println("ID "+ userId);

        System.out.println("LENGTH "+ userIdentityServerList.size());
        for (UserProfileIdentityServer userProfileIdentityServer : userIdentityServerList) {
                String abc = IDENTITY_SERVER_NAME_AUTH0;
                if (userProfileIdentityServer.getIdentityServer().equalsIgnoreCase("auth0-Username-Password-Authentication")) {
                    System.out.println("FOUND "+ userProfileIdentityServer.getNickName());
                    authIdentityServerObj = userProfileIdentityServer;
                    break;
                }
            }

            //check if user has identityserver record
            if (authIdentityServerObj != null) {
                //get identity server user id
                String providerUserId = authIdentityServerObj.getUserIdentityServersId();
                //create roleid array list
                List<String> roles = new ArrayList<>();

                //add role ids to role id array list
                for (Role role : roleHashSet) {
                    if (providerRoleRepository.existsByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(role.getId(), PROVIDER)) {
                        ProviderRole providerRole = providerRoleRepository.findByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(role.getId(), PROVIDER).get();
                        System.out.println("RoleADDED "+ providerRole.getProviderRoleId());
                        roles.add(providerRole.getProviderRoleId());
                    }
                }

                String subpath = "users/" + providerUserId + "/roles";

                //send request to update provider
                try {
                	if(!roles.isEmpty() && !userRoleList.isEmpty()) {
                		ResponseEntity responseEntity =httpService.sendProviderRestRequest(httpMethod, subpath, roles, String.class);
                	}else {
                		System.out.print("*****ROLE IS EMPTY***** "+subpath);
                	}
                } catch (FXDefaultException e) {
                    e.printStackTrace();
                }
            }
            TenantHolder.clear();
    }
    
    @Override
    public void updateAuthUser(Long userId, HashSet<Role> roleHashSet, HttpMethod httpMethod, String tenant) {
            logger.debug("Starting to update auth0 with user roles for user "+userId);
            System.out.println("ROLESHASH "+ roleHashSet.size());
            TenantHolder.setTenantId(tenant);
            //get user-profile's identity server list
            List<UserProfileIdentityServer> userIdentityServerList = userProfileIdentityServerRepository.findAllByUserProfile_Id(userId);

            UserProfileIdentityServer authIdentityServerObj = null;
            //check availability of auth0 identity server record
        System.out.println("ID "+ userId);

        System.out.println("LENGTH "+ userIdentityServerList.size());
        for (UserProfileIdentityServer userProfileIdentityServer : userIdentityServerList) {
                String abc = IDENTITY_SERVER_NAME_AUTH0;
                if (userProfileIdentityServer.getIdentityServer().equalsIgnoreCase("auth0-Username-Password-Authentication")) {
                    System.out.println("FOUND "+ userProfileIdentityServer.getNickName());
                    authIdentityServerObj = userProfileIdentityServer;
                    break;
                }
            }

            //check if user has identityserver record
            if (authIdentityServerObj != null) {
                //get identity server user id
                String providerUserId = authIdentityServerObj.getUserIdentityServersId();
                //create roleid array list
                List<String> roles = new ArrayList<>();

                //add role ids to role id array list
                for (Role role : roleHashSet) {
                    if (providerRoleRepository.existsByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(role.getId(), PROVIDER)) {
                        ProviderRole providerRole = providerRoleRepository.findByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(role.getId(), PROVIDER).get();
                        System.out.println("RoleADDED "+ providerRole.getProviderRoleId());
                        roles.add(providerRole.getProviderRoleId());
                    }
                }
                
                String subpath = "users/" + providerUserId + "/roles";
                //String subpath = "/users/" + providerUserId + "/roles";
                //logger.debug(subpath);
                //System.out.println( "/users/" + providerUserId.toLowerCase() + "/roles");


                //send request to update provider
                try {
                	if(!roles.isEmpty()) {
                		ResponseEntity responseEntity =httpService.sendProviderRestRequest(httpMethod, subpath, roles, String.class);	
                	}else {
                		System.out.print("*****ROLE IS EMPTY***** "+subpath);
                	}
                } catch (FXDefaultException e) {
                    e.printStackTrace();
                }
            }
            TenantHolder.clear();
    }


    @Override
    public Object assignBranch(String projection, Long id, Long branchId, BranchAssignment branchAssignment, String user, String tenent) throws FXDefaultException {
        //check user availability
        if (!userProfileRepository.existsById(id)) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_USER_PROFILE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }
        UserProfile userProfile = userProfileRepository.findById(id).get();

        //check branch availability
        if (!branchRepository.existsById(branchId)) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_BRANCH_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Branch branch = branchRepository.findById(branchId).get();

//        if (userProfileBranchRepository.existsByUserProfileAndBranch(userProfile, branch)) {
//            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("DUPLICATE_BRANCH_USER"), new Date(), HttpStatus.BAD_REQUEST);
//        }

        //create new branch record for userprofile
        List<UserProfileBranch> userBranches = userProfileBranchRepository.findAllByUserProfile(userProfile);

        for(UserProfileBranch userBranch : userBranches){
            int x = userBranch.getStatus();
            if(userBranch.getStatus() ==1 ){
                if(userBranch.getBranch().getBranchCode().equals(branch.getBranchCode())) {
                    if (branchAssignment.getStatus() == 1) {
                        throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("USER_ALREADY_HAS_AN_ACTIVE_BRANCH"), new Date(), HttpStatus.BAD_REQUEST);
                    }
                }
            }
        }

        UserProfileBranch userProfileBranch = new UserProfileBranch();
        userProfileBranch.setUserProfile(userProfile);
        userProfileBranch.setBranch(branch);

        //set branch properties such as from date/to date and status
        if (branchAssignment != null) {
            userProfileBranch.setFromDate(branchAssignment.getFromDate());
            userProfileBranch.setToDate(branchAssignment.getToDate());
            userProfileBranch.setStatus(branchAssignment.getStatus());
        }
        //create id class and set
//        UserProfileBranchId userProfileBranchId = new UserProfileBranchId();
//        userProfileBranchId.setBranchId(branchId);
//        userProfileBranchId.setUserProfileId(id);
//        userProfileBranch.setUserProfileBranchId(userProfileBranchId);

        //audit log
        userProfileBranch.setCreatedOn(new Timestamp(new Date().getTime()));
        userProfileBranch.setCreatedBy(user);

        //save usernbranch record
        userProfileBranchRepository.save(userProfileBranch);

        //create userrole mapping locally and in auth0
        if(branchAssignment.getStatus() == 1) {
            assignBranchRoleToUser(userProfile, branch, branchAssignment, user, tenent);
        }
        return userProfileBranch;
    }

    @Async
    CompletableFuture<?> assignBranchRoleToUser(UserProfile userProfile, Branch branch, BranchAssignment branchAssignment, String user, String tenant) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenant);
            //find role containing branch code
            if (!roleRepository.existsByName(String.format(BRANCH_PERM_NAME, branch.getBranchCode()))) {
                try {
                    throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_BRANCH_PERM"), new Date(), HttpStatus.BAD_REQUEST);
                } catch (FXDefaultException e) {
                    logger.debug("permission with given branch code not available, aborting process");
                    e.printStackTrace();
                }
            }
            //get role with branchcode name
            Role role = roleRepository.findByName(String.format(BRANCH_PERM_NAME, branch.getBranchCode())).get();
            logger.debug(role.getName());

            //add role to set for auth0 update
            HashSet<Role> roleSet = new HashSet<>();
            //role for auth0 update
            roleSet.add(role);

            //update role of user in auth0
            if(roleSet.size() > 0) {
                this.updateAuthUser(userProfile.getId(), roleSet, HttpMethod.PATCH, tenant);
            }
            TenantHolder.clear();
        }, executor);
    }

    @Override
    public Object updateBranch(String projection, Long id, Long branchId, BranchAssignment branchAssignment, String user, String tenent) throws FXDefaultException {
        HashSet<Role> roleSet = new HashSet<>();

        //check user availability
        if (!userProfileRepository.existsById(id)) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_USER_PROFILE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }
        UserProfile userProfile = userProfileRepository.findById(id).get();

        //check branch availability
        if (!branchRepository.existsById(branchId)) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_BRANCH_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Branch branch = branchRepository.findById(branchId).get();

        //check if mapping exists
        if (!userProfileBranchRepository.existsById(branchAssignment.getUserBranchId())) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_BRANCH_USER"), new Date(), HttpStatus.BAD_REQUEST);
        }

        UserProfileBranch userProfileBranch = userProfileBranchRepository.findById(branchAssignment.getUserBranchId()).get();

        List<UserProfileBranch> userBranches = userProfileBranchRepository.findAllByUserProfile(userProfile);


        for(UserProfileBranch userBranch : userBranches){
            if(userBranch.getStatus() == 1){
                if(userBranch.getBranch().getBranchCode().equals(branch.getBranchCode())) {
                    if (branchAssignment.getStatus() == 1) {
                        throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("USER_ALREADY_HAS_AN_ACTIVE_BRANCH"), new Date(), HttpStatus.BAD_REQUEST);
                    }
                }
            }
        }
        //update status of the branch
        if (branchAssignment != null) {
            userProfileBranch.setStatus(branchAssignment.getStatus());
        }

        //audit log
        userProfileBranch.setModifiedOn(new Timestamp(new Date().getTime()));
        userProfileBranch.setModifiedBy(user);

        //update userprofile branch record
        userProfileBranchRepository.save(userProfileBranch);

        // update status of role-user mapping and remove role from auth0
        updateBranchRoleOfUser(userProfileBranch, userProfile, branch, branchAssignment, user, tenent);

        return userProfileBranch;
    }

    @Async
    CompletableFuture<?> updateBranchRoleOfUser(UserProfileBranch userProfileBranch, UserProfile userProfile, Branch branch, BranchAssignment branchAssignment, String user, String tenant) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenant);

            //get role with branchcode name
            Role role = roleRepository.findByName(String.format(BRANCH_PERM_NAME, branch.getBranchCode())).get();
            logger.debug(role.getName());
            //add role to set for auth0 update
            HashSet<Role> roleSet = new HashSet<>();
            //role for auth0 update
            roleSet.add(role);

            //update role of user in auth0
            if (userProfileBranch.getStatus()==(byte)1 && roleSet.size() >0) {
                this.updateAuthUser(userProfile.getId(), roleSet, HttpMethod.PATCH, tenant);
            } else if (userProfileBranch.getStatus()==(byte)0 && roleSet.size() >0) {
                this.updateAuthUser(userProfile.getId(), roleSet, HttpMethod.DELETE, tenant);
            }

            TenantHolder.clear();
        }, executor);
    }

    @Override
    public Object getBranches(String projection, Long id, String user, String tenent) throws FXDefaultException {
        //check user availability
        if (!userProfileRepository.existsById(id)) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_USER_PROFILE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }
        UserProfile userProfile = userProfileRepository.findById(id).get();

        Iterable<UserProfileBranch> userProfileBranches = userProfileBranchRepository.findAllByUserProfile(userProfile);
        return userProfileBranches;
    }

    @Override
    public Object getRoles(String projection, String username,String searchKey,String user, String tenent) throws FXDefaultException {
        UserProfile usr = new UserProfile();
        List<String> rolesList = new ArrayList<>();
        logger.debug("username" + username);


        UserProfileIdentityServer userProfileIdentityServer = userProfileIdentityServerRepository.findByNickName(username).fetchFirst();


        if(userProfileIdentityServer == null){
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_IDENTITY_SERVER_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }
        logger.debug("userProfileIdentity" + userProfileIdentityServer.getUserProfile().getId());
        if(userProfileRepository.findById(userProfileIdentityServer.getUserProfile().getId()).isPresent()) {
            logger.debug("FOUND");

            usr = userProfileRepository.findById(userProfileIdentityServer.getUserProfile().getId()).get();
            List<UserRole> userRoles;
            logger.debug("searchkey" + searchKey);

            if(searchKey != null) {
                logger.debug("usr" + usr.getId());

                userRoles = userRoleRepository.getRolesByKey(usr, searchKey).fetch();
            }else{
                userRoles = userRoleRepository.findAllByUser(usr);
            }
            logger.debug("usr length" + userRoles.size());

            for(UserRole role : userRoles){
                if(this.roleRepository.findById(role.getRole().getId()).isPresent()) {
                    Long abc = role.getRole().getId();
                    Role roleInDb = this.roleRepository.findById(role.getRole().getId()).get();
                    rolesList.add(roleInDb.getName());
                }
            }
            return rolesList;
        }else {
            return null;
        }
    }

    @Override
    public Object getDelegatableRoles(String projection, String username,String searchKey,String user, String tenent) throws FXDefaultException {
        UserProfile usr = new UserProfile();
        List<String> rolesList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        boolean isAdmin = false;

        UserProfileIdentityServer userProfileIdentityServer = userProfileIdentityServerRepository.findByNickName(username).fetchFirst();

        if(userProfileIdentityServer == null){
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_IDENTITY_SERVER_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        //check if user is a superAdmin
        String subpath = "/users/" + userProfileIdentityServer.getUserIdentityServersId() + "/roles/calculate";

        JavaType itemType = objectMapper.getTypeFactory().constructCollectionType(List.class, UserRoleResponse.class);


        ResponseEntity<Object> responseEntity = httpService.sendProviderRestRequest(HttpMethod.GET, subpath, null, Object.class);

        List<UserRoleResponse> userRoleResponseList = objectMapper.convertValue(responseEntity.getBody(),itemType);

        for(UserRoleResponse userRoleResponse: userRoleResponseList){
            if (userRoleResponse.getName().equals("AllPermission")) {
                isAdmin = true;
                break;
            }
        }
        if(isAdmin){
            Iterable<Role> allRoles = roleRepository.findAll();
            for(Role role : allRoles){
                rolesList.add(role.getName());
            }
            return rolesList;
        }else {
            logger.debug("userProfileIdentity" + userProfileIdentityServer.getUserProfile().getId());
            List<Role> userCreatedRoles = roleRepository.findByCreatedBy(username);

            for(Role role: userCreatedRoles){
                rolesList.add(role.getName());
            }
            if (userProfileRepository.findById(userProfileIdentityServer.getUserProfile().getId()).isPresent()) {
                logger.debug("FOUND");

                usr = userProfileRepository.findById(userProfileIdentityServer.getUserProfile().getId()).get();
                List<UserRole> userRoles;
                logger.debug("searchkey" + searchKey);

                if (searchKey != null) {
                    logger.debug("usr" + usr.getId());

                    userRoles = userRoleRepository.getRolesByKey(usr, searchKey).fetch();
                } else {
                    userRoles = userRoleRepository.findAllByUser(usr);
                }
                logger.debug("usr length" + userRoles.size());

                for (UserRole role : userRoles) {
                    if (this.roleRepository.findById(role.getRole().getId()).isPresent()) {
                        Long abc = role.getRole().getId();
                        Role roleInDb = this.roleRepository.findById(role.getRole().getId()).get();
                        rolesList.add(roleInDb.getName());
                    }
                }
                return rolesList;
            } else {
                return null;
            }
        }
    }
    
	@Override
	public Authorization getAllUserDetails(Pageable pageable, String projection,String userId, Predicate predicate, String tenent) throws FXDefaultException {
		logger.debug("Starting to update auth0 with user roles for user " + userId);

		TenantHolder.setTenantId(tenent);
		// get user-profile's identity server list
		List<UserProfileIdentityServer> userIdentityServerList = userProfileIdentityServerRepository.findAllByUserProfile_UserId(userId);

		UserProfileIdentityServer authIdentityServerObj = null;
		// check availability of auth0 identity server record
		System.out.println("ID " + userId);

		System.out.println("LENGTH " + userIdentityServerList.size());
		for (UserProfileIdentityServer userProfileIdentityServer : userIdentityServerList) {
			String abc = IDENTITY_SERVER_NAME_AUTH0;
			if (userProfileIdentityServer.getIdentityServer().equalsIgnoreCase("auth0-Username-Password-Authentication")) {
				System.out.println("FOUND " + userProfileIdentityServer.getNickName());
				authIdentityServerObj = userProfileIdentityServer;
				break;
			}
		}

		ResponseEntity responseEntity =null;
		UserMap userMap=null;
		
		// check if user has identityserver record
		if (authIdentityServerObj != null) {
			// get identity server user id
			String providerUserId = authIdentityServerObj.getUserIdentityServersId();

			String subpath = "users/" + providerUserId;

			// send request to update provider
			try {
				 responseEntity = httpService.sendProviderRestRequest(HttpMethod.GET, subpath, null,UserMap.class);
				 userMap = (UserMap) responseEntity.getBody();
			} catch (FXDefaultException e) {
				e.printStackTrace();
			}
		}
		TenantHolder.clear();
		
		return userMap!=null && userMap.getApp_metadata()!=null?userMap.getApp_metadata().getAuthorization():null;
	}

}
