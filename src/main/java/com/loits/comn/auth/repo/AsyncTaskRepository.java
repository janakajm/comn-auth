package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.AsyncTask;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AsyncTaskRepository extends PagingAndSortingRepository<AsyncTask, Long>,
        QuerydslPredicateExecutor<AsyncTask> {

  AsyncTask findFirstByTask(String task, Sort sort);
}
