package com.loits.comn.auth.services.projections;

import com.loits.comn.auth.domain.AsyncLog;
import com.loits.comn.auth.domain.AsyncSubTask;
import com.loits.comn.auth.domain.AsyncTask;
import com.loits.comn.auth.domain.Role;
import org.springframework.data.rest.core.config.Projection;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Projection(types = {AsyncTask.class}, name = "asyncTask")
public interface AsyncTaskProjection {

    Long getId();

    String getTask();

    Date getSDate();

    Date getEDate();

    Date getMDate();

    String getJobId();

    String getStatus();

    String getModule();

    String getMeta();

    String getSeverity();

    String getNotify();

}