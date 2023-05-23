package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.AsyncSubTask;
import com.loits.comn.auth.domain.AsyncTask;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

@Projection(types = {AsyncSubTask.class}, name = "asyncSubTask")
public interface AsyncSubTaskProjection {

    Long getId();

    String getTask();

    Date getSDate();

    Date getEDate();

    Date getMDate();

    String getStatus();

    String getMeta();

    String getSeverity();

    String getNotify();

//    AsyncTask getAsyncTask();

//    interface AsyncTask{
//        String getTask();
//
//        String getStatus();
//
//        String getModule();
//
//        String getMeta();
//
//        String getSeverity();
//
//        String getNotify();
//    }

}