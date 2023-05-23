package com.loits.comn.auth.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.core.LogginAuthentcation;
import com.loits.comn.auth.domain.Auth0PermissionRole;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.ProcessLog;
import com.loits.comn.auth.domain.ProviderPermission;
import com.loits.comn.auth.domain.ProviderRole;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.domain.RolePermission;
import com.loits.comn.auth.domain.SuccessAndErrorDetailsResource;
import com.loits.comn.auth.dto.AppMetaData;
import com.loits.comn.auth.dto.Authorization;
import com.loits.comn.auth.repo.Auth0PermissionRoleRepository;
import com.loits.comn.auth.repo.ProviderPermissionRepository;
import com.loits.comn.auth.repo.ProviderRoleRepository;
import com.loits.comn.auth.repo.RolePermissionDumpRepository;
import com.loits.comn.auth.services.HttpService;
import com.loits.comn.auth.services.ModuleService;
import com.loits.comn.auth.services.PermissionService;
import com.loits.comn.auth.services.RolePermissionService;
import com.loits.comn.auth.services.RoleService;
import com.loits.comn.auth.services.UserService;
import com.querydsl.core.types.Predicate;
import java.util.ListIterator;
import java.util.Optional;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rolePermissionValidate/v1")
@SuppressWarnings("unchecked")
public class RolePermissionValidateController {

	 Logger logger = LogManager.getLogger(RolePermissionValidateController.class);
	 
	 @Autowired
	 PermissionService permissionService;
	 
	 @Autowired
	 HttpService httpService;
	 
	 @Autowired
	 RoleService roleService;
	 
	 @Autowired
	 RolePermissionService rolePermissionService;
	 
	 @Autowired
	 ProviderRoleRepository providerRoleRepository;
	 
	 @Autowired
	 ProviderPermissionRepository providerPermissionRepository;
	 
	 @Autowired
	 RolePermissionDumpRepository rolePermissionDumpRepository;
	 
	 @Autowired
	 Auth0PermissionRoleRepository auth0PermissionRoleRepository;
	 
	 @Autowired
	 ModuleService moduleService;
	 
	 @Autowired
	 UserService userService;
	 
	 @Value("${auth.provider}")
	 private String PROVIDER;
	 	 
	 /**
     * Get all Permissions from comn_auth table and auth0 side ,and return missing auth0 Permissions
     *
     * @param tenent
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     */
	@GetMapping(path = "/Permission/{tenent}", produces = "application/json")
	public @ResponseBody List<?> getPermissions(@PathVariable(value = "tenent") String tenent,
			@PageableDefault(size = 100000) Pageable pageable,
			@RequestParam(value = "bookmarks", required = false) String bookmarks,
			@RequestParam(value = "searchq", required = false) String search,
			@QuerydslPredicate(root = Permission.class) Predicate predicate,
			@RequestHeader(value = "username", defaultValue = "sysUser") String user,
			@RequestParam(name = "projection", defaultValue = "defaultProjection") String projection)
			throws FXDefaultException {
		
		String userName=null;
		if(LogginAuthentcation.getUserName()==null || LogginAuthentcation.getUserName().isEmpty()) { 
			userName=LogginAuthentcation.getUserName();
 		}else if(user!=null) {
 			userName=user;
 		}

		logger.debug(String.format("Loading Permission details.(Projection: %s | Tenent: %s )", projection, tenent));
		
		ArrayList<String> process=new ArrayList<>();
		String processCode="PER";
		ProcessLog processLog=moduleService.checkProcessOn(processCode);
		
		if(processLog!=null && processLog.getProcess()!=null && processLog.getProcess().equals("1")) {
			LoggerRequest.getInstance().logInfo("************************Permission sync Process already running..");
		}else {
			permissionService.syncPermission(userName,tenent,processCode);
		}
		process.add("OK");
		return process;
	}
  
    /**
     * Get all Roles from comn_auth table and auth0 side ,and return missing auth0 roles
     *
     * @param tenent
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/Role/{tenent}", produces = "application/json")
	public @ResponseBody List<?> getRoles(@PathVariable(value = "tenent") String tenent,
			@PageableDefault(size = 100000) Pageable pageable,
			@RequestParam(value = "bookmarks", required = false) String bookmarks,
			@RequestParam(value = "searchq", required = false) String search,
			@QuerydslPredicate(root = Role.class) Predicate predicate,
			@RequestHeader(value = "username", defaultValue = "sysUser") String user,
			@RequestParam(name = "projection", defaultValue = "defaultProjection") String projection)
			throws FXDefaultException {
    	
    	ArrayList<String> process=new ArrayList<>();
    	
    	String userName=null;
		if(LogginAuthentcation.getUserName()==null || LogginAuthentcation.getUserName().isEmpty()) { 
			userName=LogginAuthentcation.getUserName();
 		}else if(user!=null) {
 			userName=user;
 		}

		logger.debug(String.format("Loading Role details.(Projection: %s | Tenent: %s )", projection, tenent));
		
		String processCode="ROL";
		ProcessLog processLog=moduleService.checkProcessOn(processCode);
		
		if(processLog!=null && processLog.getProcess()!=null && processLog.getProcess().equals("1")) {		
			LoggerRequest.getInstance().logInfo("************************Role sync Process already running..");
		}else {	
			roleService.syncRole(userName,tenent,processCode);
		}
		process.add("OK");
		return process;

	}
    
    /**
     * Get map role from comn_auth table and auth0 side ,and return missing auth0 map
     *
     * @param tenent
     * @param roleId
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     */
    @GetMapping(path = "/role-permission-map/{tenent}/{roleId}", produces = "application/json")
	public @ResponseBody List<?> getPermissionByRole(@PathVariable(value = "tenent") String tenent,@PathVariable(value = "roleId") String roleId,
			@PageableDefault(size = 100000) Pageable pageable,
			@RequestParam(value = "bookmarks", required = false) String bookmarks,
			@RequestParam(value = "searchq", required = false) String search,
			@QuerydslPredicate(root = Role.class) Predicate predicate,
			@RequestHeader(value = "username", defaultValue = "sysUser") String user,
			@RequestParam(name = "projection", defaultValue = "defaultProjection") String projection)
			throws FXDefaultException {

		logger.debug(String.format("Loading Role details.(Projection: %s | Tenent: %s )", projection, tenent));

		Iterable<RolePermission> dbRolePermissionList = roleService.getPermissionByRoleId(Long.parseLong(roleId),tenent);

		List RoleListFromAuth0 = roleService.getAllRoleFromAuth0(pageable, bookmarks, search, projection, tenent);
		
		String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		
		ArrayList<String> missingPermissionList=new ArrayList<>();
		for(RolePermission rolePermission:dbRolePermissionList){

			boolean foundOnAuth0 = false;
			ListIterator<LinkedHashMap> RoleFromAuth0ListIterator = RoleListFromAuth0.listIterator();

			while (RoleFromAuth0ListIterator.hasNext()) {
				LinkedHashMap roleFromAuth0 = RoleFromAuth0ListIterator.next();
				if (roleFromAuth0 != null) {
					if (rolePermission.getRole()!=null && rolePermission.getRole().getName().equals(roleFromAuth0.get("name")) && applicationId!=null && applicationId.equals(roleFromAuth0.get("applicationId"))) {
						
						ArrayList<String> list=(ArrayList<String>) roleFromAuth0.get("permissions");
						
						String permissionId=rolePermission.getPermission()!=null?String.valueOf(rolePermission.getPermission().getId()):null;
						
						if(permissionId!=null) {
							String providerPermissionId=roleService.findByProviderPermissionId(Long.parseLong(permissionId));
						
							if (!(list.contains(providerPermissionId)) && applicationId!=null) {
								StringBuffer sb=new StringBuffer();
								sb.append(permissionId);
								sb.append(" / ");
								sb.append(rolePermission.getPermission().getName()!=null?rolePermission.getPermission().getName():"");
								missingPermissionList.add(sb.toString());
								
								roleService.assign(projection, Long.parseLong(roleId), Long.parseLong(permissionId), "sysUser", tenent, false,true);
								
								/*try {
									roleService.updateRolePermission(Long.parseLong(roleId), Long.parseLong(permissionId));
								} catch (Exception e) {
									LoggerRequest.getInstance().logInfo("Exception "+e.toString());
								}*/
							}
						}
						
				
					}
				}
			}
		}
		return missingPermissionList;

	}
    
    /**
     * return missing auth0 map
     *
     * @param tenent
     * @param pageable
     * @param bookmarks
     * @param projection
     * @return
     * 
     * This API need to use map existing role-permission table for missing permission(existing tenent).not need to check application same because we save data according to application using save-auth0-details API
     */
    @GetMapping(path = "/role-permission-map/{tenent}", produces = "application/json")
	public @ResponseBody List<?> autoAssignPermission(@PathVariable(value = "tenent") String tenent,
			@PageableDefault(size = 100000) Pageable pageable,
			@RequestParam(value = "bookmarks", required = false) String bookmarks,
			@RequestParam(value = "searchq", required = false) String search,
			@QuerydslPredicate(root = Role.class) Predicate predicate,
			@RequestHeader(value = "username", defaultValue = "sysUser") String user,
			@RequestParam(name = "projection", defaultValue = "defaultProjection") String projection)
			throws FXDefaultException {
    	
    	ArrayList<String> process=new ArrayList<>();
    	
    	String userName=null;
		if(LogginAuthentcation.getUserName()!=null && !LogginAuthentcation.getUserName().isEmpty()) { 
			userName=LogginAuthentcation.getUserName();
 		}else if(user!=null) {
 			userName=user;
 		}
		
		String processCode="MAP";
		ProcessLog processLog=moduleService.checkProcessOn(processCode);
		
		if(processLog!=null && processLog.getProcess()!=null && processLog.getProcess().equals("1")) {		
			LoggerRequest.getInstance().logInfo("************************Process already running..");
		}else {
			roleService.rolePermissionMapping(userName,tenent,processCode);
		}
		
		process.add("OK");
		
		return process;
	}
    
    /**
     *
     * @param tenent
     * @param processCode
     * @return
     */
    @GetMapping(path = "/process-run/{tenent}/{processCode}", produces = "application/json")
	public @ResponseBody List<?> getProcess(@PathVariable(value = "tenent") String tenent,@PathVariable(value = "processCode") String processCode)
			throws FXDefaultException {

		return moduleService.getProcessByProcessCode(tenent,processCode);

	}
    
    /**
     * Assign permission to  Role
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     * 
     * This API need to use map migrated role-permission table(when crate new tenent)
     */
   // This method for data migration when create new tenent
    @PutMapping(path = "/auto-add-permission/{tenent}", produces = "application/json")
    public @ResponseBody List<?> assignPermission(@PathVariable(value = "tenent") String tenent,@RequestHeader(value = "username", defaultValue = "sysUser") String user) throws FXDefaultException {
        return roleService.assignAuto("defaultProjection", user, tenent, false,true);
    }
    
    /**
     * Duplicate check  Role
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @GetMapping(path = "/duplicate-role/{tenent}", produces = "application/json")
    public @ResponseBody List<?> duplicateRole(@PathVariable(value = "tenent") String tenent,@RequestHeader(value = "username", defaultValue = "sysUser") String user) throws FXDefaultException {
    	
    	ArrayList<String> allRoleList=new ArrayList<>();
    	ArrayList<String> dupRoleList=new ArrayList<>();
    	
    	List RoleListFromAuth0 = roleService.getAllRoleFromAuth0(null, null, null, null, tenent);
    	
    	ListIterator<LinkedHashMap> RoleFromAuth0ListIterator = RoleListFromAuth0.listIterator();
    	
    	String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}

		while (RoleFromAuth0ListIterator.hasNext()) {
			LinkedHashMap roleFromAuth0 = RoleFromAuth0ListIterator.next();
			if (roleFromAuth0 != null) {
				if (roleFromAuth0.get("name")!=null && applicationId!=null && applicationId.equals(roleFromAuth0.get("applicationId"))) {
					if(allRoleList.contains(roleFromAuth0.get("name"))) {
						dupRoleList.add(((String) roleFromAuth0.get("_id")).trim());
	    			}else {
	    				allRoleList.add(((String) roleFromAuth0.get("name")).trim());
	    			}
				}
			}
		}
    	
        return dupRoleList;
    }
    
    /**
     * Duplicate check  Permission
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @GetMapping(path = "/duplicate-permission/{tenent}", produces = "application/json")
    public @ResponseBody List<?> duplicatePermission(@PathVariable(value = "tenent") String tenent,@RequestHeader(value = "username", defaultValue = "sysUser") String user) throws FXDefaultException {
    	
    	ArrayList<String> allPermissioneList=new ArrayList<>();
    	ArrayList<String> dupPermissionList=new ArrayList<>();
    	
    	List PermissionListFromAuth0 = permissionService.getAllPermissionFromAuth0(null, null, null,null, tenent);
    		
    	String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}

		ListIterator<LinkedHashMap> PermissionFromAuth0ListIterator = PermissionListFromAuth0.listIterator();

		while (PermissionFromAuth0ListIterator.hasNext()) {
			LinkedHashMap premissionFromAuth0 = PermissionFromAuth0ListIterator.next();

			if (premissionFromAuth0.get("name")!=null && applicationId!=null && applicationId.equals(premissionFromAuth0.get("applicationId"))) {
				if(allPermissioneList.contains(premissionFromAuth0.get("name"))) {
					dupPermissionList.add(((String) premissionFromAuth0.get("_id")).trim());
    			}else {
    				allPermissioneList.add(((String) premissionFromAuth0.get("name")).trim());
    			}
				
			}
		}
    	
        return dupPermissionList;
    }
    
    /**
     * Duplicate check  Role
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @GetMapping(path = "/wrong-roleId/{tenent}", produces = "application/json")
    public @ResponseBody List<?> wrongRole(@PathVariable(value = "tenent") String tenent,@RequestHeader(value = "username", defaultValue = "sysUser") String user) throws FXDefaultException {
    	
    	ArrayList<String> dupRoleList=new ArrayList<>();
    	
    	List RoleListFromAuth0 = roleService.getAllRoleFromAuth0(null, null, null, null, tenent);
    	
    	ListIterator<LinkedHashMap> RoleFromAuth0ListIterator = RoleListFromAuth0.listIterator();
    	
    	String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}

		while (RoleFromAuth0ListIterator.hasNext()) {
			LinkedHashMap roleFromAuth0 = RoleFromAuth0ListIterator.next();
			if (roleFromAuth0 != null) {
				if (roleFromAuth0.get("name")!=null && applicationId!=null && applicationId.equals(roleFromAuth0.get("applicationId"))) {
					
					Role role=roleService.getRoleByName(roleFromAuth0.get("name").toString());
					
					if(role!=null && role.getId()!=null) {
						Optional<ProviderRole> providerRoleOp =providerRoleRepository.findByRoleId(role.getId());
						
						if(providerRoleOp.isPresent() && roleFromAuth0.get("name")!=null && role!=null && role.getName()!=null && role.getName().equals(roleFromAuth0.get("name"))) {
							ProviderRole providerRole =providerRoleOp.get();
							if(!roleFromAuth0.get("_id").equals(providerRole.getProviderRoleId())) {
								dupRoleList.add("auth0 "+roleFromAuth0.get("_id").toString() +" auth0 module "+providerRole.getProviderRoleId());
								
								roleService.updateRoleByuid(roleFromAuth0.get("_id").toString(),providerRole.getProviderRoleId());
							}
							
						}
					}
					
				}
			}
		}
    	
        return dupRoleList;
    }
    
    /**
     * Duplicate check  Permission
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @GetMapping(path = "/wrong-permissionId/{tenent}", produces = "application/json")
    public @ResponseBody List<?> wrongPermission(@PathVariable(value = "tenent") String tenent,@RequestHeader(value = "username", defaultValue = "sysUser") String user) throws FXDefaultException {
    	

    	ArrayList<String> dupPermissionList=new ArrayList<>();
    	
    	List PermissionListFromAuth0 = permissionService.getAllPermissionFromAuth0(null, null, null,null, tenent);
    		
    	String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}

		ListIterator<LinkedHashMap> PermissionFromAuth0ListIterator = PermissionListFromAuth0.listIterator();

		while (PermissionFromAuth0ListIterator.hasNext()) {
			LinkedHashMap premissionFromAuth0 = PermissionFromAuth0ListIterator.next();

			if (premissionFromAuth0.get("name")!=null && applicationId!=null && applicationId.equals(premissionFromAuth0.get("applicationId"))) {
				
				Permission permission=null;
				try {
					permission = permissionService.getPermissionByName(premissionFromAuth0.get("name").toString());
					if(permission!=null && permission.getId()!=null) {
						Optional<ProviderPermission> providerPermissionOp =providerPermissionRepository.findByProviderPermissionIdClass_Permission(permission.getId());
						
						ProviderPermission providerPermission =providerPermissionOp.get();
						
						if((permission!=null && premissionFromAuth0.get("name").equals(permission.getName())) && !premissionFromAuth0.get("_id").equals(providerPermission.getProviderPermissionId())) {
							
							dupPermissionList.add("auth0 "+premissionFromAuth0.get("_id").toString()+" auth0 module "+providerPermission.getProviderPermissionId());
							
							permissionService.updatePermissionByuid(premissionFromAuth0.get("_id").toString(),providerPermission.getProviderPermissionId());
						}
					}
					
				} catch (Exception e) {
					LoggerRequest.getInstance().logInfo("Exception "+e.toString());
				}
				
			}
		}
    	
        return dupPermissionList;
    }
    
    /**
     * Missing Provider Permission
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     */
    
    //Very First Need to Remove Permission Duplication Before Execute This API
    @GetMapping(path = "/provider-permission-missing-list/{tenent}", produces = "application/json")
    public @ResponseBody List<?> missingProviderPermission(@PathVariable(value = "tenent") String tenent,@RequestHeader(value = "username", defaultValue = "sysUser") String user) throws FXDefaultException {
    	ArrayList<String> process=new ArrayList<>();
    	
    	String userName=null;
		if(LogginAuthentcation.getUserName()!=null && !LogginAuthentcation.getUserName().isEmpty()) { 
			userName=LogginAuthentcation.getUserName();
 		}else if(user!=null) {
 			userName=user;
 		}
		
		String processCode="PPM";
		ProcessLog processLog=moduleService.checkProcessOn(processCode);
		
		if(processLog!=null && processLog.getProcess()!=null && processLog.getProcess().equals("1")) {		
			LoggerRequest.getInstance().logInfo("************************Process already running..");
		}else {
			permissionService.missingProviderPermission(userName,tenent,processCode);
		}
		
		process.add("OK");
		
        return process;
    }
    
    /**
     * Missing Provider Role
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     */
    
    //Very First Need to Remove Role Duplication Before Execute This API
    @GetMapping(path = "/provider-role-missing-list/{tenent}", produces = "application/json")
    public @ResponseBody List<?> missingProviderRole(@PathVariable(value = "tenent") String tenent,@RequestHeader(value = "username", defaultValue = "sysUser") String user) throws FXDefaultException {
    	
    	ArrayList<String> process=new ArrayList<>();
    	
    	String userName=null;
		if(LogginAuthentcation.getUserName()!=null && !LogginAuthentcation.getUserName().isEmpty()) { 
			userName=LogginAuthentcation.getUserName();
 		}else if(user!=null) {
 			userName=user;
 		}
		
		String processCode="PRM";
		ProcessLog processLog=moduleService.checkProcessOn(processCode);
		
		if(processLog!=null && processLog.getProcess()!=null && processLog.getProcess().equals("1")) {		
			LoggerRequest.getInstance().logInfo("************************Process already running..");
		}else {
			roleService.missingProviderRole(userName,tenent,processCode);
		}
		
		process.add("OK");
    	
        return process;
    }
    
    /**
     * add auth0 mapping
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     * Saving data according to the application
     */
    @PostMapping(path = "/save-auth0-details/{tenent}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Object> addAuth0Mapping(@PathVariable(value = "tenent") String tenent,@RequestHeader(value = "username", defaultValue = "sysUser") String user) throws FXDefaultException { 	
		try {
			roleService.saveAuth0Mapping(tenent,user);
			
			SuccessAndErrorDetailsResource successAndErrorDetailsResource = new SuccessAndErrorDetailsResource();
			successAndErrorDetailsResource.setMessages("CREATED");
			return new ResponseEntity<>(successAndErrorDetailsResource, HttpStatus.CREATED);
			
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
			SuccessAndErrorDetailsResource successAndErrorDetailsResource = new SuccessAndErrorDetailsResource();
			successAndErrorDetailsResource.setMessages("BAD REQUEST");
			return new ResponseEntity<>(successAndErrorDetailsResource, HttpStatus.BAD_REQUEST);
		}

    }
    
    /**
     * delete auth0 mapping
     * @param tenent
     * @param user
     * @return
     * @throws FXDefaultException
     */
    @DeleteMapping(path = "/remove-auth0-details/{tenent}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Object> removeAuth0Mapping(@PathVariable(value = "tenent") String tenent,@RequestHeader(value = "username", defaultValue = "sysUser") String user) throws FXDefaultException { 	
		try {
			roleService.deleteAuth0Mapping(tenent,user);
			
			SuccessAndErrorDetailsResource successAndErrorDetailsResource = new SuccessAndErrorDetailsResource();
			successAndErrorDetailsResource.setMessages("DELETE");
			return new ResponseEntity<>(successAndErrorDetailsResource, HttpStatus.OK);
			
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
			SuccessAndErrorDetailsResource successAndErrorDetailsResource = new SuccessAndErrorDetailsResource();
			successAndErrorDetailsResource.setMessages("BAD REQUEST");
			return new ResponseEntity<>(successAndErrorDetailsResource, HttpStatus.BAD_REQUEST);
		}

    }
    
    
    @PostMapping(path = "/add-role-permission/{tenent}", produces = "application/json")
	public @ResponseBody List<?> addRolePermissionMapping(@PathVariable(value = "tenent") String tenent,
 			@PageableDefault(size = 100000) Pageable pageable,
 			@RequestParam(value = "bookmarks", required = false) String bookmarks,
 			@RequestParam(value = "searchq", required = false) String search,
 			@QuerydslPredicate(root = Role.class) Predicate predicate,
 			@RequestHeader(value = "username", defaultValue = "sysUser") String user,
 			@RequestParam(name = "projection", defaultValue = "defaultProjection") String projection)
 			throws FXDefaultException {

 		logger.debug(String.format("Loading Role details.(Projection: %s | Tenent: %s )", projection, tenent));
 		ArrayList<String> process=new ArrayList<>();

 		List RoleListFromAuth0 = roleService.getAllRoleFromAuth0(pageable, bookmarks, search, projection, tenent);
 		
 		String applicationId=null;
 		try {
 			applicationId = permissionService.getApplicationIdByTenant(tenent);
 		} catch (Exception e) {
 			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
 		}
 		
		ListIterator<LinkedHashMap> RoleFromAuth0ListIterator = RoleListFromAuth0.listIterator();
		
		auth0PermissionRoleRepository.deleteAll();
		
		while (RoleFromAuth0ListIterator.hasNext()) {
			LinkedHashMap roleFromAuth0 = RoleFromAuth0ListIterator.next();
			if (roleFromAuth0 != null) {
				if (applicationId.equals(roleFromAuth0.get("applicationId"))) {
					
					ArrayList<String> list=(ArrayList<String>) roleFromAuth0.get("permissions");

					for(String arr:list) {
						Auth0PermissionRole auth0=new Auth0PermissionRole();
						String permissionName=providerPermissionRepository.findByPermissionNameByProviderId(arr);
						
						auth0.setPermissionName(permissionName);
						auth0.setPermissionProvider(arr);
						auth0.setRoleName(roleFromAuth0.get("_id")!=null?roleFromAuth0.get("name").toString():null);
						auth0.setRoleProvider(roleFromAuth0.get("_id")!=null?roleFromAuth0.get("_id").toString():null);
						
						auth0PermissionRoleRepository.saveAndFlush(auth0);
					}
			
				}
			}
		}
		
		process.add("OK");
		
		return process;
 	}
    
    @GetMapping(path = "/{tenent}/user-details/{userId}", produces = "application/json")
 	public @ResponseBody Authorization getUserDetails(@PathVariable(value = "tenent") String tenent,@PathVariable(value = "userId") String userId,
  			@PageableDefault(size = 100000) Pageable pageable,
  			@RequestHeader(value = "username", required = false) String user,
  			@RequestParam(name = "projection", defaultValue = "defaultProjection") String projection)
  			throws FXDefaultException {

  		logger.debug(String.format("Loading Role details.(Projection: %s | Tenent: %s )", projection, tenent));
  		ArrayList<String> process=new ArrayList<>();

 		return userService.getAllUserDetails(pageable, projection, userId, null, tenent);
  	}
    
}