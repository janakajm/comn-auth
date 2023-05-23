package com.loits.comn.auth.services;

import com.loits.comn.auth.domain.Branch;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BranchService {

    Page<?> getAll(Pageable pageable, String bookmarks, Predicate predicate, String projection, String search);

    Object deleteBranchesInBulk(List<Long> branchIdList);

    Object deleteBranch(Long id);

}
