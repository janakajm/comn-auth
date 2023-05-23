package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    Page<?> getAll(Pageable pageable, String bookmarks, Predicate predicate, String projection, String search);

    Object getOne(Long id, String projection) throws FXDefaultException;
}
