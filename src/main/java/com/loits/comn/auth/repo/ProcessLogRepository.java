package com.loits.comn.auth.repo;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.loits.comn.auth.domain.ProcessLog;

@RepositoryRestResource(exported = false)
public interface ProcessLogRepository extends PagingAndSortingRepository<ProcessLog, Long>,
QuerydslPredicateExecutor<ProcessLog>{
	
	@Query("FROM ProcessLog WHERE processCode=:code AND process='1'")
    List<ProcessLog> findByProcessCode(@Param("code") String code);

	@Transactional
	@Modifying
	@Query("DELETE FROM ProcessLog WHERE processCode =:code")
	public void deleteProcess(@Param("code") String code);

}
