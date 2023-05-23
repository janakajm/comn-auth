package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
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
public interface UserGroupProfileRepository extends PagingAndSortingRepository<UserGroupProfile, Long>,
        QuerydslPredicateExecutor<UserGroupProfile>,QuerydslBinderCustomizer<QUserGroupProfile>{

    void deleteAllByUserGroup(UserGroup userGroup);

    Iterable<UserGroupProfile> findAllByUserGroup(UserGroup userGroup);

    boolean existsByUserGroupAndUserProfile(UserGroup userGroup, UserProfile userProfile);

    @Override
    default public void customize(QuerydslBindings bindings, QUserGroupProfile root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }

}
