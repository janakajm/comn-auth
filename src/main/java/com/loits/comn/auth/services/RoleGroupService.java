package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.ProviderGroup;
import com.loits.comn.auth.domain.RoleGroup;
import com.loits.comn.auth.dto.AddRoleGroup;
import com.loits.comn.auth.dto.UpdateRoleGroup;
import com.querydsl.core.types.Predicate;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleGroupService {

    Object create(String projection, AddRoleGroup addRoleGroup, String user, String tenent,boolean notInAuth0) throws FXDefaultException;

    Object delete(String projection, Long id, String user, String tenent) throws FXDefaultException;

    Page<?> getAll(Pageable pageable, String bookmarks, String projection, String search, Predicate predicate, String tenent) throws FXDefaultException;

    Object update(String projection, Long id, UpdateRoleGroup updateRoleGroup, String user, String tenent) throws FXDefaultException;

    Object getOne(String projection, String tenent, Long id) throws FXDefaultException;

    Object assign(String projection, Long roleGroupId, Long roleId, String user, String tenent,boolean notInAuth0) throws FXDefaultException;

    Object remove(String projection, Long roleGroupId, Long roleId, String user, String tenent) throws FXDefaultException;

    Page<?> getAllFromExtension(Pageable pageable, String bookmarks, String projection, String tenant) throws FXDefaultException;
    
    public List<?> getAllRoleFromAuth0(Pageable pageable, String bookmarks, String projection, String groupId, String tenent) throws FXDefaultException ;
    
    public void updateRoleGroup(Long roleGroupId, Long roleId)throws FXDefaultException;
    
    public List<String> findByRoleGroup();
    
    public List<?> getAllGroupFromAuth0(Pageable pageable, String bookmarks, String projection, String groupId, String tenent) throws FXDefaultException ;

    public List<ProviderGroup> getAllDeletedGroup()throws FXDefaultException;

	public void deleteGroup(Long groupId)throws FXDefaultException;
}
