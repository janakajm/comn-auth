package com.loits.comn.auth.services.impl;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.loits.comn.auth.repo.custom.UserPermissionRepositoryCustom;
import com.loits.comn.auth.utility.PermissionSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.PermissionGroupId;
import com.loits.comn.auth.domain.PermissionGroups;
import com.loits.comn.auth.domain.UserProfile;
import com.loits.comn.auth.dto.ClientScopes;
import com.loits.comn.auth.dto.MapScopeGroups;
import com.loits.comn.auth.repo.PermissionGroupsRepository;
import com.loits.comn.auth.repo.PermissionRepository;
import com.loits.comn.auth.repo.ProviderGroupRepository;
import com.loits.comn.auth.repo.ProviderPermissionRepository;
import com.loits.comn.auth.repo.RoleGroupRepository;
import com.loits.comn.auth.repo.RoleGroupUserRepository;
import com.loits.comn.auth.repo.UserProfileRepository;
import com.loits.comn.auth.services.ClientScopesGroupService;

@Component
@Transactional(rollbackFor=Exception.class)
public class ClientScopesGroupServiceImpl implements ClientScopesGroupService {

    Logger logger = LogManager.getLogger(ClientScopesGroupServiceImpl.class);
    
    @Autowired
    ProviderPermissionRepository providerPermissionRepository;
    
    @Autowired
    PermissionRepository permissionRepository;
    @Autowired
	UserPermissionRepositoryCustom userPermissionRepositoryCustom;
    
    @Autowired
    RoleGroupRepository roleGroupRepository;
    
    @Autowired
    ProviderGroupRepository providerGroupRepository;
    
    @Autowired
    UserProfileRepository userProfileRepository;
    
    @Autowired
    RoleGroupUserRepository roleGroupUserRepository;
    
    @Autowired
    PermissionGroupsRepository permissionGroupsRepository;

	@Override
	public void mappingScopeToGruop(@Valid MapScopeGroups mapScopeGroups, String user, String tenent,boolean tenantUpdate) throws FXDefaultException {
		if (!providerPermissionRepository.findByProviderPermissionId(mapScopeGroups.getClientScopeId()).isPresent()) {
			throw new FXDefaultException("3002", "NOT_EXIST", Translator.toLocale("NOT_EXIST"), new Date(), HttpStatus.BAD_REQUEST);
        }
		
		//when remove group then that group need to remove from the mapping scope also.
		
		Long permissionId =providerPermissionRepository.findByProviderPermissionProviderPermissionIdLong(mapScopeGroups.getClientScopeId());
		
		if(mapScopeGroups!=null && mapScopeGroups.getHttpMethod()!=null && mapScopeGroups.getHttpMethod().equalsIgnoreCase("PUT")) {
			List<Long> roleGroupUserList =roleGroupUserRepository.findRoleGroupUser();
			
			for(Long groupId:roleGroupUserList) {
		        PermissionGroups permissionGroups =new PermissionGroups();
	
		        PermissionGroupId permissionGroupId = new PermissionGroupId();
		        permissionGroupId.setGroupId(groupId);
		       
		        permissionGroupId.setPermissionId(permissionId);
		        
		        permissionGroups.setPermissionGroupId(permissionGroupId);
		        permissionGroups.setCreatedBy(user);
		        permissionGroups.setCreatedOn(new Timestamp(new Date().getTime()));
		        permissionGroups.setPermission(permissionRepository.findById(permissionId).get());
		        permissionGroups.setRoleGroup(roleGroupRepository.findById(groupId).get());
	
		        permissionGroupsRepository.save(permissionGroups);
			}
		}if(mapScopeGroups!=null && mapScopeGroups.getHttpMethod()!=null && mapScopeGroups.getHttpMethod().equalsIgnoreCase("DELETE")) {
			permissionGroupsRepository.deletePermissionGroups(permissionId);
		}
		
	}

	@Override
	public ClientScopes getClientScopeByUser(String userId, String tenent,List<String> module) {

		ClientScopes clientScopes = new ClientScopes();
		List<String> scopes = new ArrayList<String>();

		Specification<Permission> permissionSpecification = null;
		for (String word : module) {
			Specification<Permission> wordSpecification = PermissionSpecification.nameContains(word);
			if (permissionSpecification == null) {
				permissionSpecification = wordSpecification;
			} else {
				permissionSpecification = permissionSpecification.or(wordSpecification);
			}
		}


		Specification<Permission> userSpecification = PermissionSpecification.hasPermissionWithUserId(userId);
		assert permissionSpecification != null;
		Specification<Permission> specification = permissionSpecification.and(userSpecification);

		scopes = userPermissionRepositoryCustom.findAll(specification).stream().map(Permission::getName).distinct().
				collect(Collectors.toList());
		clientScopes.setScopes(scopes);
		return clientScopes;
	}

}
