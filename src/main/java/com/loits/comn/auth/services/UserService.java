package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.UserRole;
import com.loits.comn.auth.dto.AddUserGroups;
import com.loits.comn.auth.dto.AppMetaData;
import com.loits.comn.auth.dto.Authorization;
import com.loits.comn.auth.dto.BranchAssignment;
import com.loits.comn.auth.dto.RemoveUser;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface UserService {
    Page<?> getAll(Pageable pageable, String bookmarks, Predicate predicate, String projection, String search);

    Page<?> getAllDelegatableRoles(Pageable pageable, String bookmarks, String projection, String search, String user, Predicate predicate, String tenent) throws FXDefaultException;

    Iterable<?> assign(String projection, List<AddUserGroups> addUserGroupsList, String user, String tenent) throws FXDefaultException;

    Iterable<?> removeBulk(String projection, Long roleGroupId, List<RemoveUser> removeUserList, String user, String tenent) throws FXDefaultException;

    Iterable<?> update(String projection, List<AddUserGroups> addUserGroupsList, String user, String tenent) throws FXDefaultException;

    Object removeExpired(String projection, String user, String tenent);

    Object getLoggedInUser(String bookmarks, String user, Pageable pageable, String projection) throws FXDefaultException;

    //@Async
    void updateAuthUser(Long userId, HashSet<Role> roleHashSet, HttpMethod httpMethod, String tenant);

    Object assignBranch(String projection, Long id, Long branchId, BranchAssignment branchAssignment, String user, String tenent) throws FXDefaultException;

    Object updateBranch(String projection, Long id, Long branchId, BranchAssignment branchAssignment, String user, String tenent) throws FXDefaultException;

    Object getBranches(String projection, Long id, String user, String tenent) throws FXDefaultException;

    Object getRoles(String projection, String id,String searchKey, String user, String tenent) throws FXDefaultException;

    Object getDelegatableRoles(String projection, String id,String searchKey, String user, String tenent) throws FXDefaultException;
    
    void updateAuthUser(Long userId, HashSet<Role> roleHashSet, HttpMethod httpMethod, String tenant,List<UserRole> userRoleList);

    Authorization getAllUserDetails(Pageable pageable, String projection, String userId, Predicate predicate, String tenent)throws FXDefaultException;

}
