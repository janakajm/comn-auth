package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface HistoryService {

    @Async
    CompletableFuture<?> saveBulkHistoryRecord(List<Permission> permissionList, String recordType);

    @Async
    CompletableFuture<?> saveHistoryRecord(Permission permission, String recordType);

    @Async
    CompletableFuture<?> saveHistoryRecord(Role role, String recordType);

    @Async
    CompletableFuture<?> saveHistoryRecord(RoleGroup roleGroup, String recordType);

    @Async
    CompletableFuture<?> saveHistoryRecord(RolePermission rolePermission, String recordType);
    
    void savePermissionHistoryRecord(Permission permission, String recordType,String tenent);
   
    @Async
    CompletableFuture<?> saveHistoryRecord(RoleGroupRole roleGroupRole, String recordType);
	
	void saveRolePermissionHistory(RolePermission rolePermission, String recordType);

	void saveRolePermissionHistoryRecord(RolePermission rolePermission, String recordType);

}
