package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.AsyncTask;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.RolePermission;
import com.loits.comn.auth.dto.AddRole;
import com.loits.comn.auth.dto.UpdateRole;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RoleService {

    Page<?> getAll(Pageable pageable, String s, String bookmarks, Predicate predicate, String projection);

    Page<?> getAllFromExtension(Pageable pageable, String bookmarks, String projection, String id) throws FXDefaultException;

    Object getOne(String projection, String tenent, Long id) throws FXDefaultException;

    Object delete(String projection, Long id, String user, String tenent, boolean tenantUpdate) throws FXDefaultException;

    Object create(String projection, AddRole addRole, String user, String tenent, boolean tenantUpdate, boolean authUpdate, boolean notInAuth0) throws FXDefaultException;

    Object bulkCreate(String projection, List<AddRole> addRoleList, String user, String tenent, boolean tenantUpdate, boolean authUpdate,boolean notInAuth0);

    Object update(String projection, Long id, UpdateRole updateRole, String user, String tenent, boolean tenantUpdate) throws FXDefaultException;

    Object assign(String projection, Long roleId, Long permissionId, String user, String tenent, boolean tenantUpdate,boolean notInAuth0) throws FXDefaultException;
    
    Object assignAsync(String projection, Long roleId, Long permissionId, String user, String tenent, boolean tenantUpdate,boolean notInAuth0) throws FXDefaultException;

    Object remove(String projection, Long roleId, Long permissionId, String user, String tenent, boolean tenantUpdate) throws FXDefaultException;

    @Async
    CompletableFuture<?> asyncUpdateOperations(Role role, Permission permission, HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate, String operationType, boolean authUpdate);

    @Async
    CompletableFuture<?> updateAuth0(Role role, HttpMethod httpMethod, String subpath, String tenant, AsyncTask at);

    /* added by Pasindu */
   // public List<String> getAllwithoutPageSize(Pageable pageable, String bookmarks, String search, Predicate predicate, String projection, String tenent) throws FXDefaultException;  
    
    public Iterable<Role> getAllwithoutPageSize(Pageable pageable, String bookmarks, String search, Predicate predicate, String projection, String tenent);
    
    /** added by Pasindu */
    public List<?> getAllRoleFromAuth0(Pageable pageable, String bookmarks, String projection, String permissionId, String tenent) throws FXDefaultException ;
    
    public void updateRole(Role role);
    
    public void  updateRoleByName(String roleName) throws FXDefaultException;
    
    public Iterable<RolePermission> getPermissionByRoleId(Long roleId,String tenent);
    
    public String findByProviderPermissionId(Long permissionId);
    
    List<?> assignAuto(String projection, String user, String tenent, boolean tenantUpdate,boolean notInAuth0) throws FXDefaultException;
    
    Object createAutoRole(String user, String tenent, boolean tenantUpdate, boolean authUpdate, boolean notInAuth0) throws FXDefaultException;
    
    public void updateRolePermission(Long roleId,Long permissionId);
    
    public Role getRoleByName(String name)throws FXDefaultException;
    
    public void updateRoleByuid(String correctId,String wrongId)throws FXDefaultException;
    
    public void addProviderRecord(String providerId,Role role)throws FXDefaultException;

	public void saveAuth0Mapping(String tenent,String user);

	public void deleteAuth0Mapping(String tenent, String user);

	public void deleteRoleByName(String roleName) throws FXDefaultException;

	public Iterable<Role> getAllDeletedRole()throws FXDefaultException;

	public void deleteRole(Role role)throws FXDefaultException;

	Iterable<?> getAllForExport(String bookmarks, Predicate predicate, String projection, String tenent);

	public void rolePermissionMapping(String userName,String tenent,String processCode);

	public void syncRole(String userName, String tenent, String processCode);

	public void updateRolesAuth0Field(Long id, String username, String tenent)throws Exception;

	public void updateRolesPermissionAuth0Field(Long roleId, Long permissionId, String username, String tenent);

	void missingProviderRole(String userName, String tenent, String processCode) throws FXDefaultException;
 

}
