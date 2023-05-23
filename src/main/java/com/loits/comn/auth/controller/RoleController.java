package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.SuccessAndErrorDetailsResource;
import com.loits.comn.auth.dto.AddPermission;
import com.loits.comn.auth.dto.AddRole;
import com.loits.comn.auth.dto.RoleDto;
import com.loits.comn.auth.dto.UpdateRole;
import com.loits.comn.auth.services.PermissionService;
import com.loits.comn.auth.services.RoleService;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;

import javax.validation.Valid;


/**
 * Managing Roles
 *
 * @author Minoli de Silva - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/role/v1")
@SuppressWarnings("unchecked")
public class RoleController {

    Logger logger = LogManager.getLogger(RoleController.class);

    @Autowired
    RoleService roleService;
    
    @Autowired
    PermissionService permissionService;
    
    /**
     * Get all Roles
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
                     @RequestParam(value = "searchq", required = false) String search,
                     @RequestParam(value = "bookmarks", required = false) String bookmarks,
                     @QuerydslPredicate(root = Role.class) Predicate predicate,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Role details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return roleService.getAll(pageable, bookmarks, search, predicate, projection);
    }

    /**
     * Get role by Id
     *
     * @param tenent
     * @param id
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/{id}", produces = "application/json")
    public ResponseEntity<?> getPermission(@PathVariable(value = "tenent") String tenent,
                                           @PathVariable(value = "id") Long id,
                                           @RequestParam(name = "projection",
                                                   defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Individual Role details.(Id: %s | Projection: %s | Tenent: %s )",
                id, projection, tenent));

        Resource resource = new Resource(roleService.getOne(projection, tenent, id));
        return ResponseEntity.ok(resource);

    }

    /**
     * Create new Roles
     *
     * @param tenent
     * @param projection
     * @param addRole
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> addRole(@PathVariable(value = "tenent") String tenent,
                                     @RequestParam(value = "projection",
                                             defaultValue = "defaultProjection") String projection,
                                     @RequestBody @Valid AddRole addRole,
                                     @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                     @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate
    ) throws FXDefaultException {
        logger.debug(String.format("Creating Role data.(Projection: %s |" +
                " | User : %s " +
                " | Tenent: %s)", projection, user, tenent));

        Resource resource = new Resource(roleService.create(projection, addRole, user, tenent, tenantUpdate, true,false));
        return ResponseEntity.ok(resource);
    }

    /**
     * Update existing Role
     *
     * @param tenent
     * @param id
     * @param projection
     * @param updateRole
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/{id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updateRole(@PathVariable(value = "tenent") String tenent,
                                        @PathVariable(value = "id") Long id,
                                        @RequestParam(value = "projection",
                                                defaultValue = "defaultProjection") String projection,
                                        @RequestBody @Valid UpdateRole updateRole,
                                        @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                        @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate
    ) throws FXDefaultException {

        logger.debug(String.format("Updating Role data.(Projection: %s " +
                " | Id : %s | User : %s | Tenent : %s)", projection, id, user, tenent));

        Resource resource = new Resource(roleService.update(projection, id, updateRole, user, tenent, tenantUpdate));
        return ResponseEntity.ok(resource);
    }

    /**
     * Delete existing Role
     *
     * @param tenent
     * @param id
     * @param projection
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/{tenent}/{id}")
    public @ResponseBody
    ResponseEntity<?> deleteRole(@PathVariable(value = "tenent") String tenent,
                                 @PathVariable(value = "id") Long id,
                                 @RequestParam(value = "projection",
                                         defaultValue = "defaultProjection") String projection,
                                 @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                 @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate)
            throws FXDefaultException {


        logger.debug(String.format("Deleting Role data.(Projection: %s |" +
                " Role id : %s | User : %s| Tenent : %s)", projection, id, user, tenent));

        Resource resource = new Resource(roleService.delete(projection, id, user, tenent, tenantUpdate));
        return ResponseEntity.ok(resource);
    }
    
    /**
     * Delete existing Role
     *
     * @param tenent
     * @param projection
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/delete-role/{tenent}")
    public @ResponseBody
    List<?> roleDelete(@PathVariable(value = "tenent") String tenent,
                                 @RequestParam(value = "projection",
                                 defaultValue = "defaultProjection") String projection,
                                 @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                 @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate)
            throws FXDefaultException {

    	ArrayList<String> deletedRoleList=new ArrayList<>();
    	
    	Iterable<Role> dbRoleList = roleService.getAllDeletedRole();
    	
    	List RoleListFromAuth0 = roleService.getAllRoleFromAuth0(null, null, null,null, tenent);
		
		String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
			
		LoggerRequest.getInstance().logInfo("START_LOOP");
		boolean foundOnAuth0 = false;
		
		for(Role roleDB:dbRoleList){
			ListIterator<LinkedHashMap> RoleFromAuth0ListIterator = RoleListFromAuth0.listIterator();

			while (RoleFromAuth0ListIterator.hasNext()) {
				LinkedHashMap roleFromAuth0 = RoleFromAuth0ListIterator.next();

				if (applicationId!=null && applicationId.equals(roleFromAuth0.get("applicationId")) && roleDB.getName().equals(roleFromAuth0.get("name"))) {
					foundOnAuth0 = true;
					break;
				}	
			}
			
			if(foundOnAuth0) {
				roleService.delete(projection, roleDB.getId(), user, tenent, false);
			}else {
				deletedRoleList.add(roleDB.getName());
				try {
					roleService.deleteRole(roleDB);
				} catch (Exception e) {
					LoggerRequest.getInstance().logInfo("Exception_DELETE "+e.toString());
				}
			}
			
		}
		
        return deletedRoleList;
    }

    /**
     * Assign permission to  Role
     *
     * @param tenent
     * @param roleId
     * @param permissionId
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/{role-id}/permission/{perm-id}", produces = "application/json")
    public ResponseEntity<?> assignPermission(@PathVariable(value = "tenent") String tenent,
                                              @PathVariable(value = "role-id") Long roleId,
                                              @PathVariable(value = "perm-id") Long permissionId,
                                              @RequestParam(value = "projection",
                                                      defaultValue = "defaultProjection") String projection,
                                              @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                              @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate
    ) throws FXDefaultException {

        logger.debug(String.format("Assigning Permission To Role.(Projection: %s " +
                " | RoleId : %s | PermissionId : %s | User : %s | Tenent : %s)", projection, roleId, permissionId, user, tenent));

        Resource resource = new Resource(roleService.assign(projection, roleId, permissionId, user, tenent, tenantUpdate,false));
        return ResponseEntity.ok(resource);
    }

    /**
     * Delete existing Role
     *
     * @param tenent
     * @param roleId
     * @param permissionId
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/{tenent}/{role-id}/permission/{perm-id}")
    public @ResponseBody
    ResponseEntity<?> removePermission(@PathVariable(value = "tenent") String tenent,
                                       @PathVariable(value = "role-id") Long roleId,
                                       @PathVariable(value = "perm-id") Long permissionId,
                                       @RequestParam(value = "projection",
                                               defaultValue = "defaultProjection") String projection,
                                       @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                       @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate)
            throws FXDefaultException {


        logger.debug(String.format("Remove Permission From Role.(Projection: %s |" +
                " Role id : %s | Permission id : %s |User : %s| Tenent : %s)", projection, roleId, permissionId, user, tenent));

        Resource resource = new Resource(roleService.remove(projection, roleId, permissionId, user, tenent, tenantUpdate));
        return ResponseEntity.ok(resource);
    }
    
    /**
     * Create role in Bulk
     *
     * @param tenent
     * @param projection
     * @param addRoleList
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}/bulk-create", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> addRoleBulk(@PathVariable(value = "tenent") String tenent,
                                               @RequestParam(value = "projection",
                                               defaultValue = "defaultProjection") String projection,
                                               @RequestBody @Valid List<AddRole> addRoleList,
                                               @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                               @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate
    ) throws FXDefaultException {
    	 System.out.println(String.format("=======Creating Role Bulk data.(Projection: %s |" +
                "| User : %s " +
                " | Tenent: %s)", projection, user, tenent));

        Resource resource = new Resource(roleService.bulkCreate(projection, addRoleList, user, tenent, tenantUpdate, true,false));
        return ResponseEntity.ok(resource);
    }
    
    /**
     * Get all Role Not-Pageable
     *
     * @param tenent
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/export", produces = "application/json")
    public ResponseEntity<?>
    getRoleExport(@PathVariable(value = "tenent") String tenent,
                           @RequestParam(value = "bookmarks", required = false) String bookmarks,
                           @RequestParam(name = "projection",
                                   defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Role details for Export.(Projection: %s | Tenent: %s )",
                projection, tenent));

        Resources resources = new Resources(roleService.getAllForExport(bookmarks, null, projection, tenent));
        return ResponseEntity.ok(resources);
    }
    
    /**
     * Update Role
     *
     * @param tenent
     * @param user
     * @return
     * @throws Exception
     */
    @PutMapping(path = "/{tenent}/update-auth/role/{id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updateRolesAuth0Field(@PathVariable(value = "tenent") String tenent,@PathVariable(value = "id", required = true) Long id ,@RequestHeader(value = "username", defaultValue = "sysUser") String username){
    	SuccessAndErrorDetailsResource successAndErrorDetailsResource=new SuccessAndErrorDetailsResource(); 	
        try {
        	roleService.updateRolesAuth0Field(id,username,tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo(e.toString());
		}
        successAndErrorDetailsResource = new SuccessAndErrorDetailsResource("OK");
		return new ResponseEntity<>(successAndErrorDetailsResource,HttpStatus.OK);
    }
    
    /**
     * Update Role Permission
     *
     * @param tenent
     * @param user
     * @return
     * @throws Exception
     */
    @PutMapping(path = "/{tenent}/update-auth/role/{roleId}/permission/{permissionId}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updateRolesPermissionAuth0Field(@PathVariable(value = "tenent") String tenent,@PathVariable(value = "roleId", required = true) Long roleId,@PathVariable(value = "permissionId", required = true) Long permissionId ,@RequestHeader(value = "username", defaultValue = "sysUser") String username){
    	SuccessAndErrorDetailsResource successAndErrorDetailsResource=new SuccessAndErrorDetailsResource(); 	
        try {
        	roleService.updateRolesPermissionAuth0Field(roleId,permissionId,username,tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo(e.toString());
		}
        successAndErrorDetailsResource = new SuccessAndErrorDetailsResource("OK");
		return new ResponseEntity<>(successAndErrorDetailsResource,HttpStatus.OK);
    }
    
}