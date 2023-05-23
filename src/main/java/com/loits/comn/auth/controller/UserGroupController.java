package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.UserGroup;
import com.loits.comn.auth.dto.AddUserGroupGroups;
import com.loits.comn.auth.dto.AddUserGroups;
import com.loits.comn.auth.dto.RemoveGroup;
import com.loits.comn.auth.dto.RemoveUser;
import com.loits.comn.auth.services.UserGroupService;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;


/**
 * Managing Modules
 *
 * @author Minoli de Silva - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/user-group/v1")
@SuppressWarnings("unchecked")
public class UserGroupController {

    Logger logger = LogManager.getLogger(UserGroupController.class);

    @Autowired
    UserGroupService userGroupService;

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
                     @QuerydslPredicate(root = UserGroup.class) Predicate predicate,
                     @RequestParam(name = "searchq", required = false) String search,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading UserGroup details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return userGroupService.getAll(pageable, search, bookmarks, predicate, projection);
    }

    /**
     * Get Group/(s) (Permissions) to User-Group
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @GetMapping(path = "/{tenent}/{id}/role-group", produces = "application/json")
    public Object getGroups(@PathVariable(value = "tenent") String tenent,
                                          @PathVariable(value = "id") Long userGroupId,
                                          @RequestParam(value = "projection",
                                                  defaultValue = "defaultProjection") String projection,
                                          @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Getting UserGroups to Users To RoleGroup.(Projection: %s " +
                "| User : %s | Tenent : %s)", projection, user, tenent));

        return userGroupService.getByUserGroup(projection, userGroupId, user, tenent);
    }


    /**
     * Assign Group/(s) (Permissions) to User-Group
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}/role-group", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> assignGroups(@PathVariable(value = "tenent") String tenent,
                                          @RequestBody @Valid List<AddUserGroupGroups> addUserGroupGroups,
                                          @RequestParam(value = "projection",
                                                  defaultValue = "defaultProjection") String projection,
                                          @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Assigning UserGroups to Users To RoleGroup.(Projection: %s " +
                "| User : %s | Tenent : %s)", projection, user, tenent));

        Resources resource = new Resources(userGroupService.assign(projection, addUserGroupGroups, user, tenent));
        return ResponseEntity.ok(resource);
    }

    /**
     * Remove group from usergroups
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/role-group/{group-id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> removeGroup(@PathVariable(value = "tenent") String tenent,
                                         @RequestBody @Valid List<RemoveGroup> removeGroupList,
                                         @PathVariable(value = "group-id") Long roleGroupId,
                                         @RequestParam(value = "projection",
                                                 defaultValue = "defaultProjection") String projection,
                                         @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Removing Group from UserGroups.(Projection: %s | Group Id: %s" +
                "| User : %s | Tenent : %s)", projection, roleGroupId, user, tenent));

        Resources resource = new Resources(userGroupService.removeBulk(projection, roleGroupId, removeGroupList, user, tenent));
        return ResponseEntity.ok(resource);
    }


}