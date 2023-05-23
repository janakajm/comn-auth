package com.loits.comn.auth.services.impl;


import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.QAsyncTask;
import com.loits.comn.auth.repo.AsyncTaskRepository;
import com.loits.comn.auth.services.PermissionService;
import com.loits.comn.auth.services.TaskService;
import com.loits.comn.auth.services.projections.AsyncTaskProjection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TaskServiceImpl implements TaskService {

    Logger logger = LogManager.getLogger(TaskServiceImpl.class);

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    AsyncTaskRepository asyncTaskRepository;

    @Override
    public Page<?> getAll(Pageable pageable, String bookmarks, Predicate predicate, String projection, String search) {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QAsyncTask qAsyncTask = QAsyncTask.asyncTask;

        //search by name on demand
        if(search!=null && !search.isEmpty()){
            bb.and(qAsyncTask.task.containsIgnoreCase(search));
        }

        //update pageable object to ignorecase in sorting
        Sort sort = pageable.getSort();
        sort.forEach(order -> order.ignoreCase());
        PageRequest pageRequest =null;

        if(sort.isUnsorted()){
             pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "sDate"));

        }else{
            sort.forEach(order -> order.ignoreCase());
             pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }

        return asyncTaskRepository.findAll(bb.getValue(), pageRequest).map(
                asyncTask -> projectionFactory.createProjection(AsyncTaskProjection.class,asyncTask)
        );
    }

    @Override
    public Object getOne(Long id, String projection) throws FXDefaultException {
        if(!asyncTaskRepository.existsById(id))   {
            throw  new FXDefaultException("3001","INVALID_ATTEMPT","Task by given id does not exist!", new Date(), HttpStatus.BAD_REQUEST, false);
        }

        return projectionFactory.createProjection(AsyncTaskProjection.class, asyncTaskRepository.findById(id));
    }
}
