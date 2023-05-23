package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.StringPath;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

@RepositoryRestResource(exported = false)
public interface ProviderRoleRepository extends PagingAndSortingRepository<ProviderRole, ProviderRoleIdClass>,
        QuerydslPredicateExecutor<ProviderRole> , QuerydslBinderCustomizer<QProviderRole> {

    Boolean existsByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(Long id, String provider);

    Optional<ProviderRole> findByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(Long id, String provider);

    Boolean existsByProviderRoleIdClass_Role(Long roleId);
    
    Boolean existsByProviderRoleId(String id);

    Optional<ProviderRole> findByProviderRoleId(String id);

    Boolean existsByRoleIdAndProvider(Long roleId, String provider);

    Optional<ProviderRole> findFirstByRoleIdAndProvider(Long role, String provider);

    Optional<ProviderRole> deleteByRoleIdAndProvider(Long role, String provider);

    @Override
    default public void customize(QuerydslBindings bindings, QProviderRole root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }

    Optional<ProviderRole> findByProviderRoleIdClass_Role(Long id);
    
    Optional<ProviderRole> findByProviderRoleIdAndProvider(String providerId, String provider);
    
    @Query("SELECT pr FROM ProviderRole pr WHERE pr.providerRoleId=:providerId")
    Optional<ProviderRole> findByProviderRoleProviderRoleId(@Param("providerId") String providerId);
    
    @Query("SELECT pr.roleId FROM ProviderRole pr WHERE pr.providerRoleId=:providerId")
    Long findByProviderRoleProviderRoleIdLong(@Param("providerId") String providerId);

	Optional<ProviderRole> findByRoleId(Long roleId);
	
    @Modifying
    @Transactional
    @Query("UPDATE ProviderRole SET providerRoleId =:correctId WHERE providerRoleId =:wrongId")
    void updateRoleByuid(@Param("correctId") String correctId,@Param("wrongId") String wrongId);
}
