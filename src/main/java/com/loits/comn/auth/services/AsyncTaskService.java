package com.loits.comn.auth.services;


import com.loits.comn.auth.domain.AsyncLog;
import com.loits.comn.auth.domain.AsyncSubTask;
import com.loits.comn.auth.domain.AsyncTask;

import java.util.HashMap;

/**
 * @author Lahiru Bandara - Infinitum360
 * @version 1.0.0
 */

public interface AsyncTaskService {

  AsyncTask saveTask(AsyncTask task, String id, String currentStatus,
                     String activity, HashMap<String, Object> meta);

  AsyncTask getLastRunTaskByActivity(String activity);

  AsyncTask getAsyncTaskDurationPopulated(String KEY, String type,
                                          int defaultBackDays);

  AsyncSubTask saveSubTask(AsyncSubTask calcTask, AsyncTask asyncTask,
                           String currentStatus, String activity, HashMap<String, Object> meta);

  AsyncLog saveSubTaskLog(AsyncSubTask calcTask, String reference, String error, String refKey,
                          String refValue,
                          String refTalbe, Exception e);

  AsyncLog saveTaskLog(AsyncTask task, String reference, String error, String refKey,
                       String refValue,
                       String refTalbe, Exception e);


}
