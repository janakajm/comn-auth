package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.AsyncSubTask;
import com.loits.comn.auth.domain.AsyncTask;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AsyncSubTaskRepository extends PagingAndSortingRepository<AsyncSubTask, Long>,
        QuerydslPredicateExecutor<AsyncSubTask> {

    Page<AsyncSubTask> findByAsyncTask(AsyncTask asyncTask, Predicate predicate, Pageable pageable);

}
