package com.loits.comn.auth.repo.customImpl;

import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.repo.custom.UserProfileIdentityServerRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;

public class UserProfileIdentityServerRepositoryCustomImpl implements UserProfileIdentityServerRepositoryCustom {

    Logger logger = LogManager.getLogger(UserRoleRepositoryCustomImpl.class);

    @Autowired
    EntityManager em;


    @Override
    public JPAQuery<UserProfileIdentityServer> findByNickName(String nickName) {


        QUserProfileIdentityServer userIdentityServer = QUserProfileIdentityServer.userProfileIdentityServer;
        BooleanBuilder bb = new BooleanBuilder();
        JPAQueryFactory qf = new JPAQueryFactory(em);

        bb.and(userIdentityServer.nickName.eq(nickName));
        return qf.selectFrom(userIdentityServer).where(bb);
    }

}
