package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
import com.querydsl.core.types.dsl.StringPath;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

import javax.transaction.Transactional;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

@RepositoryRestResource(exported = false)
public interface ProviderPermissionRepository extends PagingAndSortingRepository<ProviderPermission, ProviderPermissionIdClass>,
        QuerydslPredicateExecutor<ProviderPermission> , QuerydslBinderCustomizer<QProviderPermission>{

    @Override
    default public void customize(QuerydslBindings bindings, QProviderPermission root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }

    Optional<ProviderPermission> findByProviderPermissionIdClass_Permission(Long permissionId);

    Boolean existsByProviderPermissionIdClass_Permission(Long permissionId);

    boolean existsByProviderPermissionId(String id);

    Optional<ProviderPermission> findByProviderPermissionId(String id);
    
    @Query("SELECT pp FROM ProviderPermission pp where pp.providerPermissionId=:providerId")
    Optional<ProviderPermission> findByProviderPermissionProviderPermissionId(@Param("providerId") String providerId);
    
    @Query("SELECT pp.providerPermissionIdClass.permission FROM ProviderPermission pp where pp.providerPermissionId=:providerId")
    Long findByProviderPermissionProviderPermissionIdLong(@Param("providerId") String providerId);
    
    @Modifying
    @Transactional
    @Query("UPDATE ProviderPermission SET providerPermissionId =:correctId WHERE providerPermissionId =:wrongId")
    void updatePermissionByuid(@Param("correctId") String correctId,@Param("wrongId") String wrongId);
    
    @Query("SELECT p.name FROM Permission p WHERE p.id IN (SELECT pp.providerPermissionIdClass.permission FROM ProviderPermission pp where pp.providerPermissionId=:providerId)")
    String findByPermissionNameByProviderId(@Param("providerId") String providerId);

}
