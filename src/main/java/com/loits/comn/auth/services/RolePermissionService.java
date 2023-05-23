package com.loits.comn.auth.services;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.RolePermission;

@Service
public interface RolePermissionService {

    Object delete(String projection, String userId, String roleId, String user, String tenent);

    Object update(String projection, String userId, String roleId, String user, String tenent);
    
    List<RolePermission> findForAuth0ISNull();
    
    public List<?> getAllPermissionFromAuth0(Pageable pageable, String bookmarks,String roleId, String tenent) throws FXDefaultException ;
    
}
