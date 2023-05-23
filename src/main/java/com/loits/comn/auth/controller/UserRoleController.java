package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.services.UserRoleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Managing Permissions - NOT S
 *
 * @author Minoli de Silva - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/user-role/v1")
@SuppressWarnings("unchecked")
public class UserRoleController {

  Logger logger = LogManager.getLogger(UserRoleController.class);

  @Autowired
  UserRoleService userRoleService;

  /**
   *
   * @param tenent
   * @param userId
   * @param roleId
   * @param projection
   * @param user
   * @return
   * @throws FXDefaultException
   */
  @PutMapping(path = "/{userId}/{roleId}")
  public @ResponseBody
  ResponseEntity<?> updateRole(@PathVariable(value = "tenent") String tenent,
                               @PathVariable(value = "userId") String userId,
                               @PathVariable(value = "roleId") String roleId,
                               @RequestParam(value = "projection",
                                       defaultValue = "defaultProjection") String projection,
                               @RequestHeader(value = "username", defaultValue = "sysUser") String user)
          throws FXDefaultException {


    logger.debug(String.format("Assigning RoleResponse from User.(Projection: %s |" +
            " User id : %s | RoleResponse id : %s |PermissionResponse : %s| Tenent : %s)", projection, userId, roleId, user, tenent));

    Resource resource = new Resource(userRoleService.update(projection, userId, roleId, user, tenent));
    return ResponseEntity.ok(resource);
  }


  /**
   *
   * @param tenent
   * @param userId
   * @param roleId
   * @param projection
   * @param user
   * @return
   * @throws FXDefaultException
   */
  @DeleteMapping(path = "/{userId}/{roleId}")
  public @ResponseBody
  ResponseEntity<?> deleteRole(@PathVariable(value = "tenent") String tenent,
                               @PathVariable(value = "userId") String userId,
                               @PathVariable(value = "roleId") String roleId,
                               @RequestParam(value = "projection",
                                       defaultValue = "defaultProjection") String projection,
                               @RequestHeader(value = "username", defaultValue = "sysUser") String user)
          throws FXDefaultException {


    logger.debug(String.format("Deleting RoleResponse from User.(Projection: %s |" +
            " User id : %s | RoleResponse id : %s |PermissionResponse : %s| Tenent : %s)", projection, userId, roleId, user, tenent));

    Resource resource = new Resource(userRoleService.delete(projection, userId, roleId, user, tenent));
    return ResponseEntity.ok(resource);
  }
}