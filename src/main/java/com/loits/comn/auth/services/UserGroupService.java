package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.dto.AddUserGroupGroups;
import com.loits.comn.auth.dto.RemoveGroup;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserGroupService {

    Page<?> getAll(Pageable pageable, String search, String bookmarks, Predicate predicate, String projection);

    Iterable<?> assign(String projection, List<AddUserGroupGroups> addUserGroupGroups, String user, String tenent) throws FXDefaultException;

    Object getByUserGroup(String projection, Long userGroupId, String user, String tenent) throws FXDefaultException;

    Iterable<?> removeBulk(String projection, Long roleGroupId, List<RemoveGroup> removeGroupList, String user, String tenent) throws FXDefaultException;
}
