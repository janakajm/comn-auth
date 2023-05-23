package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.AsyncLog;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AsyncLogRepository extends PagingAndSortingRepository<AsyncLog, Long>,
        QuerydslPredicateExecutor<AsyncLog> {


}
