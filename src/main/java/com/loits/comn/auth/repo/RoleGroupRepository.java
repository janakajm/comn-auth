package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.ProviderGroup;
import com.loits.comn.auth.domain.QRoleGroup;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.RoleGroup;
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
public interface RoleGroupRepository extends PagingAndSortingRepository<RoleGroup, Long>,
        QuerydslPredicateExecutor<RoleGroup>, QuerydslBinderCustomizer<QRoleGroup> {

    boolean existsByName(String name);

    Optional<RoleGroup> findByName(String name);
    
    @Query("select g from RoleGroup g where lower(g.name)=lower(:name)")
    Optional<Permission> findByNameContainingIgnoreCase(@Param("name") String name);

    List<RoleGroup> findByCreatedBy(String username);
    
    @Query("SELECT rg FROM RoleGroup rg,ProviderGroup pg WHERE rg.id=pg.providerGroupIdClass.group AND pg.providerGroupId=:providerId")
    Optional<RoleGroup> findByProviderGroupId(@Param("providerId") String providerId);

    @Override
    default public void customize(QuerydslBindings bindings, QRoleGroup root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }
    
    @Query("SELECT pg.providerGroupId FROM RoleGroup rg,ProviderGroup pg WHERE rg.id=pg.providerGroupIdClass.group")
    List<String> findByRoleGroup();

    @Query("SELECT pg FROM RoleGroup rg,ProviderGroup pg WHERE rg.id=pg.providerGroupIdClass.group AND rg.deleted='Yes'")
    List<ProviderGroup> findByDeleted();
}
