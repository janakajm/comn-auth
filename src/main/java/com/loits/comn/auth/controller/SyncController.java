package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.services.SyncService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * Managing Modules
 *
 * @author Minoli de Silva - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/sync/v1")
@SuppressWarnings("unchecked")
public class SyncController {

    Logger logger = LogManager.getLogger(SyncController.class);

    @Autowired
    SyncService syncService;

    /**
     * Sync branches
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/branch", produces = "application/json")
    public @ResponseBody
    Object syncBranches(@PathVariable(value = "tenent") String tenent,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Syncing branches details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return syncService.synchBranches(tenent);
    }


    /**
     * Sync User-Groups
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/user-group", produces = "application/json")
    public @ResponseBody
    Object syncUserGroups(@PathVariable(value = "tenent") String tenent,
                    @RequestParam(name = "projection",
                            defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Syncing usergroup details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return syncService.syncUserGroups(tenent);
    }


    /**
     * Sync Users
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/user-profile", produces = "application/json")
    public @ResponseBody
    Object syncUsers(@PathVariable(value = "tenent") String tenent,
                          @RequestParam(name = "projection",
                                  defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Syncing user profile details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return syncService.syncUsers(tenent);
    }

    /**
     * Sync Users
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/user-group-user", produces = "application/json")
    public @ResponseBody
    Object syncUserGroupUsers(@PathVariable(value = "tenent") String tenent,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Syncing usergroup users details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return syncService.syncUserGroupUsers(tenent);
    }
    
    /**
     * Sync Users
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/user-profile/{profileId}", produces = "application/json")
    public @ResponseBody
    Object syncUsersByProfileId(@PathVariable(value = "tenent") String tenent,@PathVariable(value = "profileId") Long profileId,@RequestParam(name = "projection",
                                  defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Syncing user profile details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return syncService.syncUsersByProfileId(tenent,profileId);
    }


}