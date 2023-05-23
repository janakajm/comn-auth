package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.services.ScheduledJobService;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@CrossOrigin(origins = "*")
@RestController
@EnableScheduling
@RequestMapping("/role/v1")
@SuppressWarnings("unchecked")
public class ScheduledJobController {

    @Autowired
    ScheduledJobService scheduledJobService;

    Logger logger = LogManager.getLogger(ScheduledJobController.class);


    //@Scheduled(cron = "0 0 * ? * *")
    public void runtASK() {
    	LoggerRequest.getInstance().logInfo("DPD run task started. Current Time : " + new Date());
        try {
            scheduledJobService.startCronTasks("User","AnRkr");
        } catch (Exception e) {
            e.printStackTrace();
            LoggerRequest.getInstance().logInfo("Exception : " + e.toString());
        }
    }

//    @Bean
//    public String getAutoDPDRunTime() {

//        if (config == null || StringUtils.isEmpty(config.getValueString()) || config.getValueString().equalsIgnoreCase("00:00")) {
//            logger.warn("DPD run task is disabled or not defined. Scheduler set to default");
//        } else {
//            try {
//                scheduledJobService.startCronTasks();
//
//                return cronTask;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        return "0 0 * ? * *";
//    }
    
}
