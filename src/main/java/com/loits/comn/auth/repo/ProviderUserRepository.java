package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

@RepositoryRestResource(exported = false)
public interface ProviderUserRepository extends PagingAndSortingRepository<ProviderUser, ProviderUserIdClass>,
        QuerydslPredicateExecutor<ProviderUser> , QuerydslBinderCustomizer<QProviderUser> {

    Optional<ProviderUser> findByProviderUserIdClass_UserAndProviderUserIdClass_Provider(Long userId, String provider);

    Boolean existsByProviderUserIdClass_UserAndProviderUserIdClass_Provider(Long userId, String provider);

    @Override
    default public void customize(QuerydslBindings bindings, QProviderUser root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }
}
