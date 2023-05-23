package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface PermissionGroupsRepository extends PagingAndSortingRepository<PermissionGroups, PermissionGroupId>,
        QuerydslPredicateExecutor<PermissionGroups> {

	List<PermissionGroups> findByPermission_id(Long id);
	
	@Modifying
    @Transactional
    @Query("DELETE FROM PermissionGroups WHERE permission.id =:permissionId")
	void deletePermissionGroups(@Param("permissionId") Long permissionId);

	@Query(value = "SELECT permission_id FROM permission_group WHERE group_id IN(:groupIdList)",nativeQuery = true)
	List<Object> findPermissionByGroupIdList(@Param("groupIdList") List<Long> groupIdList);

	@Query("FROM PermissionGroups WHERE roleGroup.id IN(:groupIdList) AND (upper(permission.name) LIKE '%' || upper(:searchq) || '%')")
	List<PermissionGroups> findByRoleGroup_IdIn(@Param("groupIdList") List<Long> groupIdList,@Param("searchq") String searchq);
	

}
