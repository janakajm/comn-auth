package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.Permission;
//import com.loits.comn.auth.domain.QRole;
import com.loits.comn.auth.domain.QRole;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.RoleGroupRole;
import com.loits.comn.auth.mt.MultiTenantDataSources;
import com.querydsl.core.types.dsl.StringPath;

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
public interface RoleRepository extends PagingAndSortingRepository<Role, Long>,
        QuerydslPredicateExecutor<Role>, QuerydslBinderCustomizer<QRole> {

    @Override
    default public void customize(QuerydslBindings bindings, QRole root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }

    boolean existsByName(String name);
    
    @Query("select r from Role r where lower(r.name)=lower(:name)")
    Optional<Role> findByNameContainingIgnoreCase(@Param("name") String name);

    void deleteByName(String s);

    Optional<Role> findByName(String name);

    List<Role> findByCreatedBy(String username);
    
    @Query("FROM Role WHERE auth0 is null")
    Iterable<Role> findByAuth0NotIn(String auth0);
    
    @Query("FROM Role WHERE (lower(name) LIKE 'casa%' OR lower(name) LIKE 'common%')")
    List<Role> findAllRole();
    
    @Query("SELECT name,description,search,createdBy,id FROM Role")
    List<Object[]> findByAllRole();
    
    @Query("SELECT prol.providerRoleId FROM Role rol,RoleGroupRole rog,ProviderRole prol WHERE rol.id=rog.role.id AND rol.id=prol.roleId AND rog.roleGroup.id=:roleGroupId AND rog.auth0 is null")
    List<String> findByRoleGroupId(@Param("roleGroupId") Long roleGroupId);
    
    @Query("SELECT rol FROM Role rol,ProviderRole prol WHERE rol.id=prol.roleId AND prol.providerRoleId=:providerRolId")
    Optional<Role> findByProviderRoleId(@Param("providerRolId") String providerRolId);

    @Query("FROM Role WHERE deleted='Yes'")
	Iterable<Role> findByDeleted();
}
