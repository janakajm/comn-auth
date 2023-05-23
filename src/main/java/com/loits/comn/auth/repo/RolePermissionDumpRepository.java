package com.loits.comn.auth.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.loits.comn.auth.domain.RolePermissionDump;

@RepositoryRestResource(exported = false)
public interface RolePermissionDumpRepository extends PagingAndSortingRepository<RolePermissionDump, Long>,
QuerydslPredicateExecutor<RolePermissionDump>,JpaRepository<RolePermissionDump, Long>{

	@Query("SELECT ppd FROM RolePermissionDump ppd WHERE ppd.roleId=:roleId AND ppd.permissionId=:permissionId")
	Optional<RolePermissionDump> existsByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

}
