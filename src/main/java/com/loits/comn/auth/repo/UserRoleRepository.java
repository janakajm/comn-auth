package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.repo.custom.UserRoleRepositoryCustom;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

@RepositoryRestResource(exported = false)
public interface UserRoleRepository extends PagingAndSortingRepository<UserRole, Long>,
        QuerydslPredicateExecutor<UserRole>, QuerydslBinderCustomizer<QUserRole>, UserRoleRepositoryCustom {

    boolean existsByUserAndRole(UserProfile user, Role role);

    boolean existsByUserAndRoleAndRoleGroup(UserProfile user, Role role, RoleGroup roleGroup);

    boolean existsByUserAndRoleAndRoleGroupAndUserGroupId(UserProfile user, Role role, RoleGroup roleGroup, long userGr);

    //boolean existsByUserAndRoleAndRoleGroupAndUserGroupIdIsNull(UserProfile user, Role role, RoleGroup roleGroup);

    Optional<UserRole> findByUserAndRole(UserProfile user, Role role);

    Optional<UserRole> findByUserAndRoleAndRoleGroup(UserProfile user, Role role, RoleGroup roleGroup);

    //Optional<UserRole> findByUserAndRoleAndRoleGroupAndUserGroupIdIsNull(UserProfile user, Role role, RoleGroup roleGroup);

    boolean existsByRole(Role role);

    List<UserRole> findAllByUser(UserProfile userProfile);

    List<UserRole> findAllByUserAndRoleGroupAndUserGroupId(UserProfile userProfile,RoleGroup roleGroup,long groupId);

    boolean existsByUserAndRoleAndRoleGroupNot(UserProfile user, Role role, RoleGroup roleGroup);

    //boolean existsByUserAndRoleAndRoleGroupAndUserGroupIdIsNullNot(UserProfile user, Role role, RoleGroup roleGroup);

    boolean existsByUserAndRoleAndRoleGroupAndUserGroupIdNot(UserProfile user, Role role, RoleGroup roleGroup,long userGroupId);



    @Override
    default public void customize(QuerydslBindings bindings, QUserRole root) {

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }
}
