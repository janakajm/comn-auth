package com.loits.comn.auth.repo;

//import com.loits.comn.auth.domain.QRoleGroupRoleHistory;
import com.loits.comn.auth.domain.RoleGroupRoleHistory;
import com.loits.comn.auth.domain.RolePermissionHistory;
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
public interface RoleGroupRoleHistoryRepository extends PagingAndSortingRepository<RoleGroupRoleHistory, Long>,
        QuerydslPredicateExecutor<RoleGroupRoleHistory>/*, QuerydslBinderCustomizer<QRoleGroupRoleHistory> */{

//    @Override
//    default public void customize(QuerydslBindings bindings, QRoleGroupRoleHistory root) {
//
//        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
//    }
}
