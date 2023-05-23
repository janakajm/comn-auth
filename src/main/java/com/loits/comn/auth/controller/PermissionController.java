package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.SuccessAndErrorDetailsResource;
import com.loits.comn.auth.dto.AddPermission;
import com.loits.comn.auth.dto.UpdatePermission;
import com.loits.comn.auth.services.PermissionService;
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

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;


/**
 * Managing Permissions
 *
 * @author Minoli de Silva - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/permission/v1")
@SuppressWarnings("unchecked")
public class PermissionController {

    Logger logger = LogManager.getLogger(PermissionController.class);

    @Autowired
    PermissionService permissionService;
    

    /**
     * Get all Permissions
     *
     * @param tenent
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}", produces = "application/json")
    public @ResponseBody
    Page<?> getPermissions(@PathVariable(value = "tenent") String tenent,
                           @PageableDefault(size = 10) Pageable pageable,
                           @RequestParam(value = "bookmarks", required = false) String bookmarks,
                           @RequestParam(value = "searchq", required = false) String search,
                           @QuerydslPredicate(root = Permission.class) Predicate predicate,
                           @RequestParam(name = "projection",
                                   defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Permission details.(Projection: %s | Tenent: %s )",
                projection, tenent));

        return permissionService.getAll(pageable, bookmarks, search, predicate, projection, tenent);

    }


    /**
     * Get all Permissions Not-Pageable
     *
     * @param tenent
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/{tenent}/export", produces = "application/json")
    public ResponseEntity<?>
    getPermissionExport(@PathVariable(value = "tenent") String tenent,
                           @RequestParam(value = "bookmarks", required = false) String bookmarks,
                           @QuerydslPredicate(root = Permission.class) Predicate predicate,
                           @RequestParam(name = "projection",
                                   defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Permission details for Export.(Projection: %s | Tenent: %s )",
                projection, tenent));

        Resources resources = new Resources(permissionService.getAllForExport(bookmarks, predicate, projection, tenent));
        return ResponseEntity.ok(resources);
    }

    /**
     * Get permission by Id
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

        logger.debug(String.format("Loading Individual Permission details.(Id: %s | Projection: %s | Tenent: %s )",
                id, projection, tenent));

        Resource resource = new Resource(permissionService.getOne(projection, tenent, id));
        return ResponseEntity.ok(resource);

    }

    /**
     * Create new Permission
     *
     * @param tenent
     * @param projection
     * @param addPermission
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> addPermission(@PathVariable(value = "tenent") String tenent,
                                           @RequestParam(value = "projection",
                                                   defaultValue = "defaultProjection") String projection,
                                           @RequestBody @Valid AddPermission addPermission,
                                           @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                           @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate
    ) throws FXDefaultException {
        logger.debug(String.format("Creating Permission data.(Projection: %s |" +
                "| User : %s " +
                " | Tenent: %s)", projection, user, tenent));

        Resource resource = new Resource(permissionService.create(projection, addPermission, user, tenent, tenantUpdate,false));
        return ResponseEntity.ok(resource);
    }

    /**
     * Create Permissions in Bulk
     *
     * @param tenent
     * @param projection
     * @param addPermissionList
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}/bulk-create", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> addPermissionBulk(@PathVariable(value = "tenent") String tenent,
                                               @RequestParam(value = "projection",
                                                       defaultValue = "defaultProjection") String projection,
                                               @RequestBody @Valid List<AddPermission> addPermissionList,
                                               @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                               @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate
    ) throws FXDefaultException {
    	 System.out.println(String.format("=======Creating Permission Bulk data.(Projection: %s |" +
                "| User : %s " +
                " | Tenent: %s)", projection, user, tenent));

        Resource resource = new Resource(permissionService.bulkCreate(projection, addPermissionList, user, tenent, tenantUpdate, true,false));
        return ResponseEntity.ok(resource);
    }

    /**
     * Update existing Permission
     *
     * @param tenent
     * @param id
     * @param projection
     * @param updatePermission
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/{id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updatePermission(@PathVariable(value = "tenent") String tenent,
                                              @PathVariable(value = "id") Long id,
                                              @RequestParam(value = "projection",
                                                      defaultValue = "defaultProjection") String projection,
                                              @RequestBody @Valid UpdatePermission updatePermission,
                                              @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                              @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate
    ) throws FXDefaultException {

        logger.debug(String.format("Updating Permission data.(Projection: %s " +
                " | Id : %s | User : %s | Tenent : %s)", projection, id, user, tenent));

        Resource resource = new Resource(permissionService.update(projection, id, updatePermission, user, tenent, tenantUpdate));
        return ResponseEntity.ok(resource);
    }

    /**
     * Delete existing PermissionResponse
     *
     * @param tenent
     * @param id
     * @param projection
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/{tenent}/{id}")
    public @ResponseBody
    ResponseEntity<?> deletePermission(@PathVariable(value = "tenent") String tenent,
                                       @PathVariable(value = "id") Long id,
                                       @RequestParam(value = "projection",
                                               defaultValue = "defaultProjection") String projection,
                                       @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                       @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate)
            throws FXDefaultException {


        logger.debug(String.format("Deleting Permission data.(Projection: %s |" +
                "Id : %s | User : %s| Tenent : %s)", projection, id, user, tenent));

        Resource resource = new Resource(permissionService.delete(projection, id, user, tenent, tenantUpdate));
        return ResponseEntity.ok(resource);
    }
    
    /**
     * Delete existing PermissionResponse
     *
     * @param tenent
     * @param projection
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/delete-permission/{tenent}")
    public @ResponseBody
    List<?> permissionDelete(@PathVariable(value = "tenent") String tenent,
                                       @RequestParam(value = "projection",
                                       defaultValue = "defaultProjection") String projection,
                                       @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                       @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate)
        throws FXDefaultException {
    	
    	ArrayList<String> deletedPermissionList=new ArrayList<>();
    	
    	Iterable<Permission> dbPermissionList = permissionService.getAllDeletedPermission();
    	
    	List PermissionListFromAuth0 = permissionService.getAllPermissionFromAuth0(null, null, null,null, tenent);
		
		String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
			
		LoggerRequest.getInstance().logInfo("START_LOOP");
		boolean foundOnAuth0 = false;
		
		for(Permission permissionFromDB:dbPermissionList){
			ListIterator<LinkedHashMap> PermissionFromAuth0ListIterator = PermissionListFromAuth0.listIterator();

			while (PermissionFromAuth0ListIterator.hasNext()) {
				LinkedHashMap premissionFromAuth0 = PermissionFromAuth0ListIterator.next();

				if (applicationId!=null && applicationId.equals(premissionFromAuth0.get("applicationId")) && permissionFromDB.getName().equals(premissionFromAuth0.get("name"))) {
					foundOnAuth0 = true;
					break;
				}	
			}
			
			if(foundOnAuth0) {
				permissionService.delete(projection, permissionFromDB.getId(), user, tenent, false);
			}else {
				deletedPermissionList.add(permissionFromDB.getName());
				try {
					permissionService.deletePermission(permissionFromDB);
				} catch (Exception e) {
					LoggerRequest.getInstance().logInfo("Exception_DELETE "+e.toString());
				}
			}
			
		}
		
        return deletedPermissionList;
    }


    /**
     * Update Permissions in Auth0 - API
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}/update-auth", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updateApiPermissions(@PathVariable(value = "tenent") String tenent,
                                           @RequestParam(value = "projection",
                                                   defaultValue = "defaultProjection") String projection,
                                           @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {
        logger.debug(String.format("Updating Permission in Auth0 API.(Projection: %s |" +
                "| User : %s " +
                " | Tenent: %s)", projection, user, tenent));

        return permissionService.updateAuthAPI(projection, user, tenent);
    }
    
    /**
     * Update Permissions
     *
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/update-auth/permission/{id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updatePermissionsAuth0Field(@PathVariable(value = "tenent") String tenent,@PathVariable(value = "id", required = true) Long id ,@RequestHeader(value = "username", defaultValue = "sysUser") String username){
    	SuccessAndErrorDetailsResource successAndErrorDetailsResource=new SuccessAndErrorDetailsResource(); 	
        try {
			permissionService.updatePermissionsAuth0Field(id,username, tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo(e.toString());
		}
        successAndErrorDetailsResource = new SuccessAndErrorDetailsResource("OK");
		return new ResponseEntity<>(successAndErrorDetailsResource,HttpStatus.OK);
    }
}