package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.Module;
import com.loits.comn.auth.domain.QModule;
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
public interface ModuleRepository extends PagingAndSortingRepository<Module, String>,
        QuerydslPredicateExecutor<Module>, QuerydslBinderCustomizer<QModule>{
    boolean existsByCode(String code);

    @Override
    default public void customize(QuerydslBindings bindings, QModule root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));

    }
}
