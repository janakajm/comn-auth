package com.loits.comn.auth.repo.customImpl;

import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.repo.custom.RoleGroupUserRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;

public class RoleGroupUserRepositoryCustomImpl implements RoleGroupUserRepositoryCustom {

    Logger logger = LogManager.getLogger(RoleGroupUserRepositoryCustomImpl.class);

    @Autowired
    EntityManager em;


    @Override
    public boolean isDuplicate(UserProfile user, RoleGroup roleGroup) {

        QRoleGroupUser roleGroupUser = QRoleGroupUser.roleGroupUser;
        BooleanBuilder bb = new BooleanBuilder();
        JPAQueryFactory qf = new JPAQueryFactory(em);

        // build the JPA Query
        bb.and(roleGroupUser.user.eq(user).and(roleGroupUser.roleGroup.eq(roleGroup)))
                .and(roleGroupUser.userGroupId.isNull());
       return qf.selectFrom(roleGroupUser).where(bb).fetchCount() > 0;
    }

    @Override
    public RoleGroupUser getByRoleGroupIdAndUserId(Long roleGroupId, Long userId) {
        QRoleGroupUser roleGroupUser = QRoleGroupUser.roleGroupUser;
        BooleanBuilder bb = new BooleanBuilder();
        JPAQueryFactory qf = new JPAQueryFactory(em);

        // build the JPA Query
        bb.and(roleGroupUser.roleGroupUserId.userId.eq(userId).and(roleGroupUser.roleGroupUserId.roleGroupId.eq(roleGroupId)))
                .and(roleGroupUser.userGroupId.isNull());
        return qf.selectFrom(roleGroupUser).where(bb).fetchOne();
    }

    @Override
    public JPAQuery<RoleGroupUser> getDelegatableRoles(UserProfile user) {
        QRoleGroupUser roleGroupUser = QRoleGroupUser.roleGroupUser;
        BooleanBuilder bb = new BooleanBuilder();
        JPAQueryFactory qf = new JPAQueryFactory(em);

        // build the JPA Query
        bb.and(roleGroupUser.user.eq(user).and(roleGroupUser.delegatable.eq((byte) 1)));

        return qf.selectFrom(roleGroupUser).where(bb);
    }

    @Override
    public boolean isDuplicatebyUserGroupId(UserProfile user, RoleGroup roleGroup,Long userGroupId) {

        QRoleGroupUser roleGroupUser = QRoleGroupUser.roleGroupUser;
        BooleanBuilder bb = new BooleanBuilder();
        JPAQueryFactory qf = new JPAQueryFactory(em);

        // build the JPA Query
        bb.and(roleGroupUser.user.eq(user).and(roleGroupUser.roleGroup.eq(roleGroup)))
                .and(roleGroupUser.userGroupId.eq(userGroupId));
        return qf.selectFrom(roleGroupUser).where(bb).fetchCount() > 0;
    }

}
