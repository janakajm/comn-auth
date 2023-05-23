package com.loits.comn.auth.services.impl;


import com.loits.comn.auth.domain.QBranch;
import com.loits.comn.auth.repo.BranchRepository;
import com.loits.comn.auth.services.BranchService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchServiceImpl implements BranchService {

    Logger logger = LogManager.getLogger(BranchServiceImpl.class);

    @Autowired
    BranchRepository branchRepository;

    @Override
    public Page<?> getAll(Pageable pageable, String bookmarks, Predicate predicate, String projection, String search) {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QBranch qBranch = QBranch.branch;

        //search by name on demand
        if(search!=null && !search.isEmpty()){
            bb.and(qBranch.branchCode.containsIgnoreCase(search).or(qBranch.branchCode.containsIgnoreCase(search)));
        }

        //update pageable object to ignorecase in sorting
        Sort sort = pageable.getSort();
        sort.forEach(order -> order.ignoreCase());
        PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return branchRepository.findAll(bb.getValue(), pageRequest);
    }

    @Override
    public Object deleteBranchesInBulk(List<Long> branchIdList) {
        return null;
    }

    @Override
    public Object deleteBranch(Long id) {
        return null;
    }

}
