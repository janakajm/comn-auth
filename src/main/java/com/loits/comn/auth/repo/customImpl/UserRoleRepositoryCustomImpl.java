package com.loits.comn.auth.repo.customImpl;

import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.repo.custom.UserRoleRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;

@Repository
public class UserRoleRepositoryCustomImpl implements UserRoleRepositoryCustom {

  Logger logger = LogManager.getLogger(UserRoleRepositoryCustomImpl.class);

  @Autowired
  EntityManager em;

  @Override
  public boolean isDuplicate(
          UserProfile user, Role role, RoleGroup roleGroup) {

    QUserRole userRole = QUserRole.userRole;
    BooleanBuilder bb = new BooleanBuilder();
    JPAQueryFactory qf = new JPAQueryFactory(em);

    // build the JPA Query
    bb.and(userRole.user.eq(user).and(userRole.role.eq(role).and(userRole.roleGroup.eq(roleGroup))))
              .and(userRole.userGroupId.isNull());
    return qf.selectFrom(userRole).where(bb).fetchCount() > 0;
  }

  @Override
  public JPAQuery<UserRole> getUniqueUserRole(UserProfile user, Role role, RoleGroup roleGroup) {
    QUserRole userRole = QUserRole.userRole;
    BooleanBuilder bb = new BooleanBuilder();
    JPAQueryFactory qf = new JPAQueryFactory(em);

    bb.and(userRole.user.eq(user).and(userRole.role.eq(role).and(userRole.roleGroup.eq(roleGroup))))
            .and(userRole.userGroupId.isNull());
    return qf.selectFrom(userRole).where(bb);
  }

  @Override
  public boolean isDuplicateNot(UserProfile user, Role role, RoleGroup roleGroup) {

    QUserRole userRole = QUserRole.userRole;
    BooleanBuilder bb = new BooleanBuilder();
    JPAQueryFactory qf = new JPAQueryFactory(em);

    // build the JPA Query
    bb.and(userRole.user.eq(user).and(userRole.role.eq(role).and(userRole.roleGroup.eq(roleGroup))))
            .and(userRole.userGroupId.isNull());
    return !(qf.selectFrom(userRole).where(bb).fetchCount() > 0);
  }

  @Override
  public boolean checkForDuplicatesByRoleGroups(UserProfile user, RoleGroup roleGroup) {

    QUserRole userRole = QUserRole.userRole;
    BooleanBuilder bb = new BooleanBuilder();
    JPAQueryFactory qf = new JPAQueryFactory(em);

    // build the JPA Query
    bb.and(userRole.user.eq(user).and(userRole.roleGroup.eq(roleGroup)));

    return qf.selectFrom(userRole).where(bb).fetchCount() > 1;
  }

  @Override
  public boolean checkForDuplicateRolesByRole(UserProfile user, Role role) {
    QUserRole userRole = QUserRole.userRole;
    BooleanBuilder bb = new BooleanBuilder();
    JPAQueryFactory qf = new JPAQueryFactory(em);

    // build the JPA Query
    bb.and(userRole.user.eq(user).and(userRole.role.eq(role)));

    return qf.selectFrom(userRole).where(bb).fetchCount() > 1;
  }

  public JPAQuery<UserRole> getRolesByKey(UserProfile user, String key) {

    QUserRole userRole = QUserRole.userRole;
    BooleanBuilder bb = new BooleanBuilder();
    JPAQueryFactory qf = new JPAQueryFactory(em);

    // build the JPA Query
    bb.and(userRole.user.eq(user).and(userRole.role.name.startsWith(key)));
    return qf.selectFrom(userRole).where(bb);
  }

  @Override
  public JPAQuery<UserRole> getDelegatableRoles(UserProfile user) {
    QUserRole userRole = QUserRole.userRole;
    BooleanBuilder bb = new BooleanBuilder();
    JPAQueryFactory qf = new JPAQueryFactory(em);

    // build the JPA Query
    bb.and(userRole.user.eq(user).and(userRole.delegatable.eq((byte) 1)));

    return qf.selectFrom(userRole).where(bb);
  }

}
