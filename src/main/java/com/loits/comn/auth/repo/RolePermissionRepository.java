package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
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
public interface RolePermissionRepository extends PagingAndSortingRepository<RolePermission, RolePermissionId>,
        QuerydslPredicateExecutor<RolePermission>/*, QuerydslBinderCustomizer<QRolePermission>*/ {

    Boolean existsByPermissionId(Long id);

    Boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);

    List<RolePermission> findByRoleId(Long roleId);

//    @Override
//    default public void customize(QuerydslBindings bindings, QRolePermission root) {
//
//        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
//    }
    
    @Query("FROM RolePermission WHERE auth0 IS NULL")
    List<RolePermission> findForAuth0();

    boolean existsByRoleId(Long id);
    
    @Query("FROM RolePermission WHERE auth0 IS NULL")
    List<RolePermission> findForAuth0ISNull();

    @Modifying
    @Transactional
    @Query("DELETE FROM RolePermission WHERE role.id =:roleId AND permission.id =:permissionId")
	void deleteByRoleAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
