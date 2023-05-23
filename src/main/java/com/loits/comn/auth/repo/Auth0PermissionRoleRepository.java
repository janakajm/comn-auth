package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.Auth0PermissionRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource(exported = false)
public interface Auth0PermissionRoleRepository extends PagingAndSortingRepository<Auth0PermissionRole, Long>,QuerydslPredicateExecutor<Auth0PermissionRole>,JpaRepository<Auth0PermissionRole, Long>{

}
