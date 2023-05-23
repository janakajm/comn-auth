package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.QPermission;
import com.querydsl.core.types.dsl.StringPath;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

@RepositoryRestResource(exported = false)
public interface PermissionRepository extends PagingAndSortingRepository<Permission, Long>,QuerydslPredicateExecutor<Permission>, QuerydslBinderCustomizer<QPermission>,JpaRepository<Permission, Long>{

    boolean existsByName(String name);
    
    @Query("select p from Permission p where lower(p.name)=lower(:name)")
    Optional<Permission> findByNameContainingIgnoreCase(@Param("name") String name);

    @Override
    default public void customize(QuerydslBindings bindings, QPermission root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }

    Permission findByName(String name);
    
    Optional<Permission> findById(Long id);
    
    @Query("FROM Permission WHERE auth0 is null")
    Iterable<Permission> findByAuth0NotIn(String auth0);
    
    @Query("FROM Permission WHERE deleted='Yes'")
    Iterable<Permission> findByDeleted();
}
