package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.Module;
import com.loits.comn.auth.dto.AddModule;
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
@RequestMapping("/module/v1")
@SuppressWarnings("unchecked")
public class ModuleController {

    Logger logger = LogManager.getLogger(ModuleController.class);

    @Autowired
    ModuleService moduleService;

    /**
     * Get all Modules
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
                     @QuerydslPredicate(root = Module.class) Predicate predicate,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Module details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return moduleService.getAll(pageable, bookmarks, predicate, projection);
    }

    /**
     * Create new Module
     *
     * @param tenent
     * @param projection
     * @param addModule
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> addRole(@PathVariable(value = "tenent") String tenent,
                                     @RequestParam(value = "projection",
                                             defaultValue = "defaultProjection") String projection,
                                     @RequestBody @Valid AddModule addModule,
                                     @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {
        logger.debug(String.format("Creating Module data.(Projection: %s |" +
                " | User : %s " +
                " | Tenent: %s)", projection, user, tenent));

        Resource resource = new Resource(moduleService.create(projection, addModule, user, tenent));
        return ResponseEntity.ok(resource);
    }

    /**
     * Delete existing Module
     *
     * @param tenent
     * @param code
     * @param projection
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/{tenent}/{code}")
    public @ResponseBody
    ResponseEntity<?> deleteRole(@PathVariable(value = "tenent") String tenent,
                                 @PathVariable(value = "code") String code,
                                 @RequestParam(value = "projection",
                                         defaultValue = "defaultProjection") String projection,
                                 @RequestHeader(value = "username", defaultValue = "sysUser") String user)
            throws FXDefaultException {


        logger.debug(String.format("Deleting Module data.(Projection: %s |" +
                " Code: %s | User : %s| Tenent : %s)", projection, code, user, tenent));

        Resource resource = new Resource(moduleService.delete(projection, code, user, tenent));
        return ResponseEntity.ok(resource);
    }
}