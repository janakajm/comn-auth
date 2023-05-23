package com.loits.comn.auth.services.impl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loits.comn.auth.commons.NullAwareBeanUtilsBean;
import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.core.RestTemplateResponseErrorHandler;
import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.dto.*;
import com.loits.comn.auth.mt.TenantHolder;
import com.loits.comn.auth.repo.*;
import com.loits.comn.auth.services.*;
import com.loits.comn.auth.services.projections.BasicRoleGroupProjection;
import com.loits.comn.auth.services.projections.RoleGroupProjectionWithRoles;
import com.loits.comn.auth.services.projections.RoleGroupRoleProjection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Transactional
public class RoleGroupServiceImpl implements RoleGroupService {

    Logger logger = LogManager.getLogger(RoleGroupServiceImpl.class);

    @Value("${auth.provider}")
    private String PROVIDER;
    
    @Value("${auth0.authorization.extension.api.url}")
    private String EXTENSION_API_URL;

    private static final String ROOT_PATH ="groups";

    @Autowired
    RoleGroupRepository roleGroupRepository;

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    HistoryService historyService;

    @Autowired
    HttpService httpService;

    @Autowired
    ProviderGroupRepository providerGroupRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    RoleGroupRoleRepository roleGroupRoleRepository;

    @Autowired
    ProviderRoleRepository providerRoleRepository;

    @Autowired
    RoleGroupUserRepository roleGroupUserRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    UserService userService;

    @Autowired
    Executor executor;
    
    @Autowired
    RestTemplateBuilder builder;
    
    @Autowired
    TokenService tokenService;

    @Override
    public Page<?> getAll(Pageable pageable, String bookmarks, String projection, String search, Predicate predicate, String tenent) throws FXDefaultException {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QRoleGroup qRoleGroup = QRoleGroup.roleGroup;

        //split and separate ids sent as a string
        if (!StringUtils.isEmpty(bookmarks)) {
            ArrayList<Long> ids = new ArrayList<>();
            for (String id : bookmarks.split(",")) {
                ids.add(Long.parseLong(id));
            }
            bb.and(qRoleGroup.id.in(ids));
        }

        //search by fields on demand
        if(search!=null && !search.isEmpty()){
            bb.and(qRoleGroup.name.containsIgnoreCase(search));
        }

        //update pageable object to ignorecase in sorting
        Sort sort = pageable.getSort();
        sort.forEach(order -> order.ignoreCase());
        PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return roleGroupRepository.findAll(bb.getValue(), pageRequest).map(
                roleGroup -> projectionFactory.createProjection(BasicRoleGroupProjection.class, roleGroup)
        );
    }

    @Override
    public Object getOne(String projection, String tenent, Long id) throws FXDefaultException {
        if(!roleGroupRepository.existsById(id)){
            throw new FXDefaultException("3001","INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        RoleGroup roleGroup = roleGroupRepository.findById(id).get();
        return projectionFactory.createProjection(RoleGroupProjectionWithRoles.class, roleGroup);
    }


    @Override
    public Object create(String projection, AddRoleGroup addRoleGroup, String user, String tenent,boolean notInAuth0) throws FXDefaultException {
        if(roleGroupRepository.findByNameContainingIgnoreCase(addRoleGroup.getName()).isPresent() && !notInAuth0){
            throw new FXDefaultException("3002","DUPLICATE", Translator.toLocale("DUPLICATE_ROLE_GROUP"), new Date(), HttpStatus.BAD_REQUEST);
        }
        
        RoleGroup roleGroup =null;
        if(notInAuth0) {
        	roleGroup=roleGroupRepository.findById(addRoleGroup.getId()).get();
        }else {
        	roleGroup = new RoleGroup();
        }

        //copy properties from input object to RoleGroup object
        NullAwareBeanUtilsBean nullAwareBeanUtilsBean =new NullAwareBeanUtilsBean();
        try {
            nullAwareBeanUtilsBean.copyProperties(roleGroup, addRoleGroup);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        roleGroup.setSearch(addRoleGroup.getName()+addRoleGroup.getDescription());
        //set created properties
        roleGroup.setCreatedBy(user);
        roleGroup.setCreatedOn(new Timestamp(new Date().getTime()));

        //temporary - for async operations
        roleGroup.setTenant(tenent);

        //save to db
        roleGroupRepository.save(roleGroup);

        //Async operations to create provider group & delegatable role
        asyncCreateOperations(roleGroup,HttpMethod.POST, ROOT_PATH, tenent);

        //save to history
        historyService.saveHistoryRecord(roleGroup, "CREATE");

        return projectionFactory.createProjection(BasicRoleGroupProjection.class, roleGroup);
    }

    @Override
    public Object update(String projection, Long id, UpdateRoleGroup updateRoleGroup, String user, String tenent) throws FXDefaultException {
        if(!roleGroupRepository.existsById(id)){
            throw new FXDefaultException("3001","INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        RoleGroup roleGroup = roleGroupRepository.findById(id).get();

        if(!updateRoleGroup.getVersion().equals(roleGroup.getVersion())){
            throw new FXDefaultException("3003", "VERSION_MISMATCH", Translator.toLocale("VERSION_MISMATCH"), new Date(), HttpStatus.BAD_REQUEST);
        }

        //Only description is updated
        roleGroup.setDescription(updateRoleGroup.getDescription());

        roleGroup.setSearch(roleGroup.getName()+updateRoleGroup.getDescription());

        //set created properties
        roleGroup.setCreatedBy(user);
        roleGroup.setCreatedOn(new Timestamp(new Date().getTime()));

        //temporary - for async operations
        roleGroup.setTenant(tenent);

        //save to db
        roleGroupRepository.save(roleGroup);

        //save to history
        historyService.saveHistoryRecord(roleGroup, "UPDATE");

        ProviderGroupIdClass providerGroupIdClass = new ProviderGroupIdClass();
        providerGroupIdClass.setGroup(id);
        providerGroupIdClass.setProvider("auth0");
        if(providerGroupRepository.existsById(providerGroupIdClass)) {
            ProviderGroup providerGroup =
                    providerGroupRepository.findById(providerGroupIdClass).get();

            String providerPermissionId1 =  providerGroup.getProviderGroupId();

            String subPath = ROOT_PATH + "/" + providerPermissionId1;
            //send async request to update auth0
            updateAuth0(roleGroup, HttpMethod.PUT, subPath, tenent);
        }


        return projectionFactory.createProjection(BasicRoleGroupProjection.class, roleGroup);
    }

    @Override
    public Object delete(String projection, Long id, String user, String tenent) throws FXDefaultException {
        if(!roleGroupRepository.existsById(id)){
            throw new FXDefaultException("3001","INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        RoleGroup roleGroup = roleGroupRepository.findById(id).get();

        if(roleGroupRoleRepository.existsByRoleGroupId(id)){
            throw new FXDefaultException("3001","INVALID_ATTEMPT", Translator.toLocale("ROLE_GROUP_FK"), new Date(), HttpStatus.BAD_REQUEST);
        }

        //temporary - for async operations
        roleGroup.setTenant(tenent);

        //save to db
        //roleGroupRepository.delete(roleGroup);

        //delete delegatable role
        if(roleRepository.existsByName(roleGroup.getName()+"-delegatable")){
            roleRepository.deleteByName(roleGroup.getName()+"-delegatable");
        }

        //save to history
        historyService.saveHistoryRecord(roleGroup, "DELETE");

        //delete in auth0
        ProviderGroupIdClass providerGroupIdClass = new ProviderGroupIdClass(id, "auth0");
        if(providerGroupRepository.existsById(providerGroupIdClass)) {
            ProviderGroup providerGroup =
                    providerGroupRepository.findById(providerGroupIdClass).get();

            String providerPermissionId1 = providerGroup.getProviderGroupId();

            String subPath = ROOT_PATH + "/" + providerPermissionId1;
            //updateAuth0(new RoleGroup(), HttpMethod.DELETE, subPath, tenent);
            //providerGroupRepository.deleteById(providerGroupIdClass);
            
            updateAuth0FroGroup(roleGroup, HttpMethod.DELETE, subPath, tenent);
        }

        return projectionFactory.createProjection(BasicRoleGroupProjection.class, roleGroup);
    }

    @Override
    public Object assign(String projection, Long roleGroupId, Long roleId, String user, String tenent,boolean notInAuth0) throws FXDefaultException {
        if(!roleGroupRepository.existsById(roleGroupId)){
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if(!roleRepository.existsById(roleId)){
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if(roleGroupRoleRepository.existsByRoleGroupIdAndRoleId(roleGroupId, roleId) && !notInAuth0){
            throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_GROUP_ROLE"), new Date(), HttpStatus.BAD_REQUEST);
        }

        RoleGroup roleGroup = roleGroupRepository.findById(roleGroupId).get();
        Role role = roleRepository.findById(roleId).get();
        
        RoleGroupRole roleGroupRole =null;
        if(notInAuth0) {
        	roleGroupRole=roleGroupRoleRepository.findByRoleGroupIdAndRoleId(roleGroupId, roleId).get();
        }else {
        	roleGroupRole = new RoleGroupRole();
        }
        
        //RoleGroupRole roleGroupRole = new RoleGroupRole();
        RoleGroupRoleId roleGroupRoleId = new RoleGroupRoleId();
        roleGroupRoleId.setRoleGroupId(roleGroupId);
        roleGroupRoleId.setRoleId(roleId);

        roleGroupRole.setRole(role);
        roleGroupRole.setRoleGroup(roleGroup);
        roleGroupRole.setRoleGroupRoleId(roleGroupRoleId);

        //set created log
        roleGroupRole.setCreatedBy(user);
        roleGroupRole.setCreatedOn(new Timestamp(new Date().getTime()));

        roleGroupRole = roleGroupRoleRepository.save(roleGroupRole);


        //temporary for async operations
        roleGroupRole.setTenant(tenent);

        historyService.saveHistoryRecord(roleGroupRole, "CREATE");

       //updateAuth0(roleGroupRole, HttpMethod.PATCH, ROOT_PATH, tenent, user);
        updateAuth0ForGruopMapping(roleGroupRole, HttpMethod.PATCH, ROOT_PATH, tenent, user);

        return projectionFactory.createProjection(RoleGroupRoleProjection.class, roleGroupRole);
    }

    @Override
    public Object remove(String projection, Long roleGroupId, Long roleId, String user, String tenent) throws FXDefaultException {
        if(!roleGroupRepository.existsById(roleGroupId)){
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_GROUP_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if(!roleRepository.existsById(roleId)){
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        RoleGroupRole roleGroupRole = roleGroupRoleRepository.findByRoleGroupIdAndRoleId(roleGroupId, roleId).get();

        //roleGroupRoleRepository.deleteById(roleGroupRole.getRoleGroupRoleId());

        //temporary for async operations
        roleGroupRole.setTenant(tenent);

        historyService.saveHistoryRecord(roleGroupRole, "DELETE");

        //updateAuth0(roleGroupRole, HttpMethod.DELETE, ROOT_PATH, tenent, user);
        updateAuth0ForGruopMapping(roleGroupRole, HttpMethod.DELETE, ROOT_PATH, tenent, user);

        return projectionFactory.createProjection(RoleGroupRoleProjection.class, roleGroupRole);

    }

    @Override
    public Page<?> getAllFromExtension(Pageable pageable, String bookmarks, String projection, String tenant) throws FXDefaultException {
        String dataType = ROOT_PATH;
        String subPath = dataType;

        //send get request for permissions and return page
        return httpService.sendProviderGetRequestAsPage(subPath, pageable, GroupResponse.class, null, dataType, GroupResponse.class);
    }

    @Async
    CompletableFuture<?> updateAuth0(RoleGroup roleGroup, HttpMethod httpMethod, String subpath, String tenant){
        return CompletableFuture.runAsync(()->{
            TenantHolder.setTenantId(tenant);

            GroupRequest groupRequest = null;
            ResponseEntity responseEntity = null;
            GroupResponse groupResponse = null;

            if(!httpMethod.equals(HttpMethod.DELETE)){
                groupRequest = new GroupRequest();
                NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
                try {
                    nullAwareBeanUtilsBean.copyProperties(groupRequest, roleGroup);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                groupRequest.setName(tenant+":"+groupRequest.getName());
            }

            //get Auth0 response
            try {
                responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, groupRequest, GroupResponse.class);

                ProviderGroupIdClass providerGroupIdClass = new ProviderGroupIdClass();
                providerGroupIdClass.setProvider(PROVIDER);
                providerGroupIdClass.setGroup(roleGroup.getId());

                if(httpMethod.equals(HttpMethod.POST)){
                    groupResponse = (GroupResponse) responseEntity.getBody();
                    ProviderGroup providerGroup = new ProviderGroup();
                    providerGroup.setProviderGroupIdClass(providerGroupIdClass);
                    providerGroup.setProviderGroupId(groupResponse.get_id());
                    providerGroupRepository.save(providerGroup);
                }
                if (httpMethod.equals(HttpMethod.DELETE) && responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==204) {
                	deleteByGroupById(roleGroup.getId());
                }

            } catch (Exception e) {
                logger.debug("Permission with id "+roleGroup.getId()+" was unable to be updated in "+PROVIDER);
                e.printStackTrace();

            }
            TenantHolder.clear();
        });
    }
    
    @Async
    void updateAuth0FroGroup(RoleGroup roleGroup, HttpMethod httpMethod, String subpath, String tenant){
            TenantHolder.setTenantId(tenant);

            GroupRequest groupRequest = null;
            ResponseEntity responseEntity = null;
            GroupResponse groupResponse = null;

            if(!httpMethod.equals(HttpMethod.DELETE)){
                groupRequest = new GroupRequest();
                NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
                try {
                    nullAwareBeanUtilsBean.copyProperties(groupRequest, roleGroup);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                groupRequest.setName(tenant+":"+groupRequest.getName());
            }
            //get Auth0 response
            try {
                responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, groupRequest, GroupResponse.class);

                ProviderGroupIdClass providerGroupIdClass = new ProviderGroupIdClass();
                providerGroupIdClass.setProvider(PROVIDER);
                providerGroupIdClass.setGroup(roleGroup.getId());

                if(httpMethod.equals(HttpMethod.POST)){
                    groupResponse = (GroupResponse) responseEntity.getBody();
                    ProviderGroup providerGroup = new ProviderGroup();
                    providerGroup.setProviderGroupIdClass(providerGroupIdClass);
                    providerGroup.setProviderGroupId(groupResponse.get_id());
                    providerGroupRepository.save(providerGroup);
                }
                if (httpMethod.equals(HttpMethod.DELETE) && responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==204) {
                	deleteByGroupById(roleGroup.getId());
                }

            } catch (Exception e) {
                logger.debug("Permission with id "+roleGroup.getId()+" was unable to be updated in "+PROVIDER);
                e.printStackTrace();

            }
            TenantHolder.clear();
    }

    private void deleteByGroupById(Long id) {
    	Optional<RoleGroup> rolegroup=roleGroupRepository.findById(id);
    	if(rolegroup.isPresent()) {
    		RoleGroup roleGroup=rolegroup.get();
    		roleGroup.setDeleted("Yes");
    		roleGroupRepository.save(roleGroup);
    	}
	}

	@Async
    CompletableFuture<?> asyncCreateOperations(RoleGroup roleGroup, HttpMethod httpMethod, String rootPath, String tenant) {
        return CompletableFuture.runAsync(() -> {

            //Create group in provider
            updateAuth0(roleGroup, httpMethod, rootPath, tenant);


        });
    }


    @Async
    CompletableFuture<?> updateUsersWithRole(RoleGroupRole roleGroupRole, HttpMethod httpMethod, String tenant, String user) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenant);

            List<UserRole> userRoleList = new ArrayList<>();
            List<RoleGroupUser> roleGroupUserList = roleGroupUserRepository.findByRoleGroup(roleGroupRole.getRoleGroup());
            if(httpMethod==HttpMethod.PATCH) {
                for (RoleGroupUser roleGroupUser : roleGroupUserList) {
                    UserProfile userProfile = roleGroupUser.getUser();
                    UserRole userRole = new UserRole();
                    UserRoleId userRoleId = new UserRoleId();

                    //basic properties
                    userRole.setUserRoleId(userRoleId);
                    userRoleId.setRoleId(roleGroupRole.getRole().getId());
                    userRoleId.setUserId(userProfile.getId());
                    userRoleId.setRoleGroupId(roleGroupRole.getRoleGroup().getId());
                    userRole.setRole(roleGroupRole.getRole());
                    userRole.setUser(userProfile);
                    userRole.setRoleGroup(roleGroupRole.getRoleGroup());

                    //expiretime and delegability
                    userRole.setExpires(roleGroupUser.getExpires());
                    userRole.setDelegatable(roleGroupUser.getDelegatable());

                    //audit log
                    userRole.setCreatedBy(user);
                    userRole.setCreatedOn(new Timestamp(new Date().getTime()));

                    //TODO status?
                    userRole.setStatus((byte) 1);
                    userRoleRepository.save(userRole);
                    HashSet<Role> roleHashSet = new HashSet<>();
                    roleHashSet.add(roleGroupRole.getRole());
                    if(roleHashSet.size() !=0) {
                        userService.updateAuthUser(userProfile.getId(), roleHashSet, httpMethod, tenant);
                    }

                }
            }else if(httpMethod==HttpMethod.DELETE){
                for (RoleGroupUser roleGroupUser : roleGroupUserList) {
                    UserProfile userProfile = roleGroupUser.getUser();
                    if(userRoleRepository.existsByUserAndRoleAndRoleGroup(userProfile, roleGroupRole.getRole(), roleGroupRole.getRoleGroup())) {
                        UserRole userRole =
                                userRoleRepository.findByUserAndRoleAndRoleGroup(userProfile, roleGroupRole.getRole(), roleGroupRole.getRoleGroup()).get();
                        userRoleRepository.delete(userRole);
                        HashSet<Role> roleHashSet = new HashSet<>();
                        roleHashSet.add(roleGroupRole.getRole());
                        if(roleHashSet.size() != 0){
                            userService.updateAuthUser(userProfile.getId(), roleHashSet, httpMethod, tenant);
                        }
                    }
                }
            }

            TenantHolder.clear();
        });
    }


    @Async
    CompletableFuture<?> updateAuth0(RoleGroupRole roleGroupRole, HttpMethod httpMethod, String rootPath, String tenant, String user){
        return CompletableFuture.runAsync(()->{
            TenantHolder.setTenantId(tenant);

            Role role =roleGroupRole.getRole();
            RoleGroup roleGroup = roleGroupRole.getRoleGroup();

            String roleId=null;
            if(providerRoleRepository.existsByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(role.getId(), PROVIDER)){
                ProviderRole providerRole = providerRoleRepository.findByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(role.getId(), PROVIDER).get();
                roleId = providerRole.getProviderRoleId();
            }

            String groupId =null;
            if(providerGroupRepository.existsByProviderGroupIdClass_Group(roleGroup.getId())){
                ProviderGroup providerGroup = providerGroupRepository.findByProviderGroupIdClass_Group(roleGroup.getId()).get();
                groupId = providerGroup.getProviderGroupId();
            }

            String subpath = rootPath+"/"+groupId+"/roles";

            List<String> rolesList = new ArrayList<>();
            rolesList.add(roleId);

            //get Auth0 response
            try {
            	ResponseEntity<GroupResponse> responseEntity=httpService.sendProviderRestRequest(httpMethod, subpath, rolesList, GroupResponse.class);
            	
            	if(httpMethod.equals(HttpMethod.PATCH) && responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==200) {
            		updateRoleGroup(roleGroup.getId(), Long.parseLong(roleId));
            	}
            	if(httpMethod.equals(HttpMethod.DELETE) && responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==200) {
            		deleteRoleGroup(roleGroup.getId(), Long.parseLong(roleId));
            	}

            } catch (Exception e) {
                logger.debug("Unable to update roles of RoleGroup "+role.getId()+" in "+PROVIDER);
                e.printStackTrace();

            }

            updateUsersWithRole(roleGroupRole, httpMethod, tenant, user);

            TenantHolder.clear();
        }, executor);
    }
    
	void updateAuth0ForGruopMapping(RoleGroupRole roleGroupRole, HttpMethod httpMethod, String rootPath, String tenant,String user) {

		TenantHolder.setTenantId(tenant);

		Role role =roleGroupRole.getRole();
        RoleGroup roleGroup = roleGroupRole.getRoleGroup();

        String roleId=null;
        if(providerRoleRepository.existsByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(role.getId(), PROVIDER)){
            ProviderRole providerRole = providerRoleRepository.findByProviderRoleIdClass_RoleAndProviderRoleIdClass_Provider(role.getId(), PROVIDER).get();
            roleId = providerRole.getProviderRoleId();
        }

        String groupId =null;
        if(providerGroupRepository.existsByProviderGroupIdClass_Group(roleGroup.getId())){
            ProviderGroup providerGroup = providerGroupRepository.findByProviderGroupIdClass_Group(roleGroup.getId()).get();
            groupId = providerGroup.getProviderGroupId();
        }

        String subpath = rootPath+"/"+groupId+"/roles";

        List<String> rolesList = new ArrayList<>();
        rolesList.add(roleId);

		// get Auth0 response
		try {
			ResponseEntity<GroupResponse> responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath,
					rolesList, GroupResponse.class);

			if (httpMethod.equals(HttpMethod.PATCH) && responseEntity != null && responseEntity.getStatusCode() != null
					&& responseEntity.getStatusCodeValue() == 200) {
				updateRoleGroup(roleGroup.getId(), role.getId());
			}
			if (httpMethod.equals(HttpMethod.DELETE) && responseEntity != null && responseEntity.getStatusCode() != null
					&& responseEntity.getStatusCodeValue() == 204) {
				deleteRoleGroup(roleGroup.getId(), role.getId());
			}

		} catch (Exception e) {
			logger.debug("Unable to update roles of RoleGroup " + role.getId() + " in " + PROVIDER);
			e.printStackTrace();

		}

		updateUsersWithRole(roleGroupRole, httpMethod, tenant, user);

		TenantHolder.clear();

	}
    
    private void deleteRoleGroup(Long roleGroupId, Long roleId) {
		roleGroupRoleRepository.deleteRoleGroupRole(roleGroupId,roleId);
		
	}

	@Override
	public List<?> getAllRoleFromAuth0(Pageable pageable, String bookmarks, String search,
			String groupId, String tenent) throws FXDefaultException {
		String dataType = ROOT_PATH;
		String subPath = dataType;
		
		if(groupId!=null) {
			 ProviderGroupIdClass providerGroupIdClass = new ProviderGroupIdClass();
			 providerGroupIdClass.setGroup(Long.parseLong(groupId));
			 providerGroupIdClass.setProvider(PROVIDER);
		        if (providerGroupRepository.existsById(providerGroupIdClass)) {
		            ProviderGroup providerGroup =providerGroupRepository.findById(providerGroupIdClass).get();
		            
		            subPath=subPath+"/"+providerGroup.getProviderGroupId()+"/roles";
		        }
		}

		// send get request for role and return page
		return httpService.sendProviderGetRequest(subPath, pageable, GroupRoleResponse.class,groupId, dataType, GroupRoleResponse.class);
	}

	@Override
	public void updateRoleGroup(Long roleGroupId, Long roleId) throws FXDefaultException {
		Optional<RoleGroupRole> roleGroup=roleGroupRoleRepository.findByRoleGroupIdAndRoleId(roleGroupId, roleId);
		if(roleGroup.isPresent() && roleGroup.get().getAuth0()==null) {
			RoleGroupRole roleGroupRole=roleGroup.get();
			roleGroupRole.setAuth0("Yes");
			roleGroupRoleRepository.save(roleGroupRole);
		}
		
	}

	@Override
	public List<String> findByRoleGroup() {
		return roleGroupRepository.findByRoleGroup();
	}

	@Override
	public List<?> getAllGroupFromAuth0(Pageable pageable, String bookmarks, String projection, String groupId,String tenent) throws FXDefaultException {
		String dataType = ROOT_PATH;
		String subPath = dataType;
		// send get request for role and return page
		return httpService.sendProviderGetRequest(subPath, pageable, GroupRoleResponse.class,groupId, dataType, GroupRoleResponse.class);
	}

	@Override
	public List<ProviderGroup> getAllDeletedGroup() throws FXDefaultException {
		List<ProviderGroup> allgroup = roleGroupRepository.findByDeleted();
		return allgroup;
	}

	@Override
	public void deleteGroup(Long groupId) throws FXDefaultException {
		try {
			 ProviderGroupIdClass providerGroupIdClass = new ProviderGroupIdClass(groupId, PROVIDER);
			 providerGroupRepository.deleteById(providerGroupIdClass);
			 
			 roleGroupRepository.deleteById(groupId);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception_DELETE "+e.toString());
		}
		
	}
    
}


