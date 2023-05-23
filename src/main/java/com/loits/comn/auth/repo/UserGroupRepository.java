package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.Branch;
import com.loits.comn.auth.domain.QBranch;
import com.loits.comn.auth.domain.QUserGroup;
import com.loits.comn.auth.domain.UserGroup;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

@RepositoryRestResource(exported = false)
public interface UserGroupRepository extends PagingAndSortingRepository<UserGroup, Long>,
        QuerydslPredicateExecutor<UserGroup>, QuerydslBinderCustomizer<QUserGroup>{

    @Override
    default public void customize(QuerydslBindings bindings, QUserGroup root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }

}
