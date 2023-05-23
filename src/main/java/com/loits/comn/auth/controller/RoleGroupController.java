
package com.loits.comn.auth.controller;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.core.LogginAuthentcation;
import com.loits.comn.auth.domain.ProcessLog;
import com.loits.comn.auth.domain.ProviderGroup;
import com.loits.comn.auth.domain.ProviderRole;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.RoleGroup;
import com.loits.comn.auth.domain.RoleGroupRole;
import com.loits.comn.auth.dto.AddRoleGroup;
import com.loits.comn.auth.dto.GroupRoleResponse;
import com.loits.comn.auth.dto.PermissionResponse;
import com.loits.comn.auth.dto.RoleDto;
import com.loits.comn.auth.dto.UpdateRoleGroup;
import com.loits.comn.auth.repo.ProviderGroupRepository;
import com.loits.comn.auth.repo.ProviderRoleRepository;
import com.loits.comn.auth.repo.RoleGroupRepository;
import com.loits.comn.auth.repo.RoleGroupRoleRepository;
import com.loits.comn.auth.repo.RoleRepository;
import com.loits.comn.auth.services.*;
import com.querydsl.core.types.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.syntax.TokenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import javax.validation.Valid;


/**
 * Managing Role-Templates
 *
 * @author Minoli de Silva - Infinitum360
 * @version 1.0.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/role-group/v1")
@SuppressWarnings("unchecked")
public class RoleGroupController {

    Logger logger = LogManager.getLogger(RoleGroupController.class);

    @Autowired
    RoleGroupService roleGroupService;
    
    @Autowired
    RoleRepository roleRepository;
    
    @Autowired
    ProviderRoleRepository providerRoleRepository;
    
    @Value("${auth.provider}")
    private String PROVIDER;
    
    @Autowired
    RoleGroupRoleRepository roleGroupRoleRepository;
    
    @Autowired
    RoleGroupRepository roleGroupRepository;
    
    @Autowired
	PermissionService permissionService;
    
    @Autowired
	ModuleService moduleService;

    @Autowired
	TokenService tokenService;

    public static final int MAX_STATEMENT_PAGEEABLE_LENGTH=10;


    /**
     * Get all Role-Groups
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
                     @QuerydslPredicate(root = RoleGroup.class) Predicate predicate,
                     @RequestParam(name = "projection",
                             defaultValue = "defaultProjection") String projection) throws FXDefaultException {

        logger.debug(String.format("Loading Role-Group details.(Projection: %s | Tenent: %s)",
                projection, tenent));

        return roleGroupService.getAll(pageable, bookmarks, projection, search, predicate, tenent);

    }

    /**
     * Get role group by Id
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

        Resource resource = new Resource(roleGroupService.getOne(projection, tenent, id));
        return ResponseEntity.ok(resource);

    }


    /**
     * Create new Role-Group
     *
     * @param tenent
     * @param projection
     * @param addRoleGroup
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PostMapping(path = "/{tenent}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> addRole(@PathVariable(value = "tenent") String tenent,
                                     @RequestParam(value = "projection",
                                             defaultValue = "defaultProjection") String projection,
                                     @RequestBody @Valid AddRoleGroup addRoleGroup,
                                     @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {
        logger.debug(String.format("Creating Role-Group data.(Projection: %s |" +
                " | User : %s " +
                " | Tenent: %s)", projection, user, tenent));
        
        boolean notInAuth0=false;
        Resource resource = new Resource(roleGroupService.create(projection, addRoleGroup, user, tenent,notInAuth0));
        return ResponseEntity.ok(resource);
    }

    /**
     * Update existing Role-Group
     *
     * @param tenent
     * @param id
     * @param projection
     * @param updateRoleGroup
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/{id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updateRole(@PathVariable(value = "tenent") String tenent,
                                              @PathVariable(value = "id") Long id,
                                              @RequestParam(value = "projection",
                                                      defaultValue = "defaultProjection") String projection,
                                              @RequestBody @Valid UpdateRoleGroup updateRoleGroup,
                                              @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Updating Role-Group data.(Projection: %s " +
                " | Id : %s | User : %s | Tenent : %s)", projection, id, user, tenent));

        Resource resource = new Resource(roleGroupService.update(projection, id, updateRoleGroup, user, tenent));
        return ResponseEntity.ok(resource);
    }

    /**
     * Delete existing Role-Group
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
                                 @RequestHeader(value = "username", defaultValue = "sysUser") String user)
            throws FXDefaultException {


        logger.debug(String.format("Deleting Role-Group data.(Projection: %s |" +
                " Id : %s | User : %s| Tenent : %s)", projection, id, user, tenent));

        Resource resource = new Resource(roleGroupService.delete(projection, id, user, tenent));
        return ResponseEntity.ok(resource);
    }
    
    /**
     * Delete existing Role-Group
     *
     * @param tenent
     * @param projection
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/delete-group/{tenent}")
    public @ResponseBody
    List<?> deleteGroup(@PathVariable(value = "tenent") String tenent,
                                 @RequestParam(value = "projection",
                                         defaultValue = "defaultProjection") String projection,
                                 @RequestHeader(value = "username", defaultValue = "sysUser") String user)
            throws FXDefaultException {
    	
    	ArrayList<String> deletedGroupList=new ArrayList<>();
    	
    	List<ProviderGroup> dbGroupList = roleGroupService.getAllDeletedGroup();
    	
    	List groupListFromAuth0 = roleGroupService.getAllGroupFromAuth0(null, null, null,null,tenent);

		for(ProviderGroup providerGroup:dbGroupList){
			
			ListIterator<LinkedHashMap> groupFromAuth0ListIterator = groupListFromAuth0.listIterator();

			boolean foundOnAuth0=false;
			
			while (groupFromAuth0ListIterator.hasNext()) {
				LinkedHashMap groupFromAuth0 = groupFromAuth0ListIterator.next();

				if (providerGroup.getProviderGroupId().equals(groupFromAuth0.get("_id"))) {
					foundOnAuth0 = true;
					break;
				}	
			}
			
			if(foundOnAuth0) {
				roleGroupService.delete(projection, providerGroup.getProviderGroupIdClass().getGroup(), user, tenent);
			}else {
				deletedGroupList.add(providerGroup.getProviderGroupIdClass().getGroup().toString());
				try {
					roleGroupService.deleteGroup(providerGroup.getProviderGroupIdClass().getGroup());
				} catch (Exception e) {
					LoggerRequest.getInstance().logInfo("Exception_DELETE "+e.toString());
				}
			}
			
		}
		
        return deletedGroupList;
    }

    /**
     * Assign Role to  RoleGroup
     *
     * @param tenent
     * @param roleGroupId
     * @param roleId
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/{role-group-id}/role/{role-id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> assignRole(@PathVariable(value = "tenent") String tenent,
                                              @PathVariable(value = "role-group-id") Long roleGroupId,
                                              @PathVariable(value = "role-id") Long roleId,
                                              @RequestParam(value = "projection",
                                                      defaultValue = "defaultProjection") String projection,
                                              @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {

        logger.debug(String.format("Assigning Role To RoleGroup.(Projection: %s " +
                " | RoleGroupId : %s | RoleId : %s | User : %s | Tenent : %s)", projection, roleGroupId, roleId, user, tenent));

        Resource resource = new Resource(roleGroupService.assign(projection, roleGroupId, roleId, user, tenent,false));
        return ResponseEntity.ok(resource);
    }

    /**
     * Delete existing Role
     *
     * @param tenent
     * @param roleGroupId
     * @param roleId
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/{tenent}/{role-group-id}/role/{role-id}")
    public @ResponseBody
    ResponseEntity<?> removeRole(@PathVariable(value = "tenent") String tenent,
                                       @PathVariable(value = "role-group-id") Long roleGroupId,
                                       @PathVariable(value = "role-id") Long roleId,
                                       @RequestParam(value = "projection",
                                               defaultValue = "defaultProjection") String projection,
                                       @RequestHeader(value = "username", defaultValue = "sysUser") String user)
            throws FXDefaultException {


        logger.debug(String.format("Remove Role From RoleGroup.(Projection: %s |" +
                " RoleGroup id : %s | Role id : %s |User : %s| Tenent : %s)", projection, roleGroupId, roleId, user, tenent));

        Resource resource = new Resource(roleGroupService.remove(projection, roleGroupId, roleId, user, tenent));
        return ResponseEntity.ok(resource);
    }
    
    /**
     * Assign Role to  RoleGroup
     *
     * @param tenent
     * @param roleGroupId
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/missing/role-group/{role-group-id}", produces = "application/json", consumes = "application/json")
    public @ResponseBody HashMap<Long, List<RoleDto>> addMissingRoleToGroup(@PathVariable(value = "tenent") String tenent,
                                              @PathVariable(value = "role-group-id") Long roleGroupId,
                                              @RequestParam(value = "projection",defaultValue = "defaultProjection") String projection,
                                              @PageableDefault(size = 100000) Pageable pageable,
                                              @RequestParam(value = "bookmarks", required = false) String bookmarks,
                                  			  @RequestParam(value = "searchq", required = false) String search,
                                              @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {
    	ArrayList<String> missingRoleList=new ArrayList<>();
    	
    	String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
    	
    	List<String> roleList=roleRepository.findByRoleGroupId(roleGroupId);
    	
    	List RoleListFromAuth0 = roleGroupService.getAllRoleFromAuth0(pageable, bookmarks, search,roleGroupId.toString(),tenent);
    	
    	ListIterator<LinkedHashMap> RoleFromAuth0ListIterator = RoleListFromAuth0.listIterator();
    	
    	while (RoleFromAuth0ListIterator.hasNext()) {
			LinkedHashMap roleFromAuth0 = RoleFromAuth0ListIterator.next();
			//if(roleList.contains(roleFromAuth0.get("_id")) && applicationId!=null && applicationId.equals(roleFromAuth0.get("applicationId"))) {
			if(roleList.contains(roleFromAuth0.get("_id"))) {
				roleList.remove(roleFromAuth0.get("_id"));
			}
    	}
    	boolean notInAuth0=true;
    	HashMap<Long, List<RoleDto>> items = new HashMap<Long, List<RoleDto>>();
    	List<RoleDto> roleDtoList=new ArrayList<>();
    	
    	if(roleList.isEmpty()) {
    		roleGroupRoleRepository.updateRoleGroupRoleByGroupId(roleGroupId);
    	}else {
			for (String arrStr : roleList) {
				ProviderRole providerRole = providerRoleRepository.findByProviderRoleIdAndProvider(arrStr, PROVIDER).get();
				RoleDto roleDto = new RoleDto();
				Optional<Role> role = roleRepository.findByProviderRoleId(arrStr);
				if (role.isPresent()) {
					roleDto.setName(role.get().getName());
					roleDto.setId(role.get().getId());
				}
				roleDtoList.add(roleDto);
				
				items.put(roleGroupId, roleDtoList);
				Long roleId = providerRole.getRoleId();
				try {
					roleGroupService.assign(projection, roleGroupId, roleId, user, tenent, notInAuth0);
					//roleGroupService.updateRoleGroup(roleGroupId, roleId);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
    		
    	}	
    	return items;
    	
	} 
    
    /**
     * Assign Role to  RoleGroup
     *
     * @param tenent
     * @param roleGroupId
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/missing/role-group-bulk", produces = "application/json", consumes = "application/json")
    public @ResponseBody HashMap<Long, List<RoleDto>> addMissingRoleToGroupBulk(@PathVariable(value = "tenent") String tenent,
                                              @RequestParam(value = "projection",defaultValue = "defaultProjection") String projection,
                                              @PageableDefault(size = 100000) Pageable pageable,
                                              @RequestParam(value = "bookmarks", required = false) String bookmarks,
                                  			  @RequestParam(value = "searchq", required = false) String search,
                                              @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {
	
    	List<RoleGroupRole> roleGrList=roleGroupRoleRepository.findByAuth0();
    	HashMap<Long, List<RoleDto>> items = new HashMap<Long, List<RoleDto>>();
    	
    	String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
    	
    	// SET PAGE RECORD SIZE
		int pageLength = MAX_STATEMENT_PAGEEABLE_LENGTH;
		// CALCULATE PAGE COUNT (PAGE STARTS WITH ZERO INDEX)
		int pageCount = roleGrList.size() / pageLength;
		int pageRemainder = roleGrList.size() % pageLength;
		int originalPageCount = 0;
		if (pageRemainder > 0) 
			originalPageCount = pageCount + (pageRemainder - 1);
		
		loop_pageable:for (int i = 0; i <= originalPageCount; i++) {
			
			List<RoleGroupRole> roleGroupList=roleGroupRoleRepository.findByAuth0(PageRequest.of(i, pageLength));
			
			for(RoleGroupRole roleGroupRole:roleGroupList) {
	    		Long roleGroupId=roleGroupRole.getRoleGroup().getId();
	    		List<String> roleList=roleRepository.findByRoleGroupId(roleGroupId);
	        	
	        	List RoleListFromAuth0 = roleGroupService.getAllRoleFromAuth0(pageable, bookmarks, search,roleGroupId.toString(),tenent);
	        	
	        	ListIterator<LinkedHashMap> RoleFromAuth0ListIterator = RoleListFromAuth0.listIterator();
	        	
	        	while (RoleFromAuth0ListIterator.hasNext()) {
	    			LinkedHashMap roleFromAuth0 = RoleFromAuth0ListIterator.next();
	    			//if(roleList.contains(roleFromAuth0.get("_id")) && applicationId!=null && applicationId.equals(roleFromAuth0.get("applicationId"))) {
	    			if(roleList.contains(roleFromAuth0.get("_id"))) {
	    				roleList.remove(roleFromAuth0.get("_id"));
	    			}
	        	}
	        	boolean notInAuth0=true;
	        	List<RoleDto> roleDtoList=new ArrayList<>();
	        	
	        	if(roleList.isEmpty()) {
	        		roleGroupRoleRepository.updateRoleGroupRoleByGroupId(roleGroupId);
	        	}else {
					for (String arrStr : roleList) {
						ProviderRole providerRole = providerRoleRepository.findByProviderRoleIdAndProvider(arrStr, PROVIDER).get();
						RoleDto roleDto = new RoleDto();
						Optional<Role> role = roleRepository.findByProviderRoleId(arrStr);
						if (role.isPresent()) {
							roleDto.setName(role.get().getName());
							roleDto.setId(role.get().getId());
						}
						roleDtoList.add(roleDto);

						items.put(roleGroupId, roleDtoList);
						Long roleId = providerRole.getRoleId();
						try {
							roleGroupService.assign(projection, roleGroupId, roleId, user, tenent, notInAuth0);
							//roleGroupService.updateRoleGroup(roleGroupId, roleId);
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
	        	}
	        	
	    	}
		}
		return items;
    	
	} 
    
    /**
     * Missing RoleGroup
     *
     * @param tenent
     * @param projection
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @PutMapping(path = "/{tenent}/missing/group", produces = "application/json", consumes = "application/json")
    public @ResponseBody List<?> addMissingGroup(@PathVariable(value = "tenent") String tenent,
                                              @RequestParam(value = "projection",defaultValue = "defaultProjection") String projection,
                                              @PageableDefault(size = 100000) Pageable pageable,
                                              @RequestParam(value = "bookmarks", required = false) String bookmarks,
                                  			  @RequestParam(value = "searchq", required = false) String search,
                                              @RequestHeader(value = "username", defaultValue = "sysUser") String user
    ) throws FXDefaultException {
    	
    	String userName=null;
		if(LogginAuthentcation.getUserName()==null || LogginAuthentcation.getUserName().isEmpty()) { 
			userName=LogginAuthentcation.getUserName();
 		}
    	
    	String processCode="GRO";
		ProcessLog processLog=moduleService.checkProcessOn(processCode);
		
		ArrayList<String> missingGroupList=new ArrayList<>();
		
		if(processLog!=null && processLog.getProcess()!=null && processLog.getProcess().equals("1")) {		
			missingGroupList.add("Process already running..");
		}else {
			moduleService.insertProcessLog(processCode,userName!=null?userName:"sysUser");
			
			String applicationId=null;
	    	try {
				applicationId = permissionService.getApplicationIdByTenant(tenent);
			} catch (Exception e) {
				LoggerRequest.getInstance().logInfo("Exception "+e.toString());
			}
	    	
	    	
	    	List<String> groupList=roleGroupService.findByRoleGroup();
	    	
	    	List groupListFromAuth0 = roleGroupService.getAllGroupFromAuth0(pageable, bookmarks, search,null,tenent);
	    	
	    	ListIterator<LinkedHashMap> groupeFromAuth0ListIterator = groupListFromAuth0.listIterator();
	    	
	    	while (groupeFromAuth0ListIterator.hasNext()) {
	    		LinkedHashMap groupFromAuth0 = groupeFromAuth0ListIterator.next();
	    		//if(groupList.contains(groupFromAuth0.get("_id")) && applicationId!=null && applicationId.equals(groupFromAuth0.get("applicationId"))) {
				if(groupList.contains(groupFromAuth0.get("_id"))) {
					groupList.remove(groupFromAuth0.get("_id"));
				}
	    	}
	    	
	    	boolean notInAuth0=true;
	    	
	    	for(String providerGroupId:groupList) {
	    		Optional<RoleGroup> rolerGroup=roleGroupRepository.findByProviderGroupId(providerGroupId);
	    		
	    		if(rolerGroup.isPresent()) {
		    		AddRoleGroup addRoleGroup=new AddRoleGroup();
		    		addRoleGroup.setDescription(rolerGroup.get().getDescription()!=null?rolerGroup.get().getDescription():null);
		    		addRoleGroup.setName(rolerGroup.get().getName()!=null?rolerGroup.get().getName():null);
		    		addRoleGroup.setId(rolerGroup.get().getId()!=null?rolerGroup.get().getId():0L);
		    		
		        	Resource resource = new Resource(roleGroupService.create(projection, addRoleGroup, "SYSTEM", tenent,notInAuth0));
		        	
		        	missingGroupList.add(rolerGroup.get().getName()!=null?rolerGroup.get().getName():null);
	    		}
	    	}
			
	    	moduleService.deleteProcessLog(processCode);
		}
    	return missingGroupList;
	}

	// This method for testing purposes
	@GetMapping(path = "/{tenent}/rhsso-token", produces = "application/json")
	public ResponseEntity<?> getRhssoToken(@PathVariable(value = "tenent") String tenent) throws TokenException {
		String tokenResponse = tokenService.getRHSSOToken();
		
		return ResponseEntity.ok(tokenResponse);
	}
    
}
