package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;

public interface ScheduledJobService {

    void startCronTasks(String user,String tenant ) throws FXDefaultException;

    String getCronExpression();
   
}
