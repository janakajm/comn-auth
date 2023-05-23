package com.loits.comn.auth.services.impl;


import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.dto.*;
import com.loits.comn.auth.repo.*;
import com.loits.comn.auth.services.UserGroupService;
import com.loits.comn.auth.services.UserService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class UserGroupServiceImpl implements UserGroupService {

    Logger logger = LogManager.getLogger(UserGroupServiceImpl.class);

    @Autowired
    UserGroupRepository userGroupRepository;

    @Autowired
    UserService userService;

    @Autowired
    RoleGroupUserRepository roleGroupUserRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    RoleGroupRepository roleGroupRepository;

    @Autowired
    UserGroupRoleGroupRepository userGroupRoleGroupRepository;

    @Autowired
    UserGroupProfileRepository userGroupProfileRepository;

    @Value("${default.expire.years}")
    private int DEFAULT_EXPIRE_YEARS;

    @Override
    public Page<?> getAll(Pageable pageable, String search, String bookmarks, Predicate predicate, String projection) {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QUserGroup qUserGroup = QUserGroup.userGroup;

        //search by fileds on demand
        if (search != null && !search.isEmpty()) {
            bb.and(qUserGroup.groupName.containsIgnoreCase(search).or(qUserGroup.groupCode.containsIgnoreCase(search)));
        }

        //update pageable object to ignorecase in sorting
        Sort sort = pageable.getSort();
        sort.forEach(order -> order.ignoreCase());
        PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return userGroupRepository.findAll(bb.getValue(), pageRequest);
    }

    @Override
    public Iterable assign(String projection, List<AddUserGroupGroups> addUserGroupGroups, String user, String tenent) throws FXDefaultException {
        //Iterate through users
        List<UserGroupRoleGroup> userGroupRoleGroupList = new ArrayList<>();
        List<RoleGroupUser> roleGroupUserList = new ArrayList<>();
        
        HashMap<UserProfile, HashSet<Role>> profileRoleMap = new HashMap<>();

        //iterate over usergroups
        for (AddUserGroupGroups addUserGroupGroup : addUserGroupGroups) {

            //check if usergroup is available
            if (!userGroupRepository.existsById(addUserGroupGroup.getUserGroup())) {
                throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_USER_GROUP_ID") + " " + addUserGroupGroup.getUserGroup(), new Date(), HttpStatus.BAD_REQUEST);
            }

            UserGroup userGroup = userGroupRepository.findById(addUserGroupGroup.getUserGroup()).get();

            //iterate over rolegroups
            for (AddUserGroupGroups.AddGroup group : addUserGroupGroup.getGroups()) {
                if (!roleGroupRepository.existsById(group.getGroup())) {
                    throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID") + " " + group.getGroup(), new Date(), HttpStatus.BAD_REQUEST);
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

                UserGroupRoleGroup userGroupRoleGroup = new UserGroupRoleGroup();
                userGroupRoleGroup.setRoleGroup(roleGroup);
                userGroupRoleGroup.setUserGroup(userGroup);
                userGroupRoleGroup.setDelegatable(group.getDelegatable());
                userGroupRoleGroup.setExpires(expireTime);

                UserGroupRoleGroupId userGroupRoleGroupId = new UserGroupRoleGroupId();
                userGroupRoleGroupId.setRoleGroupId(roleGroup.getId());
                userGroupRoleGroupId.setUserGroupId(userGroup.getId());

                userGroupRoleGroup.setUserGroupRoleGroupId(userGroupRoleGroupId);

                userGroupRoleGroupList.add(userGroupRoleGroup);

                //get all user profiles in usergroup
                Iterable<UserGroupProfile> userGroupProfileList = userGroupProfileRepository.findAllByUserGroup(userGroup);
                //assign rolegroup to users
                for (UserGroupProfile userGroupProfile : userGroupProfileList) {
                    //get userprofile
                    UserProfile userProfile = userGroupProfile.getUserProfile();
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
                    roleGroupUser.setUserGroupId(addUserGroupGroup.getUserGroup());
                    // add rolegroup to list
                    roleGroupUserList.add(roleGroupUser);

                    //get rolegrouproles from rolegroup
                    List<RoleGroupRole> roleGroupRoleList = roleGroup.getRoleGroupRoles();
                    //HashSet<Role> roleSet = new HashSet<>();

                    //Assign roles to user
                    for (RoleGroupRole roleGroupRole : roleGroupRoleList) {
                        Role role = roleGroupRole.getRole();
                        HashSet<Role> roleSet = new HashSet<>();
                        List<UserRole> userRoleList = new ArrayList<>();

                        if (!userRoleRepository.existsByUserAndRoleAndRoleGroupAndUserGroupId(userProfile, role, roleGroup,addUserGroupGroup.getUserGroup())) {
                            UserRole userRole = new UserRole();
                            UserRoleId userRoleId = new UserRoleId();

                            //basic properties
                            userRole.setUserRoleId(userRoleId);
                            userRoleId.setRoleId(role.getId());
                            userRoleId.setUserId(userProfile.getId());
                            userRoleId.setRoleGroupId(roleGroup.getId());
                            userRole.setRole(role);
                            userRole.setUserGroupId(addUserGroupGroup.getUserGroup());
                            userRole.setUser(userProfile);
                            userRole.setRoleGroup(roleGroup);

                            //expiretime and delegability
                            userRole.setExpires(expireTime);
                            userRole.setDelegatable(group.getDelegatable());

                            //audit log
                            userRole.setCreatedBy(user);
                            userRole.setCreatedOn(new Timestamp(new Date().getTime()));

                            userRole.setStatus((byte) 1);

                            // add userrole to list
                            userRoleList.add(userRole);
                            roleSet.add(role);
                            
                            try {
                            	 userService.updateAuthUser(userProfile.getId(), roleSet, HttpMethod.PATCH, tenent,userRoleList);
    						} catch (Exception e) {
    							System.out.println("EXCEPTION "+e);
    						}

                        }
                    }
                    //profileRoleMap.put(userProfile, roleSet);

                }
            }

        }
        userGroupRoleGroupRepository.saveAll(userGroupRoleGroupList);
        roleGroupUserRepository.saveAll(roleGroupUserList);
        
        //updateAuth0 for all valid records
        /*for (Map.Entry<UserProfile, HashSet<Role>> entry : profileRoleMap.entrySet()) {
            //send request to update auth0 roles in user
            if (profileRoleMap.entrySet().size() !=0) {
                userService.updateAuthUser(entry.getKey() != null ? entry.getKey().getId() : null, entry.getValue(), HttpMethod.PATCH, tenent);
            }
        }
        userRoleRepository.saveAll(userRoleList);*/
        
        return addUserGroupGroups;
    }

    @Override
    public Object getByUserGroup(String projection, Long userGroupId, String user, String tenent) throws
            FXDefaultException {
        //check if usergroup is available
        if (!userGroupRepository.existsById(userGroupId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_USER_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        UserGroup userGroup = userGroupRepository.findById(userGroupId).get();

        Iterable<UserGroupRoleGroup> userGroupRoleGroupIterable = userGroupRoleGroupRepository.findByUserGroup(userGroup);

        userGroupRoleGroupIterable.forEach(userGroupRoleGroup -> userGroupRoleGroup.getRoleGroup().setRoleGroupRoles(null));

        return userGroupRoleGroupIterable;

    }

    @Override
    public Iterable<?> removeBulk(String projection, Long roleGroupId, List<RemoveGroup> removeGroupList,
                                  String user, String tenent) throws FXDefaultException {

        List<UserGroupRoleGroup> userGroupRoleGroupList = new ArrayList<>();
        List<RoleGroupUser> roleGroupUserList = new ArrayList<>();
        List<UserRole> userRoleList = new ArrayList<>();
        HashMap<UserProfile, HashSet<Role>> profileRoleMap = new HashMap<>();

        if (!roleGroupRepository.existsById(roleGroupId)) {
            throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        RoleGroup roleGroup = roleGroupRepository.findById(roleGroupId).get();
        for (RemoveGroup removeGroup : removeGroupList) {
            if (!userGroupRepository.existsById(removeGroup.getUserGroup())) {
                throw new FXDefaultException("3002", "INVALID ATTEMPT", Translator.toLocale("INVALID_USER_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
            }

            UserGroup userGroup = userGroupRepository.findById(removeGroup.getUserGroup()).get();

            if (userGroupRoleGroupRepository.existsByUserGroupAndRoleGroup(userGroup, roleGroup)) {
                UserGroupRoleGroup userGroupRoleGroup = userGroupRoleGroupRepository.findByUserGroupAndRoleGroup(userGroup, roleGroup);
                userGroupRoleGroupList.add(userGroupRoleGroup);
            }

            //get all user profiles in usergroup
            Iterable<UserGroupProfile> userGroupProfileList = userGroupProfileRepository.findAllByUserGroup(userGroup);

            for(UserGroupProfile userGroupProfile: userGroupProfileList){
                //get userprofile
                UserProfile userProfile = userGroupProfile.getUserProfile();
                //Create RoleGroup user record/ assign role-group to user
                List<RoleGroupUser> roleGroupUsers = roleGroupUserRepository.findAllByUserAndRoleGroupAndUserGroupId(userProfile, roleGroup,userGroup.getId());

                if(roleGroupUserRepository.isDuplicatebyUserGroupId(userProfile,roleGroup,userGroup.getId())) {
                    roleGroupUserList.addAll(roleGroupUsers);
                }

                List<UserRole> userRoles = userRoleRepository.findAllByUserAndRoleGroupAndUserGroupId(userProfile, roleGroup,userGroup.getId());
                userRoleList.addAll(userRoles);

                HashSet<Role> roleHashSet = new HashSet<>();
                for (UserRole userRole: userRoles){
                    if(!userRoleRepository.existsByUserAndRoleAndRoleGroupAndUserGroupIdNot(userRole.getUser(), userRole.getRole(), roleGroup,userGroup.getId())) {
                        if(!userRoleRepository.checkForDuplicateRolesByRole(userProfile,userRole.getRole())) {
                            roleHashSet.add(userRole.getRole());
                            logger.debug("ROLE HASH : "+ userProfile.getUserName() + userRole.getRole().getName());
                        }
                    }
                }
                profileRoleMap.put(userProfile, roleHashSet);
                logger.debug("userProfile"+ userProfile.getId());
                logger.debug("rolesetSize"+ roleHashSet.size());
            }

        }

        userGroupRoleGroupRepository.deleteAll(userGroupRoleGroupList);
        roleGroupUserRepository.deleteAll(roleGroupUserList);
        userRoleRepository.deleteAll(userRoleList);

        for (Map.Entry<UserProfile, HashSet<Role>> entry : profileRoleMap.entrySet()) {
            //send request to update auth0 roles in user
            if(entry.getValue().size() != 0) {
                userService.updateAuthUser(entry.getKey() != null ? entry.getKey().getId() : null, entry.getValue(), HttpMethod.DELETE, tenent);
            }
        }
        return removeGroupList;
    }
}
