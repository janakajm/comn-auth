package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
import com.querydsl.core.types.dsl.StringPath;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
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
public interface RoleGroupRoleRepository extends PagingAndSortingRepository<RoleGroupRole, RoleGroupRoleId>,
        QuerydslPredicateExecutor<RoleGroupRole>/*, QuerydslBinderCustomizer<QRoleGroupRole>*/ {

    Boolean existsByRoleId(Long id);

    Boolean existsByRoleGroupId(Long id);

    Boolean existsByRoleGroupIdAndRoleId(Long roleGroupId, Long roleId);

    Optional<RoleGroupRole> findByRoleGroupIdAndRoleId(Long roleGroupId, Long roleId);

    List<RoleGroupRole> findByRoleGroupId(Long roleGroupId);
    
    @Query("FROM RoleGroupRole WHERE auth0 IS NULL")
    List<RoleGroupRole> findByAuth0();
    
    @Query("FROM RoleGroupRole WHERE auth0 IS NULL")
    List<RoleGroupRole> findByAuth0(Pageable pageable);
    
    @Modifying
    @Transactional
    @Query("UPDATE RoleGroupRole SET auth0 ='Yes' WHERE roleGroup.id =:roleGroupId")
    void updateRoleGroupRoleByGroupId(@Param("roleGroupId") Long roleGroupId);
    
    @Modifying
    @Transactional
    @Query("UPDATE RoleGroupRole SET auth0 ='Yes' WHERE roleGroup.id =:roleGroupId AND role.id NOT IN(:roleIdList)")
    void updateRoleGroupRoleByGroupIdAndRoleList(@Param("roleGroupId") Long roleGroupId,@Param("roleIdList") List<Long> roleIdList);

    @Modifying
    @Transactional
    @Query("DELETE FROM RoleGroupRole WHERE role.id =:roleId AND roleGroup.id =:roleGroupId ")
	void deleteRoleGroupRole(@Param("roleGroupId") Long roleGroupId, @Param("roleId") Long roleId);


//    @Override
//    default public void customize(QuerydslBindings bindings, QRoleGroupRole root) {
//
//        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
//    }

}
