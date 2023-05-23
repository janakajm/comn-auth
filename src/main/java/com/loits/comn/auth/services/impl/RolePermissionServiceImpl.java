package com.loits.comn.auth.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.ProviderRole;
import com.loits.comn.auth.domain.ProviderRoleIdClass;
import com.loits.comn.auth.domain.RolePermission;
import com.loits.comn.auth.dto.RoleResponse;
import com.loits.comn.auth.repo.ProviderRoleRepository;
import com.loits.comn.auth.repo.RolePermissionRepository;
import com.loits.comn.auth.services.HttpService;
import com.loits.comn.auth.services.RolePermissionService;

@Component
@Transactional(rollbackFor = Exception.class)
public class RolePermissionServiceImpl implements RolePermissionService{
	
	@Autowired
    RolePermissionRepository rolePermissionRepository;
	
	@Autowired
    HttpService httpService;
	
	@Autowired
	ProviderRoleRepository providerRoleRepository;
	
	private static final String ROOT_PATH = "roles";
	
	@Value("${auth.provider}")
    private String PROVIDER;

	@Override
	public Object delete(String projection, String userId, String roleId, String user, String tenent) {
		return null;
	}

	@Override
	public Object update(String projection, String userId, String roleId, String user, String tenent) {
		return null;
	}

	@Override
	public List<RolePermission> findForAuth0ISNull() {
		return rolePermissionRepository.findForAuth0ISNull();
	}

	@Override
	public List<?> getAllPermissionFromAuth0(Pageable pageable, String bookmarks, String roleId,String tenent) throws FXDefaultException {
		String dataType = ROOT_PATH;
		String subPath = dataType;
		subPath = subPath;
		
		if(roleId!=null) {
			 ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
			 providerRoleIdClass.setRole(Long.parseLong(roleId));
			 providerRoleIdClass.setProvider(PROVIDER);
		        if (providerRoleRepository.existsById(providerRoleIdClass)) {
		            ProviderRole providerRole =providerRoleRepository.findById(providerRoleIdClass).get();
		            
		            subPath=subPath+"/"+providerRole.getProviderRoleId();
		        }
		}

		// send get request for permissions and return page
		return httpService.sendProviderGetRequest(subPath, pageable, RoleResponse.class,
				roleId, dataType, RoleResponse.class);
	}
	

}
