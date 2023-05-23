package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LogginAuthentcation;
import com.loits.comn.auth.domain.RoleGroup;
import com.loits.comn.auth.domain.UserProfile;
import com.loits.comn.auth.dto.AddUserGroups;
import com.loits.comn.auth.dto.BranchAssignment;
import com.loits.comn.auth.dto.RemoveUser;
import com.loits.comn.auth.services.UserProfileService;
import com.loits.comn.auth.services.UserService;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;


/**
 * Managing Permissions
 *
 * @author Minoli de Silva - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/user/v1")
@SuppressWarnings("unchecked")
public class UserController {

    Logger logger = LogManager.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @Autowired
    UserProfileService userProfileService;

    /**
     * Get all UserProfiles
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
                     @QuerydslPredicate(root = UserProfile.class) Predicate predicate,
                     @RequestParam(name = "searchq", required = false) String search,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading User Profile details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return userProfileService.getAll(pageable, search, bookmarks, predicate, projection);
    }

    /**
     * Get UserProfile by id
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/{id}", produces = "application/json")
    public @ResponseBody
    Object getUsers(@PathVariable(value = "tenent") String tenent,
                    @PathVariable(value = "id") Long id,
                    @RequestParam(name = "projection",
                            defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading User Profile details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return userProfileService.getOne(tenent, id, projection);
    }

    /**
     * Get roles by user id
     *
     * @param tenent
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/roles", produces = "application/json")
    public @ResponseBody
    Object getUserRoles(@PathVariable(value = "tenent") String tenent,
                        @RequestParam(name = "s", required = false) String s,
                        @RequestParam(name = "projection",
                            defaultValue = "defaultProjection") String projection,
                        @RequestHeader(value = "username", defaultValue = "sysUser") String user

    ) throws FXDefaultException {
    	
    	String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }

        logger.debug(String.format("Loading User Profile details.(Projection: %s | Tenent: %s)",
                projection, tenent));
        return userService.getRoles(projection,user,s,loginUser!=null?loginUser:user,tenent);
    }



    /**
     * Get all Role-Groups
     *
     * @param tenent
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/role-group/delegatable", produces = "application/json")
    public @ResponseBody
    Page<?> getDelegatableUserRoles(@PathVariable(value = "tenent") String tenent,
                     @PageableDefault(size = 10) Pageable pageable,
                     @RequestParam(value = "searchq", required = false) String search,
                     @RequestParam(value = "bookmarks", required = false) String bookmarks,
                     @QuerydslPredicate(root = RoleGroup.class) Predicate predicate,
                     @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Role-Group details.(Projection: %s | Tenent: %s)",
                projection, tenent));
        
        String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }
     
        return userService.getAllDelegatableRoles(pageable, bookmarks, projection, search, loginUser!=null?loginUser:user, predicate, tenent);

    }

    /**
     * Get all Users
     *
     * @param tenent
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/withRoleGroups", produces = "application/json")
    public @ResponseBody
    Page<?> getUsersWithGroups(@PathVariable(value = "tenent") String tenent,
                     @PageableDefault(size = 10) Pageable pageable,
                     @RequestParam(value = "bookmarks", required = false) String bookmarks,
                     @QuerydslPredicate(root = UserProfile.class) Predicate predicate,
                     @RequestParam(value = "searchq", required = false) String search,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading User details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return userService.getAll(pageable, bookmarks, predicate, projection, search);
    }



    /**
     * Get single user with groups
     *
     * @param tenent
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/logged-user", produces = "application/json")
    public @ResponseBody
    ResponseEntity<?> getUser(@PathVariable(value = "tenent") String tenent,
                              @PageableDefault(size = 10) Pageable pageable,
                              @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                              @RequestParam(value = "bookmarks", required = false) String bookmarks,
                              @RequestParam(name = "projection",
                                      defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Logged in User, delgatable group details.(Projection: %s | Tenent: %s)",
                projection, tenent));
        
        String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }

        Resource resource = new Resource(userService.getLoggedInUser(bookmarks, loginUser!=null?loginUser:user, pageable, projection));
        return ResponseEntity.ok(resource);
    }


    /**
     * Assign Group/(s) to User
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}/role-group", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> assignGroups(@PathVariable(value = "tenent") String tenent,
                                          @RequestBody @Valid List<AddUserGroups> addUserGroupsList,
                                          @RequestParam(value = "projection",
                                                  defaultValue = "defaultProjection") String projection,
                                          @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Assigning Groups to Users To RoleGroup.(Projection: %s " +
                "| User : %s | Tenent : %s)", projection, user, tenent));
        
        String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }

        Resources resource = new Resources(userService.assign(projection, addUserGroupsList, loginUser!=null?loginUser:user, tenent));
        return ResponseEntity.ok(resource);
    }

    /**
     * Update Group/(s) to User
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/role-group", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updateUserGroups(@PathVariable(value = "tenent") String tenent,
                                              @RequestBody @Valid List<AddUserGroups> addUserGroupsList,
                                              @RequestParam(value = "projection",
                                                      defaultValue = "defaultProjection") String projection,
                                              @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Update Groups to Users To RoleGroup.(Projection: %s " +
                "| User : %s | Tenent : %s)", projection, user, tenent));
        
        String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }

        Resources resource = new Resources(userService.update(projection, addUserGroupsList, loginUser!=null?loginUser:user, tenent));
        return ResponseEntity.ok(resource);
    }

    /**
     * Remove group from users
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/role-group/{group-id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> removeGroup(@PathVariable(value = "tenent") String tenent,
                                         @RequestBody @Valid List<RemoveUser> removeUserList,
                                         @PathVariable(value = "group-id") Long roleGroupId,
                                         @RequestParam(value = "projection",
                                                 defaultValue = "defaultProjection") String projection,
                                         @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Removing Group from Users.(Projection: %s | Group Id: %s" +
                "| User : %s | Tenent : %s)", projection, roleGroupId, user, tenent));
        
        String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }

        Resources resource = new Resources(userService.removeBulk(projection, roleGroupId, removeUserList, loginUser!=null?loginUser:user, tenent));
        return ResponseEntity.ok(resource);
    }

    /**
     * Remove expired groups
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/{tenent}/role-group", produces = "application/json")
    public ResponseEntity<?> removeExpiredGroups(@PathVariable(value = "tenent") String tenent,
                                                 @RequestParam(value = "projection",
                                                         defaultValue = "defaultProjection") String projection,
                                                 @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Removing Expired from Users.(Projection: %s " +
                "| User : %s | Tenent : %s)", projection, user, tenent));
        
        String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }

        Resource resource = new Resource(userService.removeExpired(projection, loginUser!=null?loginUser:user, tenent));
        return ResponseEntity.ok(resource);
    }

    /**
     * Get branches of User
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @GetMapping(path = "/{tenent}/{id}/branch", produces = "application/json")
    public Object getBranches(@PathVariable(value = "tenent") String tenent,
                                          @PathVariable(value = "id") Long id,
                                          @RequestParam(value = "projection",
                                                  defaultValue = "defaultProjection") String projection,
                                          @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Getting branches of user.(Projection: %s " +
                "| User : %s | Tenent : %s)", projection, user, tenent));
        
        String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }

        return userService.getBranches(projection, id, loginUser!=null?loginUser:user, tenent);
    }

    /**
     * Assign Branch to User
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}/{id}/branch/{test}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> assignBranch(@PathVariable(value = "tenent") String tenent,
                                          @PathVariable(value = "id") Long id,
                                          @PathVariable(value = "test") Long branchId,
                                          @RequestBody @Valid BranchAssignment branchAssignment,
                                          @RequestParam(value = "projection",
                                                  defaultValue = "defaultProjection") String projection,
                                          @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Assigning Branch to User To RoleGroup.(Projection: %s " +
                "| User : %s | Tenent : %s)", projection, user, tenent));
        
        String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }

        Resource resource = new Resource(userService.assignBranch(projection, id, branchId, branchAssignment, loginUser!=null?loginUser:user, tenent));
        return ResponseEntity.ok(resource);
    }

    /**
     * Assign Branch to User
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/{id}/branch/{bId}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updateBranchAssignment(@PathVariable(value = "tenent") String tenent,
                                                    @PathVariable(value = "id") Long id,
                                                    @PathVariable(value = "bId") Long branchId,
                                                    @RequestBody @Valid BranchAssignment branchAssignment,
                                                    @RequestParam(value = "projection",
                                                            defaultValue = "defaultProjection") String projection,
                                                    @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Assigning Branch to User To RoleGroup.(Projection: %s " +
                "| User : %s | Tenent : %s)", projection, user, tenent));
        
        String loginUser=null;
        if(LogginAuthentcation.getUserName()!=null) {
        	loginUser=LogginAuthentcation.getUserName().toString();
        }

        Resource resource = new Resource(userService.updateBranch(projection, id, branchId, branchAssignment, loginUser!=null?loginUser:user, tenent));
        return ResponseEntity.ok(resource);
    }

}