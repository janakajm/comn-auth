package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.services.AuthUserService;
import com.loits.comn.auth.services.SyncService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * Managing Permissions
 *
 * @author Minoli de Silva - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth-user/v1")
@SuppressWarnings("unchecked")
public class AuthUserController {

    Logger logger = LogManager.getLogger(AuthUserController.class);

    @Autowired
    AuthUserService authUserService;
    
    @Autowired
    SyncService syncService;


    /**
     * Get all Auth Users
     *
     * @param tenent
     * @param pageable
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}", produces = "application/json")
    public @ResponseBody String getUsers(@PathVariable(value = "tenent") String tenent,
                     @PageableDefault(size = 10) Pageable pageable,
                     @RequestParam(name = "searchq", required = false) String search,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading User Profile details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return authUserService.getAll(pageable, search, projection);
    }
    
    /**
     * Get all Auth Users
     *
     * @param tenent
     * @param pageable
     * @param projection
     * @return
     */
    @GetMapping(path = "all-user/{tenent}", produces = "application/json")
    public @ResponseBody String getUsersForTesting(@PathVariable(value = "tenent") String tenent,
                     //@PageableDefault(size = 10000) Pageable pageable,
                     @RequestParam(name = "searchq", required = false) String search,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading User Profile details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return authUserService.getAllUser(search, projection);
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