package com.loits.comn.auth.repo.custom;

import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.PermissionGroups;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author Minoli De Silva - Infinitum360
 * @version 1.0.0
 */

public interface UserPermissionRepositoryCustom extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

    @Query("select p FROM Permission p join UserProfile up join up.userRoleGroups ug join PermissionGroups pg where p.id = pg.permission.id AND " +
            "pg.roleGroup.id = ug.id and up.userId = : userId")
    List<Permission> findPermissionByUserId(@Param("userId") String userId);
}
