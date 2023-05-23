package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.services.DataVerifyService;
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
@RequestMapping("/verify/v1")
@SuppressWarnings("unchecked")
public class DataVerifyController {

    Logger logger = LogManager.getLogger(DataVerifyController.class);

    @Autowired
    DataVerifyService dataVerifyService;

    /**
     * Sync Permissions
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/permission", produces = "application/json")
    public @ResponseBody
    Object syncPermissions(@PathVariable(value = "tenent") String tenent,
                           @RequestParam(name = "projection",
                                   defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Syncing permission details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return dataVerifyService.syncPermissions(tenent);
    }

    /**
     * Sync Permissions
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/role", produces = "application/json")
    public @ResponseBody
    Object syncRoles(@PathVariable(value = "tenent") String tenent,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Syncing role details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return dataVerifyService.syncRoles(tenent);
    }

    /**
     * Sync Groups
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/group", produces = "application/json")
    public @ResponseBody
    Object syncGroups(@PathVariable(value = "tenent") String tenent,
                      @RequestParam(name = "projection",
                              defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Syncing group details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return dataVerifyService.syncGroups(tenent);
    }

    /**
     * Sync Groups
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/user-role", produces = "application/json")
    public @ResponseBody
    Object syncUserRole(@PathVariable(value = "tenent") String tenent,
                      @RequestParam(name = "projection",
                              defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Syncing group details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return dataVerifyService.syncUserRoles(tenent);
    }
}