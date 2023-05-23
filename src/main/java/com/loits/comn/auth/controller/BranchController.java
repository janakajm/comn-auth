package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.Branch;
import com.loits.comn.auth.domain.Module;
import com.loits.comn.auth.dto.AddModule;
import com.loits.comn.auth.repo.BranchRepository;
import com.loits.comn.auth.services.BranchService;
import com.loits.comn.auth.services.ModuleService;
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

import javax.validation.Valid;


/**
 * Managing Modules
 *
 * @author Minoli de Silva - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/branch/v1")
@SuppressWarnings("unchecked")
public class BranchController {

    Logger logger = LogManager.getLogger(BranchController.class);

    @Autowired
    BranchService branchService;

    @Autowired
    BranchRepository branchRepository;

    /**
     * Get all Branches
     *
     * @param tenent
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}", produces = "application/json")
    public @ResponseBody
    Page<?> getUsers(@PathVariable(value = "tenent") String tenent,
                     @PageableDefault(size = 10) Pageable pageable,
                     @RequestParam(value = "bookmarks", required = false) String bookmarks,
                     @QuerydslPredicate(root = Branch.class) Predicate predicate,
                     @RequestParam(name = "searchq", required = false) String search,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Branch details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return branchService.getAll(pageable, bookmarks, predicate, projection, search);
    }

    /**
     * Delete branches (test endpoint)
     *
     * @param tenent
     * @param projection
     * @return
     */
    @DeleteMapping(path = "/{tenent}", produces = "application/json")
    public @ResponseBody
    void deleteBranches(@PathVariable(value = "tenent") String tenent,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Branch details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        branchRepository.deleteAll();
    }


}