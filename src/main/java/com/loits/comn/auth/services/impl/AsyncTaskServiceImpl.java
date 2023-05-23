package com.loits.comn.auth.services.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loits.comn.auth.commons.AsyncTaskDef;
import com.loits.comn.auth.commons.HibernateProxyTypeAdapter;
import com.loits.comn.auth.domain.AsyncLog;
import com.loits.comn.auth.domain.AsyncSubTask;
import com.loits.comn.auth.domain.AsyncTask;
import com.loits.comn.auth.repo.AsyncLogRepository;
import com.loits.comn.auth.repo.AsyncSubTaskRepository;
import com.loits.comn.auth.repo.AsyncTaskRepository;
import com.loits.comn.auth.services.AsyncTaskService;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * @author Lahiru Bandara - Infinitum360
 * @version 1.0.0
 */

@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

  Logger logger = LogManager.getLogger(AsyncTaskServiceImpl.class);

  @Autowired
  AsyncTaskRepository asyncTaskRepository;

  @Autowired
  AsyncSubTaskRepository asyncSubTaskRepository;

  @Autowired
  AsyncLogRepository asyncLogRepository;

  @Value("${com.loits.auth.api.date-format}")
  private String dateFormat;

  SimpleDateFormat sdf;

  @PostConstruct
  public void init() {
    this.sdf = new SimpleDateFormat(dateFormat);
  }


  @Override
  public AsyncTask getAsyncTaskDurationPopulated(String KEY, String type,
                                                 int defaultBackDays) {

    Sort sort = Sort.by("id").descending(); // data sorting strategy
    AsyncTask lastSync = this.asyncTaskRepository.findFirstByTask(type, sort);

    Calendar nextSyncStart = Calendar.getInstance();
    AsyncTask nextSync = new AsyncTask();
    nextSync.setEDate(new Timestamp(new Date().getTime()));

    if (lastSync == null) {
      // first sync
      logger.debug(KEY + "synchronizing data for the first time");
      nextSyncStart.setTime(new Date());

      if (defaultBackDays != 0) {
        nextSyncStart.add(Calendar.DATE, defaultBackDays);
      } else
        nextSyncStart.add(Calendar.DATE, -1);
    } else if (lastSync.getStatus().equals(AsyncTaskDef.TASK_COMPLETED)) {
      logger.debug("last sync has been successful. Run a new sync");
      // Previous sync strategy has fetched data or pushed to AML
      nextSyncStart.setTime(lastSync.getEDate());
    } else {
      // Last sync had an error. Start over.
      logger.debug(KEY + "last sync had an error. Start over");
      nextSyncStart.setTime(lastSync.getSDate());
    }

    if (defaultBackDays < 0)
      nextSyncStart.add(Calendar.DATE, defaultBackDays);

    nextSync.setSDate(new Timestamp(nextSyncStart.getTime().getTime()));
    nextSync.setMDate(new Date()); // modified date is same as start date at this point
    nextSync.setEDate(getEndOfDay(new Date()));

    logger.debug(KEY + String.format("Sync task run from : %s   to : %s",
            sdf.format(nextSync.getSDate().getTime()),
            sdf.format(nextSync.getEDate().getTime())));

    return nextSync;
  }


  public Date getEndOfDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 999);
    return calendar.getTime();
  }


  @Override
  public AsyncTask saveTask(AsyncTask task, String id,
                            String currentStatus, String activity,
                            HashMap<String, Object> meta) {

    GsonBuilder b = new GsonBuilder();
    b.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
    Gson gson = b.create();

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    task.setSDate(task.getSDate() == null ? new Date() : task.getSDate());

    if (currentStatus.equalsIgnoreCase(AsyncTaskDef.TASK_INITIATED)) {
      task.setJobId(String.valueOf(id));
      task.setModule(AsyncTaskDef.Module.RO);
      task.setTask(activity);
    }

    // see if HM contain any high level meta
    if (meta != null && !meta.isEmpty()) {
      task.setMeta(gson.toJson(meta));
    }

    // set appropriate date
    if (currentStatus.equalsIgnoreCase(AsyncTaskDef.TASK_COMPLETED)
            || currentStatus.equalsIgnoreCase(AsyncTaskDef.TASK_ERROR)) {
      task.setEDate(new Timestamp(new Date().getTime()));
    } else task.setMDate(new Timestamp(new Date().getTime()));
    task.setStatus(currentStatus);

    try{
      task = this.asyncTaskRepository.save(task);
    }catch (Exception e){
      e.printStackTrace();
    }
    return task;
  }

  @Override
  public AsyncTask getLastRunTaskByActivity(String activity) {
    Sort sort = Sort.by("sDate").descending(); // data sorting strategy
    return this.asyncTaskRepository.findFirstByTask(activity, sort);

  }

  @Override
  public AsyncSubTask saveSubTask(AsyncSubTask subTask, AsyncTask asyncTask,
                                  String currentStatus, String activity,
                                  HashMap<String, Object> meta) {
    GsonBuilder b = new GsonBuilder();
    b.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
    Gson gson = b.create();

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());

    if (currentStatus.equalsIgnoreCase(AsyncTaskDef.TASK_INITIATED)) {
      // set default values
      subTask.setStatus(AsyncTaskDef.TASK_INITIATED);
      subTask.setTask(activity);
      subTask.setSDate(cal.getTime());
      subTask.setAsyncTask(asyncTask);
    }

    // see if HM contain any high level meta
    if (meta != null && !meta.isEmpty()) {
      subTask.setMeta(gson.toJson(meta));
    }

    // set appropriate date
    if (currentStatus.equalsIgnoreCase(AsyncTaskDef.TASK_COMPLETED)) {
      subTask.setEDate(new Timestamp(new Date().getTime()));
    } else subTask.setMDate(new Timestamp(new Date().getTime()));
    subTask.setStatus(currentStatus);

    return this.asyncSubTaskRepository.save(subTask);
  }

  @Override
  public AsyncLog saveSubTaskLog(AsyncSubTask subTask, String reference, String error,
                                 String refKey,
                                 String refValue,
                                 String refTalbe, Exception e) {
    AsyncLog log = new AsyncLog();

    if (e != null) {
      log.setDescription(e.getMessage());
      log.setStacktrace(ExceptionUtils.getStackTrace(e));
    }

    log.setDescription(reference);
    log.setRefTable(refTalbe);
    log.setRefKey(refKey);
    log.setRefValue(refValue);
    log.setLogTime(new Date());

    if (subTask != null) {
      log.setAsyncSubTask(subTask);
    }

    return this.asyncLogRepository.save(log);
  }


  @Override
  public AsyncLog saveTaskLog(AsyncTask task, String reference, String error,
                              String refKey,
                              String refValue,
                              String refTalbe, Exception e) {
    AsyncLog log = new AsyncLog();

    if (e != null) {
      log.setDescription(e.getMessage());
      log.setStacktrace(ExceptionUtils.getStackTrace(e));
    }

    log.setDescription(reference);
    log.setRefTable(refTalbe);
    log.setRefKey(refKey);
    log.setRefValue(refValue);
    log.setLogTime(new Date());


    if (task != null) {
      log.setAsyncTask(task);
    }

    return this.asyncLogRepository.save(log);
  }

}
