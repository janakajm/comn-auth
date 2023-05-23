package com.loits.comn.auth.repo;

import com.loits.comn.auth.domain.*;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface ScheduledJobRepository extends PagingAndSortingRepository<ScheduledJobs, Long>,
        QuerydslPredicateExecutor<ScheduledJobs>, QuerydslBinderCustomizer<QScheduledJobs> {

    Optional<ScheduledJobs> findTop1ByJobOrderByStartedTimeDesc(ScheduledJobs.JobType job);


    @Override
    default public void customize(QuerydslBindings bindings, QScheduledJobs root) {
        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    };

}
