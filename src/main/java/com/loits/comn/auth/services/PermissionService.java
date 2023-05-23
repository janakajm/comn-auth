package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.AsyncTask;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.dto.AddPermission;
import com.loits.comn.auth.dto.UpdatePermission;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface PermissionService {

    Page<?> getAll(Pageable pageable, String bookmarks, String search, Predicate predicate, String projection, String tenent) throws FXDefaultException;

    Page<?> getAllFromExtension(Pageable pageable, String bookmarks, String projection, String permissionId, String tenent) throws FXDefaultException;

    Object getOne(String projection, String tenent, Long id) throws FXDefaultException;

    Object create(String projection, AddPermission addPermission, String user, String tenant, boolean tenantUpdate,boolean notExistInAuth0) throws FXDefaultException;
 
    Object update(String projection, Long id, UpdatePermission updatePermission, String user, String tenent, boolean tenantUpdate) throws FXDefaultException;

    Object delete(String projection, Long id, String user, String tenent, boolean tenantUpdate) throws FXDefaultException;

    Iterable<?> getAllForExport(String bookmarks, Predicate predicate, String projection, String tenent);

    ResponseEntity updateAuthAPI(String projection, String user, String tenent) throws FXDefaultException;

    Object bulkCreate(String projection, List<AddPermission> addPermissionList, String user, String tenent, boolean tenantUpdate, boolean authUpdate,boolean notExistInAuth0);

    @Async
    CompletableFuture<?> updateAuth0(Permission permission, HttpMethod httpMethod, String subpath, String tenant, AsyncTask at);

    CompletableFuture<?> asyncUpdateOperations(Permission permission, HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate, boolean authUpdate);

    /*amal*/
    Object updateAuth0(Permission permission, HttpMethod httpMethod, String subpath, String tenant) throws Exception;
    void updateOperations(Permission permission,String user, HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate, boolean authUpdate) throws Exception;
   
    /* added by Pasindu */
    //public List<String> getAllwithoutPageSize() throws FXDefaultException;  
    
    public Iterable<Permission> getAllwithoutPageSize();
    
    public Iterable<Permission> getAllDeletedPermission() throws FXDefaultException;
    
    /** added by Pasindu */
    public List<?> getAllPermissionFromAuth0(Pageable pageable, String bookmarks, String projection, String permissionId, String tenent) throws FXDefaultException ;
    
    public List<?> getAllPermissionById(Long id) throws FXDefaultException ;
    
    public void updatePermission(Permission permission)throws Exception;
    
    public String getApplicationIdByTenant(String tenent) throws Exception;
    
    public void updatePermissionByName(String name) throws Exception;
    
    public void deletePermissionByName(String name) throws Exception;
    
    public void deletePermission(Permission permission) throws Exception;

	Object create(String projection, Permission permission, String user, String tenent, boolean tenantUpdate,boolean notExistInAuth0) throws FXDefaultException;
	
	Object createPermission(String projection, Permission permission, String user, String tenent, boolean tenantUpdate,boolean notExistInAuth0) throws FXDefaultException;

	public Permission getPermissionByName(String name)throws Exception;

	public void updatePermissionByuid(String correctId, String wrongId)throws Exception;
	
	public void addProviderRecord(String providerId,Permission permission)throws Exception;

	public Future<List<AddPermission>> syncPermission();

	public void syncPermission(String userName, String tenent, String processCode);

	public void updatePermissionsAuth0Field(Long id, String username, String tenent)throws Exception;

	void missingProviderPermission(String userName, String tenent, String processCode)throws FXDefaultException;
}
