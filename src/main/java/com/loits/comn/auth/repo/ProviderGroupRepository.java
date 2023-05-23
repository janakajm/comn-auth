package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
import com.querydsl.core.types.dsl.StringPath;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

@RepositoryRestResource(exported = false)
public interface ProviderGroupRepository extends PagingAndSortingRepository<ProviderGroup, ProviderGroupIdClass>,
        QuerydslPredicateExecutor<ProviderGroup>, QuerydslBinderCustomizer<QProviderGroup> {

    Boolean existsByProviderGroupIdClass_Group(Long id);

    Optional<ProviderGroup> findByProviderGroupIdClass_Group(Long id);
    
    @Override
    default public void customize(QuerydslBindings bindings, QProviderGroup root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }
    
    @Query("SELECT pp.providerGroupIdClass.group FROM ProviderGroup pp where pp.providerGroupId=:providerId")
    Long findByProviderGroupAndProviderGroupIdLong(@Param("providerId") String providerId);
}
