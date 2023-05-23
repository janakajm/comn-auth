package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.repo.custom.RoleGroupUserRepositoryCustom;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

@RepositoryRestResource(exported = false)
public interface RoleGroupUserRepository extends PagingAndSortingRepository<RoleGroupUser, RoleGroupUserId>,
        QuerydslPredicateExecutor<RoleGroupUser>, RoleGroupUserRepositoryCustom/*, QuerydslBinderCustomizer<QRoleGroupRole> */ {

    Optional<RoleGroupUser> findByRoleGroupIdAndUserId(Long roleGroupId, Long userId);

    Page<RoleGroupUser> findAllByDelegatableAndUser(Byte d, UserProfile user, Pageable pageable);

    List<RoleGroupUser> findAllByUserAndRoleGroupAndUserGroupId(UserProfile user, RoleGroup roleGroup, Long userGroupId);

    List<RoleGroupUser> findByRoleGroup(RoleGroup roleGroup);

	List<RoleGroupUser> findByUserId(Long userId);
	
	 @Query("SELECT roleGroup.id FROM RoleGroupUser GROUP BY roleGroup.id HAVING COUNT(user.id)>0")
	 List<Long> findRoleGroupUser();
	 
	 @Query("SELECT roleGroup.id FROM RoleGroupUser WHERE user.id=:userId GROUP BY roleGroup.id HAVING COUNT(user.id)>0")
	 List<Long> findRoleGroupByUserId(@Param("userId") Long userId);


//    @Override
//    default public void customize(QuerydslBindings bindings, QRoleGroupRole root) {
//
//        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
//    }

}
