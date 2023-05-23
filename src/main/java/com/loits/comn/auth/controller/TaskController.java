package com.loits.comn.auth.controller;

import com.loits.comn.auth.commons.HealthCheck;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.AsyncTask;
import com.loits.comn.auth.domain.Branch;
import com.loits.comn.auth.services.TaskService;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Handlign API status
 *
 * @author Lahiru Bandara - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/task/v1")
@SuppressWarnings("unchecked")
public class TaskController {

  Logger logger = LogManager.getLogger(TaskController.class);

  @Autowired
  TaskService taskService;

  /**
   * Get main asyncTasks
   *
   * @param tenent
   * @param pageable
   * @param bookmarks
   * @param projection
   * @return
   */
  @GetMapping(path = "/{tenent}", produces = "application/json")
  public @ResponseBody
  Page<?> getTasks(@PathVariable(value = "tenent") String tenent,
                   @PageableDefault(size = 10) Pageable pageable,
                   @RequestParam(value = "bookmarks", required = false) String bookmarks,
                   @QuerydslPredicate(root = AsyncTask.class) Predicate predicate,
                   @RequestParam(name = "searchq", required = false) String search,
                   @RequestParam(name = "projection",
                           defaultValue = "defaultProjection") String projection) throws FXDefaultException {

    logger.debug(String.format("Loading Task details.(Projection: %s | Tenent: %s)",
            projection, tenent));

    return taskService.getAll(pageable, bookmarks, predicate, projection, search);
  }
  /**
   * Get main asyncTask by id
   *
   * @param tenent
   * @param projection
   * @return
   */
  @GetMapping(path = "/{tenent}/{id}", produces = "application/json")
  public @ResponseBody
  Object getTaskById(@PathVariable(value = "tenent") String tenent,
                      @PathVariable(value = "id") Long id,
                   @RequestParam(name = "projection",
                           defaultValue = "defaultProjection") String projection) throws FXDefaultException {

    logger.debug(String.format("Loading Task details.(Projection: %s | Tenent: %s)",
            projection, tenent));

    return taskService.getOne(id, projection);
  }

}