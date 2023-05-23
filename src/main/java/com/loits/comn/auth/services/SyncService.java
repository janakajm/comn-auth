package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SyncService {

    Object synchBranches(String tenent);

    Object syncUserGroups(String tenent);

    Object syncUsers(String tenent);

    Object syncUserGroupUsers(String tenent);

	Object syncUsersByProfileId(String tenent, Long profileId);
}
