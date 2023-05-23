package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.repo.custom.UserProfileIdentityServerRepositoryCustom;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

@RepositoryRestResource(exported = false)
public interface UserProfileIdentityServerRepository extends PagingAndSortingRepository<UserProfileIdentityServer, Long>,
        QuerydslPredicateExecutor<UserProfileIdentityServer>, QuerydslBinderCustomizer<QUserProfileIdentityServer>, UserProfileIdentityServerRepositoryCustom {

    void deleteAllByUserProfile(UserProfile userProfile);

    List<UserProfileIdentityServer> findAllByUserProfile_Id(Long id);
    
    List<UserProfileIdentityServer> findAllByUserProfile_UserId(String userId);

    @Override
    default public void customize(QuerydslBindings bindings, QUserProfileIdentityServer root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }

}
