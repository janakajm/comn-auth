package com.loits.comn.auth.controller;

import com.loits.comn.auth.commons.UserNotFound;
import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LogginAuthentcation;
import com.loits.comn.auth.domain.SuccessAndErrorDetailsResource;
import com.loits.comn.auth.dto.ClientScopes;
import com.loits.comn.auth.dto.MapScopeGroups;
import com.loits.comn.auth.services.ClientScopesGroupService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import javax.validation.Valid;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/scopes-group/v1")
@SuppressWarnings("unchecked")
public class ClientScopesGroupController {

	private String userNotFound="User Name cannot be found!";
	
    Logger logger = LogManager.getLogger(ClientScopesGroupController.class);

    @Autowired
    ClientScopesGroupService clientScopesGroupService;
    
    @Autowired
	private Environment environment;

    @PutMapping(path = "/{tenent}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> mappingScopeToGruop(@PathVariable(value = "tenent") String tenent,
                                           @RequestBody @Valid MapScopeGroups mapScopeGroups,
                                           @RequestHeader(value = "username", defaultValue = "sysUser") String user,
                                           @RequestParam(name = "tenantUpdate", defaultValue = "true") boolean tenantUpdate) throws FXDefaultException{
    	
    	if(LogginAuthentcation.getUserName()==null || LogginAuthentcation.getUserName().isEmpty()) { 
 			throw new FXDefaultException("3002", "NOT_EXIST", userNotFound, new Date(), HttpStatus.BAD_REQUEST);
 		}
    	
    	clientScopesGroupService.mappingScopeToGruop(mapScopeGroups, user, tenent, tenantUpdate);
      
    	SuccessAndErrorDetailsResource successAndErrorDetailsResource=new SuccessAndErrorDetailsResource(); 
    	successAndErrorDetailsResource = new SuccessAndErrorDetailsResource("CREATED");
		return new ResponseEntity<>(successAndErrorDetailsResource,HttpStatus.OK);

    }
    
    @GetMapping(path = "/{tenent}/{userId}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> clientScopebyUser(@PathVariable(value = "tenent") String tenent,@PathVariable(value = "userId") String userId,
    		@RequestParam(value = "module", required = true) List<String> module) throws FXDefaultException{
    	
    	if(LogginAuthentcation.getUserName()==null || LogginAuthentcation.getUserName().isEmpty()) { 
    		throw new FXDefaultException("3002", "NOT_EXIST", userNotFound, new Date(), HttpStatus.BAD_REQUEST);
 		}
    	
    	ClientScopes clientScopes=clientScopesGroupService.getClientScopeByUser(userId,tenent,module);
      
    	return new ResponseEntity<>(clientScopes,HttpStatus.OK);

    }

}