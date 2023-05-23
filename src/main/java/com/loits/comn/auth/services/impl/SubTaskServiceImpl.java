package com.loits.comn.auth.services.impl;


import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.AsyncTask;
import com.loits.comn.auth.domain.QAsyncSubTask;
import com.loits.comn.auth.domain.QAsyncTask;
import com.loits.comn.auth.repo.AsyncSubTaskRepository;
import com.loits.comn.auth.repo.AsyncTaskRepository;
import com.loits.comn.auth.services.SubTaskService;
import com.loits.comn.auth.services.projections.AsyncSubTaskProjection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SubTaskServiceImpl implements SubTaskService {

    Logger logger = LogManager.getLogger(SubTaskServiceImpl.class);

    @Autowired
    RestTemplateBuilder builder;

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    AsyncSubTaskRepository asyncSubTaskRepository;

    @Autowired
    AsyncTaskRepository asyncTaskRepository;

    @Override
    public Page<?> getAll(Pageable pageable, String bookmarks, Predicate predicate, String projection) {
        BooleanBuilder bb = new BooleanBuilder(predicate);

        return asyncSubTaskRepository.findAll(bb.getValue(), pageable).map(
                asyncSubTask -> projectionFactory.createProjection(AsyncSubTaskProjection.class, asyncSubTask)
        );
    }

    @Override
    public Page<?> getByTask(Pageable pageable, String bookmarks, Long taskId, Predicate predicate, String projection, String search) throws FXDefaultException {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QAsyncSubTask qAsyncSubTask = QAsyncSubTask.asyncSubTask;

        //search asyncsubtasks
        if(search!=null && !search.isEmpty()){
            bb.and(qAsyncSubTask.task.containsIgnoreCase(search));
        }

        if(!asyncTaskRepository.existsById(taskId)){
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", "Task id not valid!", new Date(), HttpStatus.BAD_REQUEST);
        }

        bb.and(qAsyncSubTask.asyncTask.id.eq(taskId));

        return asyncSubTaskRepository.findAll(bb.getValue(),pageable).map(
            asyncSubTask -> projectionFactory.createProjection(AsyncSubTaskProjection.class,asyncSubTask)
        );
    }

}
