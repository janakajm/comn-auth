package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.AsyncSubTask;
import com.loits.comn.auth.services.SubTaskService;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;


/**
 * Handlign API status
 *
 * @author Lahiru Bandara - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/sub-task/v1")
@SuppressWarnings("unchecked")
public class SubTaskController {

    Logger logger = LogManager.getLogger(SubTaskController.class);

    @Autowired
    SubTaskService subTaskService;

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
    Page<?> getSubTasks(@PathVariable(value = "tenent") String tenent,
                        @PageableDefault(size = 10) Pageable pageable,
                        @RequestParam(value = "bookmarks", required = false) String bookmarks,
                        @QuerydslPredicate(root = AsyncSubTask.class) Predicate predicate,
                        @RequestParam(name = "projection",
                                defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Task details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return subTaskService.getAll(pageable, bookmarks, predicate, projection);
    }

    /**
     * Get main asyncTasks
     *
     * @param tenent
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/task/{id}", produces = "application/json")
    public @ResponseBody
    Page<?> getSubTasksByTask(@PathVariable(value = "tenent") String tenent,
                              @PathVariable(value = "id") Long id,
                              @PageableDefault(size = 10) Pageable pageable,
                              @RequestParam(value = "bookmarks", required = false) String bookmarks,
                              @QuerydslPredicate(root = AsyncSubTask.class) Predicate predicate,
                              @RequestParam(name = "searchq", required = false) String search,
                              @RequestParam(name = "projection",
                                      defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Task details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return subTaskService.getByTask(pageable, bookmarks, id, predicate, projection, search);
    }
}