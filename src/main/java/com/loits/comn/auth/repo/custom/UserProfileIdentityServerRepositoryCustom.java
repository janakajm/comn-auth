package com.loits.comn.auth.repo.custom;

import com.loits.comn.auth.domain.*;
import com.querydsl.jpa.impl.JPAQuery;

public interface UserProfileIdentityServerRepositoryCustom {

    JPAQuery<UserProfileIdentityServer> findByNickName(String nickName);


}
