package com.loits.comn.auth.services;

import java.util.List;

import javax.validation.Valid;

import org.springframework.stereotype.Service;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.dto.ClientScopes;
import com.loits.comn.auth.dto.MapScopeGroups;

@Service
public interface ClientScopesGroupService {

	public void mappingScopeToGruop(@Valid MapScopeGroups mapScopeGroups, String user, String tenent, boolean tenantUpdate) throws FXDefaultException;

	public ClientScopes getClientScopeByUser(String userId, String tenent,List<String> module);
	
 
}
