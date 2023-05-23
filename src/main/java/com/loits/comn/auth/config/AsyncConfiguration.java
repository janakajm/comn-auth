package com.loits.comn.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfiguration {

  @Value("${loits.tp.size}")
  int THREAD_POOL_SIZE;

  @Value("${loits.tp.queue.size}")
  int THREAD_POOL_QUEUE_SIZE;

  @Bean
  @Primary
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(THREAD_POOL_SIZE);
    executor.setMaxPoolSize(THREAD_POOL_SIZE * 2);

    if (THREAD_POOL_QUEUE_SIZE == -1)
      executor.setQueueCapacity(Integer.MAX_VALUE);
    else
      executor.setQueueCapacity(THREAD_POOL_QUEUE_SIZE);

    executor.setThreadNamePrefix("AsyncTasks-");
    executor.initialize();
    return executor;
  }
}
