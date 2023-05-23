package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserProfileService {

    Page<?> getAll(Pageable pageable, String search, String bookmarks, Predicate predicate, String projection);

    Object getOne(String tenent, Long id, String projection) throws FXDefaultException;
}
