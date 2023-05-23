package com.loits.comn.auth.services.impl;


import com.loits.comn.auth.commons.AsyncTaskDef;
import com.loits.comn.auth.commons.NullAwareBeanUtilsBean;
import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.dto.*;
import com.loits.comn.auth.helper.BulkUploadResponse;
import com.loits.comn.auth.mt.TenantHolder;
import com.loits.comn.auth.repo.*;
import com.loits.comn.auth.services.*;
import com.loits.comn.auth.services.projections.BasicRoleProjection;
import com.loits.comn.auth.services.projections.RolePermissionProjection;
import com.loits.comn.auth.services.projections.RoleProjectionWithPermissions;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class RoleServiceImpl implements RoleService {

    Logger logger = LogManager.getLogger(RoleServiceImpl.class);

    @Value("${auth.provider}")
    private String PROVIDER;

    private static final String ROOT_PATH = "roles";

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    HttpService httpService;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    HistoryService historyService;
    
    @Autowired
    PermissionService permissionService;

    @Autowired
    ProviderRoleRepository providerRoleRepository;

    @Autowired
    ProviderPermissionRepository providerPermissionRepository;

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    RolePermissionRepository rolePermissionRepository;

    @Autowired
    Executor executor;

    @Autowired
    AsyncTaskService asyncTaskService;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    RoleGroupRoleRepository roleGroupRoleRepository;
    
    @Autowired
    RolePermissionDumpRepository rolePermissionDumpRepository;
    
    @Autowired
	RolePermissionService rolePermissionService;
    
    @Autowired
    ModuleService moduleService;

    @Override
    public Page<?> getAll(Pageable pageable, String search, String bookmarks, Predicate predicate, String projection) {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QRole role = QRole.role;

        //split and separate ids sent as a string
        if (!StringUtils.isEmpty(bookmarks)) {
            ArrayList<Long> ids = new ArrayList<>();
            for (String id : bookmarks.split(",")) {
                ids.add(Long.parseLong(id));
            }
            bb.and(role.id.in(ids));
        }

        //filter roles with comn module
        bb.and(role.name.toLowerCase().notLike("comn:branch:%"));

        //search by name on demand
        if (search != null && !search.isEmpty()) {
            bb.and(role.name.containsIgnoreCase(search));
        }

        //update pageable object to ignorecase in sorting
        Sort sort = pageable.getSort();
        sort.forEach(order -> order.ignoreCase());
        PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return roleRepository.findAll(bb.getValue(), pageRequest).map(
                permission1 -> projectionFactory.createProjection(BasicRoleProjection.class, permission1)
        );
    }

    /**
     * Not used by any endpoint
     */
    @Override
    public Page<?> getAllFromExtension(Pageable pageable, String bookmarks, String projection, String id) throws FXDefaultException {
        String dataType = ROOT_PATH;
        String subPath = dataType;
        subPath = id != null ? dataType + "/" + id : subPath;

        //send get request for roles and return page
        return httpService.sendProviderGetRequestAsPage(subPath, pageable, RoleResponse.class, id, dataType, RoleProjectionWithPermissions.class);
    }

    @Override
    public Object getOne(String projection, String tenent, Long id) throws FXDefaultException {
        if (!roleRepository.existsById(id)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Role role = roleRepository.findById(id).get();
        return projectionFactory.createProjection(RoleProjectionWithPermissions.class, role);
    }

    @Override
    public Object create(String projection, AddRole addRole, String user, String tenent, boolean tenantUpdate, boolean authUpdate,boolean notInAuth0) throws FXDefaultException {
        if (roleRepository.findByNameContainingIgnoreCase(addRole.getName()).isPresent() && !notInAuth0) {
            throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_ROLE"), new Date(), HttpStatus.BAD_REQUEST);
        }
        
        Role role=null;
        if(notInAuth0) {
        	role=roleRepository.findByName(addRole.getName()).get();
        }else {
        	role=new Role();
        }
        
        //copy properties from dto to domain object
        role.setName(addRole.getName());
        role.setDescription(addRole.getDescription());
        role.setSearch(addRole.getName() + addRole.getDescription());
        System.out.println("=====================Create 1");
        role.setCreatedBy(user);
        role.setCreatedOn(new Timestamp(new Date().getTime()));
        
        //TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenent, PROVIDER);
        //role.setApplicationId(tenantAuthProvider.getClientId());
        //role.setTenantId(tenent);

        //save permission
        role = roleRepository.save(role);
        
        /*====Amal=========================================================*/
        if (addRole.getPermissions()!=null) {
        	
        	 System.out.println("=====================Create role Permission");
        	 
	        List<RolePermission> rolePermissionList = new ArrayList<>();
	        for (String permisionId : addRole.getPermissions()) {
	        	RolePermission rolePermission = saveRolePermission(role.getId(), Long.parseLong(permisionId), user,tenent,tenantUpdate); 
	            rolePermissionList.add(rolePermission);
	        }
	        role.setRolePermissions(rolePermissionList);
        }
        /*=====End==================================================*/

        //temporary - for async operations
        role.setTenant(tenent);

        System.out.println("=======================Updating tenants");
        //send request to update auth0 and other tenants
        //////////asyncUpdateOperations(role, null, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, "crud", authUpdate);
        
        updateOperations(role, null, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, "crud", authUpdate);/*amal*/
        
        System.out.println("=========================DONE UPDATING PROCESS");
        //save to history
        historyService.saveHistoryRecord(role, "CREATE");
        System.out.println("END OF REQUEST");
        return projectionFactory.createProjection(BasicRoleProjection.class, role);
    }

    @Override
    public Object bulkCreate(String projection, List<AddRole> addRoleList, String user, String tenent, boolean tenantUpdate, boolean authUpdate,boolean notInAuth0) {
        logger.debug("Bulk Role creation process started");
        BulkUploadResponse response = new BulkUploadResponse();
        List<Role> roleList = new ArrayList<>();
        List<BulkUploadResponse.Error> errorData = new ArrayList<>();

        HashSet<String> nameSet = new HashSet<>();

        addRoleList.forEach(addRole -> {
            if (!nameSet.contains(addRole.getName())) {
                nameSet.add(addRole.getName());

                try {
                    if (roleRepository.findByNameContainingIgnoreCase(addRole.getName()).isPresent()) {
                        throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_ROLE"), new Date(), HttpStatus.BAD_REQUEST);
                    }
                    //copy properties from dto to domain object
                    Role role = new Role();
                    role.setName(addRole.getName());
                    role.setDescription(addRole.getDescription());
                    role.setSearch(addRole.getName() + addRole.getDescription());

                    role.setCreatedBy(user);
                    role.setCreatedOn(new Timestamp(new Date().getTime()));

                    //save permission
                    role = roleRepository.save(role);

                    //temporary - for async operations
                    role.setTenant(tenent);

                    roleList.add(role);

                } catch (FXDefaultException e) {
                    logger.error("Bulk creation record failed for role with name " + addRole.getName());
                    e.printStackTrace();
                    BulkUploadResponse.Error err = new BulkUploadResponse.Error();
                    err.setData(addRole);
                    err.setError(e.getErrorCode());
                    err.setErrorDescription(e.getMessage());
                    errorData.add(err);
                }
            }
        });

        // save all customers
        Iterable<Role> resp = roleRepository.saveAll(roleList);
        List<Object> exportData = new ArrayList<>();
        resp.forEach(role -> {


            //send async request to update auth0 and other tenants
            //asyncUpdateOperations(role, null, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, "crud", false);
        	
        	updateOperations(role, null, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, "crud", authUpdate);

            //save to history
            historyService.saveHistoryRecord(role, "CREATE");
            // prep export list by adding projection
            exportData.add(projectionFactory.createProjection(BasicRoleProjection.class, role));

        });

        response.setData(exportData);
        response.setErrorData(errorData);
        response.setTotalRecords(addRoleList.size());
        response.setSuccessCount(exportData.size());
        System.out.println("Roles bulk creation completed. No of successful records : " + exportData.size());

        return response;
    }

    @Override
    public Object update(String projection, Long id, UpdateRole updateRole, String user, String tenent, boolean tenantUpdate) throws FXDefaultException {
        if (!roleRepository.existsById(id)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Role role = roleRepository.findById(id).get();

        if (!updateRole.getVersion().equals(role.getVersion())) {
            throw new FXDefaultException("3003", "VERSION_MISMATCH", Translator.toLocale("VERSION_MISMATCH"), new Date(), HttpStatus.BAD_REQUEST);
        }

        //update only the description
        role.setDescription(updateRole.getDescription());
        role.setSearch(role.getName() + updateRole.getDescription());

        role.setCreatedBy(user);
        role.setCreatedOn(new Timestamp(new Date().getTime()));

        role = roleRepository.save(role);

        //temporary - for async operations
        role.setTenant(tenent);

        historyService.saveHistoryRecord(role, "UPDATE");

        ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
        providerRoleIdClass.setRole(id);
        providerRoleIdClass.setProvider(PROVIDER);
        if (providerRoleRepository.existsById(providerRoleIdClass)) {
            ProviderRole providerRole =
                    providerRoleRepository.findById(providerRoleIdClass).get();

            String providerRoleId1 = providerRole.getProviderRoleId();

            String subPath = ROOT_PATH + "/" + providerRoleId1;
            //send async request to update auth0
           // asyncUpdateOperations(role, null, HttpMethod.PUT, subPath, tenent, tenantUpdate, "crud", true);
                  updateOperations(role, null, HttpMethod.PUT, subPath, tenent, tenantUpdate, "crud", true);/*amal*/
        }

        return projectionFactory.createProjection(RoleProjectionWithPermissions.class, role);
    }

    @Override
    public Object delete(String projection, Long id, String user, String tenent, boolean tenantUpdate) throws FXDefaultException {
        if (!roleRepository.existsById(id)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (rolePermissionRepository.existsByRoleId(id)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("ROLE_FK"), new Date(), HttpStatus.BAD_REQUEST);
        }
        System.out.println("DELETE STARTED");

        Role role = roleRepository.findById(id).get();

        //TODO recheck below
        if (userRoleRepository.existsByRole(role)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("ROLE_FK"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (roleGroupRoleRepository.existsByRoleId(id)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("ROLE_FK"), new Date(), HttpStatus.BAD_REQUEST);
        }

        ProviderRole providerRole = new ProviderRole();
        boolean ispresent = providerRoleRepository.findFirstByRoleIdAndProvider(id,PROVIDER).isPresent();
        boolean isExist = providerRoleRepository.existsByRoleIdAndProvider(id,PROVIDER);
        System.out.println("DELETE PROVIDER ID EXISTS IN DB "+ PROVIDER+ " "+ id);
        System.out.println("IS PRESENT BEFORE DELETE : "+ ispresent);
        System.out.println("EXIST BY ID  "+isExist);

        if(providerRoleRepository.findFirstByRoleIdAndProvider(id,PROVIDER).isPresent()) {
            providerRole =
                    providerRoleRepository.findFirstByRoleIdAndProvider(id,PROVIDER).get();
        }
        //delete from db
        //temporary - for async operations
        role.setTenant(tenent);

        historyService.saveHistoryRecord(role, "DELETE");
        System.out.println("DELETE HISTORY SAVED: "+ tenent+ " : "+id+ " : "+ role.getName());

        //delete in auth0
        //ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass(id, PROVIDER);
        System.out.println("ROLE : CHECK IF EXISTS BY PROVIDER ID :"+ PROVIDER + " "+ id );

        if (providerRoleRepository.existsByRoleIdAndProvider(id,PROVIDER)) {

            String providerRoleId1 = providerRole.getProviderRoleId();

            String subPath = ROOT_PATH + "/" + providerRoleId1;

            System.out.println("DELETE : "+subPath);
            //send async request to update auth0
            //asyncUpdateOperations(role, null, HttpMethod.DELETE, subPath, tenent, tenantUpdate, "crud", true);
            updateOperations(role, null, HttpMethod.DELETE, subPath, tenent, tenantUpdate, "crud", true);/*amal*/
            
            ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass(id,PROVIDER);
            //providerRoleRepository.deleteById(providerRoleIdClass);
        }
        /*try {
            roleRepository.deleteById(id);
        }catch (Exception e){
            e.printStackTrace();
        }*/


        return projectionFactory.createProjection(BasicRoleProjection.class, role);
    }


    @Override
    public Object assign(String projection, Long roleId, Long permissionId, String user, String tenent, boolean tenantUpdate,boolean notInAuth0) throws FXDefaultException {
    	
    	
    	if (!roleRepository.existsById(roleId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (!permissionRepository.existsById(permissionId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_PERM_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId) && !notInAuth0) {
            throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_ROLE_PERM"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Role role = roleRepository.findById(roleId).get();
        Permission permission = permissionRepository.findById(permissionId).get();
        
        RolePermission rolePermission =null;
        if(notInAuth0) {
        	rolePermission=rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId).get();
        }else {
        	 rolePermission = new RolePermission();
        }

        RolePermissionId rolePermissionId = new RolePermissionId();
        
        rolePermissionId.setPermissionId(permissionId);
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermissionId.setRoleId(roleId);
        rolePermission.setRolePermissionId(rolePermissionId);

        //set created log
        rolePermission.setCreatedBy(user);
        rolePermission.setCreatedOn(new Timestamp(new Date().getTime()));

        rolePermission = rolePermissionRepository.save(rolePermission);

        //temporary for async operations
        rolePermission.setTenant(tenent);
        if (role.getRolePermissions() != null) {
            role.getRolePermissions().add(rolePermission);
        } else {
            List<RolePermission> rolePermissionList = new ArrayList<>();
            rolePermissionList.add(rolePermission);
            role.setRolePermissions(rolePermissionList);
        }
        historyService.saveRolePermissionHistoryRecord(rolePermission, "CREATE");
        
        TenantHolder.setTenantId(tenent);

        //Check if providerRole record is available to update
        ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
        providerRoleIdClass.setRole(roleId);
        providerRoleIdClass.setProvider(PROVIDER);
        System.out.println("==============assign permision BEFORE ASYNC ROLE: "+ providerRoleRepository.existsById(providerRoleIdClass));
        System.out.println("==============assign permision PROVIDER ID: "+ providerRoleIdClass.getRole() + " : "+ providerRoleIdClass.getProvider());
        if (providerRoleRepository.existsById(providerRoleIdClass)) {
            ProviderRole providerRole =
                    providerRoleRepository.findById(providerRoleIdClass).get();

            String providerRoleId1 = providerRole.getProviderRoleId();

            String subPath = ROOT_PATH + "/" + providerRoleId1;
            //send async request to update auth0
            //asyncUpdateOperations(role, permission, HttpMethod.PUT, subPath, tenent, tenantUpdate, "assign", true);
            updateOperationsForAuth0(role, permission, HttpMethod.PUT, subPath, tenent, tenantUpdate, "assign", true);/*amal*/

        }

        return projectionFactory.createProjection(RolePermissionProjection.class, rolePermission);
    }
    
    @Override
    public Object assignAsync(String projection, Long roleId, Long permissionId, String user, String tenent, boolean tenantUpdate,boolean notInAuth0) throws FXDefaultException {
	
    	if (!roleRepository.existsById(roleId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (!permissionRepository.existsById(permissionId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_PERM_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId) && !notInAuth0) {
            throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_ROLE_PERM"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Role role = roleRepository.findById(roleId).get();
        Permission permission = permissionRepository.findById(permissionId).get();
        
        RolePermission rolePermission =null;
        if(notInAuth0) {
        	rolePermission=rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId).get();
        }else {
        	 rolePermission = new RolePermission();
        }

        RolePermissionId rolePermissionId = new RolePermissionId();
        
        rolePermissionId.setPermissionId(permissionId);
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermissionId.setRoleId(roleId);
        rolePermission.setRolePermissionId(rolePermissionId);

        //set created log
        rolePermission.setCreatedBy(user);
        rolePermission.setCreatedOn(new Timestamp(new Date().getTime()));

        rolePermission = rolePermissionRepository.save(rolePermission);

        //temporary for async operations
        rolePermission.setTenant(tenent);
        if (role.getRolePermissions() != null) {
            role.getRolePermissions().add(rolePermission);
        } else {
            List<RolePermission> rolePermissionList = new ArrayList<>();
            rolePermissionList.add(rolePermission);
            role.setRolePermissions(rolePermissionList);
        }
        historyService.saveRolePermissionHistory(rolePermission, "CREATE");
        

        //Check if providerRole record is available to update
        ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
        providerRoleIdClass.setRole(roleId);
        providerRoleIdClass.setProvider(PROVIDER);
        System.out.println("==============assign permision BEFORE ASYNC ROLE: "+ providerRoleRepository.existsById(providerRoleIdClass));
        System.out.println("==============assign permision PROVIDER ID: "+ providerRoleIdClass.getRole() + " : "+ providerRoleIdClass.getProvider());
        if (providerRoleRepository.existsById(providerRoleIdClass)) {
            ProviderRole providerRole =
                    providerRoleRepository.findById(providerRoleIdClass).get();

            String providerRoleId1 = providerRole.getProviderRoleId();

            String subPath = ROOT_PATH + "/" + providerRoleId1;
            //send async request to update auth0
            //asyncUpdateOperations(role, permission, HttpMethod.PUT, subPath, tenent, tenantUpdate, "assign", true);
            updateOperationsForAuth0(role, permission, HttpMethod.PUT, subPath, tenent, tenantUpdate, "assign", true);/*amal*/

        }

        return projectionFactory.createProjection(RolePermissionProjection.class, rolePermission);
    }

    @Override
    public Object remove(String projection, Long roleId, Long permissionId, String user, String tenent, boolean tenantUpdate) throws FXDefaultException {
        if (!roleRepository.existsById(roleId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (!permissionRepository.existsById(permissionId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_PERM_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_PERM"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Role role = roleRepository.findById(roleId).get();
        RolePermission rolePermission = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId).get();

        //temporary- for async operation
        rolePermission.setTenant(tenent);

        role.getRolePermissions().remove(rolePermission);

        historyService.saveHistoryRecord(rolePermission, "DELETE");

        Permission permission = permissionRepository.findById(permissionId).get();

        //Check if providerRole record is available to update
        ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
        providerRoleIdClass.setRole(roleId);
        providerRoleIdClass.setProvider(PROVIDER);
        System.out.println("==============Remove permisions BEFORE CHECKING IF VALUE EXIST"+ roleId+ " "+ PROVIDER);
        if (providerRoleRepository.existsById(providerRoleIdClass)) {
            ProviderRole providerRole =
                    providerRoleRepository.findById(providerRoleIdClass).get();

            String providerRoleId1 = providerRole.getProviderRoleId();

            String subPath = ROOT_PATH + "/" + providerRoleId1;
            //send async request to update auth0
           // asyncUpdateOperations(role, permission, HttpMethod.PUT, subPath, tenent, tenantUpdate, "remove", true);
            updateOperationsForRemove(role, permission, HttpMethod.PUT, subPath, tenent, tenantUpdate, "remove", true,rolePermission.getRolePermissionId());/*amal*/
        }

        return projectionFactory.createProjection(RolePermissionProjection.class, rolePermission);

    }

    @Override
    @Async
    public CompletableFuture<?> asyncUpdateOperations(Role role, Permission permission, HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate, String operationType, boolean authUpdate) {
        System.out.println("async started: "+tenant + " : "+ tenantUpdate+ " : " + authUpdate+ " "+ httpMethod);
        return CompletableFuture.runAsync(() -> {
            logger.debug("Async task started to update tenants");
            TenantHolder.setTenantId(tenant);

            ArrayList<CompletableFuture<?>> futureList = new ArrayList<>();

            // Start new Async task
            logger.debug("before async");
            AsyncTask asyncTask =
                    this.asyncTaskService.saveTask(new AsyncTask(), "Role_" + String.valueOf(role.getId()),
                            AsyncTaskDef.TASK_INITIATED,
                            AsyncTaskDef.Task.NEW_PERMISSION_CREATION, null);

            if(authUpdate) {
                System.out.println("UPDATING ANRKR: "+httpMethod);
                futureList.add(updateAuth0(role, httpMethod, subpath, tenant, asyncTask));
            }
            //Create/Delete/Update permission in other tenants if tenantUpdate is enabled and is role CRUD operation
            if (tenantUpdate && operationType.equalsIgnoreCase("crud")) {
                System.out.println("UPDATING OTHER TENANTS" + httpMethod);
                futureList.add(updateOtherTenants(tenant, role, httpMethod, asyncTask));
            } else if (tenantUpdate) {
                //For assign and remove PUT, DELETE methods used respectively
                System.out.println("UPDATING OTHER TENANTS");
                HttpMethod tenantHttpMethod = httpMethod;
                if (operationType.equalsIgnoreCase("assign")) {
                    tenantHttpMethod = HttpMethod.PUT;
                } else if (operationType.equalsIgnoreCase("remove")) {
                    tenantHttpMethod = HttpMethod.DELETE;
                }

                futureList.add(updateOtherTenants(tenant, role, permission, tenantHttpMethod, asyncTask));
            }
            TenantHolder.clear();
        }, executor);
    }

    @Override
    @Async
    public CompletableFuture<?> updateAuth0(Role role, HttpMethod httpMethod, String subpath, String tenant, AsyncTask at) {
        System.out.println("RUNNING ASYNC TO UPDATE TENANT");
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenant);
            logger.debug("Starting to update auth0 for role " + role.getId()+ " "+httpMethod);

            HashMap<String, Object> meta = new HashMap<>();
            AsyncSubTask asyncSubTask =
                    this.asyncTaskService.saveSubTask(new AsyncSubTask(), at,
                            AsyncTaskDef.TASK_INITIATED,
                            AsyncTaskDef.Task.NEW_UPDATE_PROVIDER, null);

            RoleRequest roleRequest = null;
            ResponseEntity responseEntity = null;
            RoleResponse roleResponse = null;

            //Get clientId/ secret for default tenant from comn-system-info
            TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);
            System.out.println("ANRKR PROVIDER : "+ tenantAuthProvider.getAppTenant());

            //Create Request Entity for POST/PUT requests
            if (httpMethod != HttpMethod.DELETE) {
                roleRequest = new RoleRequest();
                NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
                try {
                    nullAwareBeanUtilsBean.copyProperties(roleRequest, role);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Set app id and app type
                roleRequest.setApplicationId(tenantAuthProvider.getClientId());
                roleRequest.setApplicationType("client");
                if(role.getRolePermissions()==null) {
                    roleRequest.setPermissions(new String[0]);
                }else{
                	
                	if (role.getRolePermissions()!=null) {///Amal
                		
	                    List<RolePermission> rolePermissionList = role.getRolePermissions();
	                    List<String> permissionList = new ArrayList<>();
	                    for(RolePermission rolePermission : rolePermissionList){
	                        Permission permission = rolePermission.getPermission();
	                        if(providerPermissionRepository.existsByProviderPermissionIdClass_Permission(permission.getId())){
	                            ProviderPermission providerPermission =
	                                    providerPermissionRepository.findByProviderPermissionIdClass_Permission(permission.getId()).get();
	                            permissionList.add(providerPermission.getProviderPermissionId());
	                        }
	                    }
                	
                    System.out.println("Permission LIST size : " + permissionList.size());
                    String[] permissions = new String[permissionList.size()];
                    for(int i=0; i<permissionList.size(); i++){
                        permissions[i] = permissionList.get(i);
                    }
                    System.out.println("Permissions arr size : " + permissions.length);
                    roleRequest.setPermissions(permissions);
                    
            	  }  
                }
                if (httpMethod.equals(HttpMethod.PUT)) {
                    List<RolePermission> rolePermissionList = role.getRolePermissions();
                    String[] permissionsArray = new String[rolePermissionList.size()];
                    logger.debug("list size " + rolePermissionList.size());
                    for (int i = 0; i < rolePermissionList.size(); i++) {
                        Long apiPermissionId = rolePermissionList.get(i).getPermission().getId();
                        logger.debug(apiPermissionId);
                        if (providerPermissionRepository.existsByProviderPermissionIdClass_Permission(apiPermissionId)) {
                            ProviderPermission providerPermission = providerPermissionRepository.findByProviderPermissionIdClass_Permission(apiPermissionId).get();
                            permissionsArray[i] = providerPermission.getProviderPermissionId();
                        }
                    }
                    System.out.println("permissionsArray arr size : " + permissionsArray.length);
                    List<String> validatedPermissions = new ArrayList<>();
                    for(String permission: permissionsArray){
                        if(permission != null){
                            validatedPermissions.add(permission);
                        }
                    }
                    String[] finalArr = new String[validatedPermissions.size()];
                    for(int i = 0; i< validatedPermissions.size() ; i++){
                        finalArr[i] = validatedPermissions.get(i);
                    }
                    roleRequest.setPermissions(finalArr);
                    System.out.println("permissionsArray arr size : " + finalArr.length);
                    System.out.println("FINAL ROLE REQUEST PERMISSIONS size : " + roleRequest.getPermissions().length);
                }
            }

            //get Auth0 response
            try {
                System.out.println("sending HTTP Request :"+ httpMethod + " "+subpath);
                responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, roleRequest, RoleResponse.class);

                ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
                providerRoleIdClass.setProvider(PROVIDER);
                providerRoleIdClass.setRole(role.getId());

                //Create ProviderRole record from POST response
                System.out.println("PROVIDER ID BEFORE SAVING"+ providerRoleIdClass.getRole());
                System.out.println("PROVIDER ID BEFORE SAVING PRO"+ providerRoleIdClass.getProvider());
                if (httpMethod.equals(HttpMethod.POST)) {
                    roleResponse = (RoleResponse) responseEntity.getBody();
                    ProviderRole providerRole = new ProviderRole();
                    providerRole.setProviderRoleIdClass(providerRoleIdClass);
                    providerRole.setProviderRoleId(roleResponse.get_id());
                    System.out.println("provider saved :"+ providerRole.getRole());
                    providerRoleRepository.save(providerRole);
                }
                meta.put("role", role);
                meta.put("roleId", role.getId());
                logger.debug("Data posted to Auth0");
                this.asyncTaskService.saveSubTask(asyncSubTask,
                        null,
                        AsyncTaskDef.TASK_COMPLETED, null, meta);

            } catch (Exception ex) {
                logger.debug("Role with id " + role.getId() + " was unable to be updated in " + PROVIDER);
                ex.printStackTrace();
                this.asyncTaskService.saveSubTask(asyncSubTask,
                        null,
                        AsyncTaskDef.TASK_ERROR, null, null);
                this.asyncTaskService.saveSubTaskLog(asyncSubTask, "Auth0 update error",
                        ex.getMessage(), "",
                        ex.getMessage(), "Auth0 update error",

                        new Exception(ex.getMessage()));

            }
            TenantHolder.clear();
        }, executor);
    }

    @Async
    CompletableFuture<?> updateOtherTenants(String tenant, Role role, HttpMethod httpMethod, AsyncTask at) {
        System.out.println("updating other tenants");
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(role.getTenant());
            logger.debug("Updating other tenants");

            String entityType = "role";
            Object object = null;
            String subpath = null;

            HashMap<String, Object> meta = new HashMap<>();

            AsyncSubTask asyncSubTask =
                    this.asyncTaskService.saveSubTask(new AsyncSubTask(), at,
                            AsyncTaskDef.TASK_INITIATED,
                            AsyncTaskDef.Task.NEW_UPDATE_OTHER_TENANTS, null);

            try {
                List<AppTenant> tenantList = httpService.getTenants();
                System.out.println("OTHER TENANT COUNT for ROLE "+ role.getName() + "  "+tenantList.size());
                for (AppTenant appTenant : tenantList) {
                    if (!appTenant.getTenantId().equalsIgnoreCase(tenant)) {
                        logger.debug("Updating tenant " + appTenant.getTenantId());
                        if (httpMethod == HttpMethod.POST) {
                            //Create addPermission object
                            System.out.println("HTTP METHOD POST");
                            AddRole addRole = new AddRole();
                            addRole.setName(role.getName());
                            addRole.setDescription(role.getDescription());
                            object = addRole;

                        } else {
                            //send request to get permission by name (to get id and version
                            Role otherTenantRole = httpService.getOtherTenantEntity(appTenant.getTenantId(), Role.class, entityType, role.getName());
                            Long id = otherTenantRole.getId();
                            subpath = "/" + id.toString();
                            if (httpMethod == HttpMethod.PUT) {
                                //Create updatePermission object
                                UpdateRole updateRole = new UpdateRole();
                                updateRole.setName(role.getName());
                                updateRole.setDescription(role.getDescription());
                                updateRole.setVersion(otherTenantRole.getVersion());
                                object = updateRole;
                            }
                        }
                        //send request to update tenant
                        System.out.println("SENDING HTTPS FOR OTHER TENANTS");
                        httpService.sendOtherTenantUpdateRequest(appTenant.getTenantId(), entityType, object, httpMethod, subpath);
                    }
                }
                meta.put("role", role);
                meta.put("roleId", role.getId());
                logger.debug("Data posted to update other tenants");
                this.asyncTaskService.saveSubTask(asyncSubTask,
                        null,
                        AsyncTaskDef.TASK_COMPLETED, null, meta);
            } catch (Exception ex) {
                ex.printStackTrace();
                this.asyncTaskService.saveSubTask(asyncSubTask,
                        null,
                        AsyncTaskDef.TASK_ERROR, null, null);
                this.asyncTaskService.saveSubTaskLog(asyncSubTask, "Other tenant update error",
                        ex.getMessage(), "",
                        ex.getMessage(), "Other tenant update error",
                        new Exception(ex.getMessage()));
            }
            TenantHolder.clear();
        }, executor);
    }

    @Async
    CompletableFuture<?> updateOtherTenants(String tenant, Role role, Permission permission, HttpMethod httpMethod, AsyncTask asyncTask) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(role.getTenant());
            logger.debug("Updating other tenants");

            String entityType = "role";
            Long roleId = null, permissionId = null;

            List<AppTenant> tenantList = httpService.getTenants();

            for (AppTenant appTenant : tenantList) {
                if (!appTenant.getTenantId().equalsIgnoreCase(tenant)) {
                    logger.debug("Updating tenant " + appTenant.getTenantId());

                    //send request to get permission by name (to get id and version
                    Role otherTenantRole = httpService.getOtherTenantEntity(appTenant.getTenantId(), Role.class, "role", role.getName());
                    roleId = otherTenantRole.getId();

                    Permission otherTenantPermission = httpService.getOtherTenantEntity(appTenant.getTenantId(), Permission.class, "permission", permission.getName());
                    permissionId = otherTenantPermission.getId();

                    String subpath = "/" + roleId.toString() + "/permission/" + permissionId.toString();

                    //send request to update tenant
                    httpService.sendOtherTenantUpdateRequest(appTenant.getTenantId(), entityType, null, httpMethod, subpath);
                    System.out.println("UPDATED OTHER TENANTS : "+ subpath+" , tenant:"+ tenant );
                }
            }
            TenantHolder.clear();
        }, executor);
    }
    
    public void updateOperationsForAuth0(Role role, Permission permission, HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate, String operationType, boolean authUpdate) {
        System.out.println("================started: "+tenant + " : "+ tenantUpdate+ " : " + authUpdate+ " "+ httpMethod);
        TenantHolder.setTenantId(tenant);
        logger.debug("====================before async");

	    TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);
	    
	    /*if(tenant.equals("AnRkr")){
	        tenantAuthProvider.setPrimary(true);
	    }
        if(authUpdate) {
            System.out.println("=====================UPDATING ANRKR: "+httpMethod);
            updateAuth0RoleWithPermission(role,permission,httpMethod, subpath, tenant);
        }
        //Create/Delete/Update permission in other tenants if tenantUpdate is enabled and is role CRUD operation
        if (tenantUpdate && operationType.equalsIgnoreCase("crud")) {
            System.out.println("====================UPDATING OTHER TENANTS" + httpMethod);
            updateOtherTenants(tenant, role, httpMethod);
        } else if (tenantUpdate) {
            //For assign and remove PUT, DELETE methods used respectively
            System.out.println("===================UPDATING OTHER TENANTS");
            HttpMethod tenantHttpMethod = httpMethod;
            if (operationType.equalsIgnoreCase("assign")) {
                tenantHttpMethod = HttpMethod.PUT;
            } else if (operationType.equalsIgnoreCase("remove")) {
                tenantHttpMethod = HttpMethod.DELETE;
            }

            updateOtherTenants(tenant, role, permission, tenantHttpMethod);
        }*/ 
        
        if(tenant.equals("AnRkr")){
            tenantAuthProvider.setPrimary(true);
        }
        
        if (tenantAuthProvider.isPrimary()) {
            try {
            	System.out.println("=====================UPDATING ANRKR: "+httpMethod);
                updateAuth0RoleWithPermission(role,permission,httpMethod, subpath, tenant);
            }catch (Exception e){
                System.out.println(e);
            }
            //Create/Delete/Update permission in other tenants if tenantUpdate is enabled and is role CRUD operation
            if (tenantUpdate && operationType.equalsIgnoreCase("crud")) {
                System.out.println("====================UPDATING OTHER TENANTS" + httpMethod);
                updateOtherTenants(tenant, role, httpMethod);
            } else if (tenantUpdate) {
            	//For assign and remove PUT, DELETE methods used respectively
                System.out.println("===================UPDATING OTHER TENANTS");
                HttpMethod tenantHttpMethod = httpMethod;
                if (operationType.equalsIgnoreCase("assign")) {
                    tenantHttpMethod = HttpMethod.PUT;
                } else if (operationType.equalsIgnoreCase("remove")) {
                    tenantHttpMethod = HttpMethod.DELETE;
                }

                updateOtherTenants(tenant, role, permission, tenantHttpMethod);
            }
        } else {
            try {
            	updateAuth0RoleWithPermission(role,permission,httpMethod, subpath, tenant);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
        
        TenantHolder.clear();
    }
    
    
    /*amal*/
    public void updateOperations(Role role, Permission permission, HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate, String operationType, boolean authUpdate) {
            System.out.println("================started: "+tenant + " : "+ tenantUpdate+ " : " + authUpdate+ " "+ httpMethod);
            TenantHolder.setTenantId(tenant);
            logger.debug("====================before async");

        TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);
	        /*if(tenant.equals("AnRkr")){
	            tenantAuthProvider.setPrimary(true);
	        }
            if(authUpdate) {
                System.out.println("=====================UPDATING ANRKR: "+httpMethod);
                updateAuth0(role, httpMethod, subpath, tenant);
            }
            //Create/Delete/Update permission in other tenants if tenantUpdate is enabled and is role CRUD operation
            if (tenantUpdate && operationType.equalsIgnoreCase("crud")) {
                System.out.println("====================UPDATING OTHER TENANTS" + httpMethod);
                updateOtherTenants(tenant, role, httpMethod);
            } else if (tenantUpdate) {
                //For assign and remove PUT, DELETE methods used respectively
                System.out.println("===================UPDATING OTHER TENANTS");
                HttpMethod tenantHttpMethod = httpMethod;
                if (operationType.equalsIgnoreCase("assign")) {
                    tenantHttpMethod = HttpMethod.PUT;
                } else if (operationType.equalsIgnoreCase("remove")) {
                    tenantHttpMethod = HttpMethod.DELETE;
                }

                updateOtherTenants(tenant, role, permission, tenantHttpMethod);
            }*/
            
            
            if(tenant.equals("AnRkr")){
                tenantAuthProvider.setPrimary(true);
            }
            
            
            if (tenantAuthProvider.isPrimary()) {
                try {
                	System.out.println("=====================UPDATING ANRKR: "+httpMethod);
                	updateAuth0(role, httpMethod, subpath, tenant);
                }catch (Exception e){
                    System.out.println(e);
                    //delete in auth0 //primary tenant related exceptions
                }
                //Create/Delete/Update permission in other tenants if tenantUpdate is enabled and is role CRUD operation
                if (tenantUpdate && operationType.equalsIgnoreCase("crud")) {
                    System.out.println("====================UPDATING OTHER TENANTS" + httpMethod);
                    updateOtherTenants(tenant, role, httpMethod);
                } else if (tenantUpdate) {
                    //For assign and remove PUT, DELETE methods used respectively
                    System.out.println("===================UPDATING OTHER TENANTS");
                    HttpMethod tenantHttpMethod = httpMethod;
                    if (operationType.equalsIgnoreCase("assign")) {
                        tenantHttpMethod = HttpMethod.PUT;
                    } else if (operationType.equalsIgnoreCase("remove")) {
                        tenantHttpMethod = HttpMethod.DELETE;
                    }

                    updateOtherTenants(tenant, role, permission, tenantHttpMethod);
                }
            } else {
                try {
                	updateAuth0(role, httpMethod, subpath, tenant);
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
                
               /* System.out.println("===================DELETE OTHER TENANTS");
                
                HttpMethod tenantHttpMethod = httpMethod;
                if (operationType.equalsIgnoreCase("remove")) {
                    tenantHttpMethod = HttpMethod.DELETE;
                    deleteLoginTenants(tenant, role, permission, tenantHttpMethod);
                }*/
            }
            
            TenantHolder.clear();
    }
    
    /*amal*/
    public void updateOperationsForRemove(Role role, Permission permission, HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate, String operationType, boolean authUpdate,RolePermissionId rolePermissionId) {
            System.out.println("================started: "+tenant + " : "+ tenantUpdate+ " : " + authUpdate+ " "+ httpMethod);
            TenantHolder.setTenantId(tenant);
            logger.debug("====================before async");

        TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);
   
            if(tenant.equals("AnRkr")){
                tenantAuthProvider.setPrimary(true);
            }
             
            if (tenantAuthProvider.isPrimary()) {
                try {
                	System.out.println("=====================UPDATING ANRKR: "+httpMethod);
                	updateAuth0Remove(role, httpMethod, subpath, tenant,rolePermissionId,operationType);
                }catch (Exception e){
                    System.out.println(e);
                    //delete in auth0 //primary tenant related exceptions
                }
                //Create/Delete/Update permission in other tenants if tenantUpdate is enabled and is role CRUD operation
                if (tenantUpdate && operationType.equalsIgnoreCase("crud")) {
                    System.out.println("====================UPDATING OTHER TENANTS" + httpMethod);
                    updateOtherTenants(tenant, role, httpMethod);
                } else if (tenantUpdate) {
                    //For assign and remove PUT, DELETE methods used respectively
                    System.out.println("===================UPDATING OTHER TENANTS");
                    HttpMethod tenantHttpMethod = httpMethod;
                    if (operationType.equalsIgnoreCase("assign")) {
                        tenantHttpMethod = HttpMethod.PUT;
                    } else if (operationType.equalsIgnoreCase("remove")) {
                        tenantHttpMethod = HttpMethod.DELETE;
                    }

                    updateOtherTenants(tenant, role, permission, tenantHttpMethod);
                }
            } else {
                try {
                	updateAuth0Remove(role, httpMethod, subpath, tenant,rolePermissionId,operationType);
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
            }
            
            TenantHolder.clear();
    }
    
    /*amal*/
    public void updateAuth0(Role role, HttpMethod httpMethod, String subpath, String tenant) {
       
            TenantHolder.setTenantId(tenant);
            System.out.println("====================Starting to update auth0 for role " + role.getId()+ " "+httpMethod);

            HashMap<String, Object> meta = new HashMap<>();

            RoleRequest roleRequest = null;
            ResponseEntity responseEntity = null;
            RoleResponse roleResponse = null;

            //Get clientId/ secret for default tenant from comn-system-info
            TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);
            System.out.println("=================ANRKR PROVIDER : "+ tenantAuthProvider.getAppTenant());

            //Create Request Entity for POST/PUT requests
            if (httpMethod != HttpMethod.DELETE) {
                roleRequest = new RoleRequest();
                NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
                try {
                    nullAwareBeanUtilsBean.copyProperties(roleRequest, role);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Set app id and app type
                roleRequest.setApplicationId(tenantAuthProvider.getClientId());
                roleRequest.setApplicationType("client");
                
                //role.setApplicationId(tenantAuthProvider.getClientId());
                //role.setTenantId(tenant);
                
                if(role.getRolePermissions()==null || role.getRolePermissions().isEmpty()) {
                    roleRequest.setPermissions(new String[0]);
                }else{
                	if (role.getRolePermissions()!=null) {
	                    List<RolePermission> rolePermissionList = role.getRolePermissions();
	                    List<String> permissionList = new ArrayList<>();
	                    for(RolePermission rolePermission : rolePermissionList){
	                        Permission permission = rolePermission.getPermission();
	                        if(providerPermissionRepository.existsByProviderPermissionIdClass_Permission(permission.getId())){
	                            ProviderPermission providerPermission =
	                                    providerPermissionRepository.findByProviderPermissionIdClass_Permission(permission.getId()).get();
	                            permissionList.add(providerPermission.getProviderPermissionId());
	                        }
	                    }
	                    System.out.println("==================Permission LIST size : " + permissionList.size());
	                    String[] permissions = new String[permissionList.size()];
	                    for(int i=0; i<permissionList.size(); i++){
	                        permissions[i] = permissionList.get(i);
	                    }
	                    System.out.println("===================Permissions arr size : " + permissions.length);
	                    roleRequest.setPermissions(permissions);
                	}
                }
                if (httpMethod.equals(HttpMethod.PUT)) {
                	if (role.getRolePermissions()!=null && !role.getRolePermissions().isEmpty()) {
	                    List<RolePermission> rolePermissionList = role.getRolePermissions();
	                    String[] permissionsArray = new String[rolePermissionList.size()];
	                    System.out.println("====================list size " + rolePermissionList.size());
	                    for (int i = 0; i < rolePermissionList.size(); i++) {
	                        Long apiPermissionId = rolePermissionList.get(i).getPermission().getId();
	                        logger.debug(apiPermissionId);
	                        if (providerPermissionRepository.existsByProviderPermissionIdClass_Permission(apiPermissionId)) {
	                            ProviderPermission providerPermission = providerPermissionRepository.findByProviderPermissionIdClass_Permission(apiPermissionId).get();
	                            permissionsArray[i] = providerPermission.getProviderPermissionId();
	                        }
	                    }
	                    System.out.println("===============permissionsArray arr size : " + permissionsArray.length);
	                    List<String> validatedPermissions = new ArrayList<>();
	                    for(String permission: permissionsArray){
	                        if(permission != null){
	                            validatedPermissions.add(permission);
	                        }
	                    }
	                    String[] finalArr = new String[validatedPermissions.size()];
	                    for(int i = 0; i< validatedPermissions.size() ; i++){
	                        finalArr[i] = validatedPermissions.get(i);
	                    }
	                    roleRequest.setPermissions(finalArr);
	                    System.out.println("============permissionsArray arr size : " + finalArr.length);
	                    System.out.println("============FINAL ROLE REQUEST PERMISSIONS size : " + roleRequest.getPermissions().length);
	               
                	}
                }
            }

            //get Auth0 response
            try {
                System.out.println("sending HTTP Request :"+ httpMethod + " "+subpath);
                responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, roleRequest, RoleResponse.class);
                
                LoggerRequest.getInstance().logInfo("==========ResponseEntity=========="+responseEntity);

                if(httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
                    roleRepository.save(role);
                                   
                }

                ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
                providerRoleIdClass.setProvider(PROVIDER);
                providerRoleIdClass.setRole(role.getId());


                //Create ProviderRole record from POST response
                System.out.println("===============PROVIDER ID BEFORE SAVING"+ providerRoleIdClass.getRole());
                System.out.println("===============PROVIDER ID BEFORE SAVING PRO"+ providerRoleIdClass.getProvider());
                if (httpMethod.equals(HttpMethod.POST)) {
                    roleResponse = (RoleResponse) responseEntity.getBody();
                    ProviderRole providerRole = new ProviderRole();
                    providerRole.setProviderRoleIdClass(providerRoleIdClass);
                    providerRole.setProviderRoleId(roleResponse.get_id());
                    System.out.println("=============provider saved :"+ providerRole.getRole());
                    providerRoleRepository.save(providerRole);
                    
                    if(responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==200) {
                    	updateRoleByName(role.getName());
                    }
                }
                
                if (httpMethod.equals(HttpMethod.DELETE) && responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==204) {
                	deleteRoleByName(role.getName());
                }
                
                meta.put("role", role);
                meta.put("roleId", role.getId());
                logger.debug("===========Data posted to Auth0");

            } catch (Exception ex) {
                logger.debug("Role with id " + role.getId() + " was unable to be updated in " + PROVIDER);
                ex.printStackTrace();

            }
            TenantHolder.clear();

    }
    
    public void updateAuth0Remove(Role role, HttpMethod httpMethod, String subpath, String tenant,RolePermissionId rolePermissionId,String operationType) {
        TenantHolder.setTenantId(tenant);
        System.out.println("====================Starting to update auth0 for role " + role.getId()+ " "+httpMethod);

        HashMap<String, Object> meta = new HashMap<>();

        RoleRequest roleRequest = null;
        ResponseEntity responseEntity = null;
        RoleResponse roleResponse = null;

        //Get clientId/ secret for default tenant from comn-system-info
        TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);
        System.out.println("=================ANRKR PROVIDER : "+ tenantAuthProvider.getAppTenant());

        //Create Request Entity for POST/PUT requests
        if (httpMethod != HttpMethod.DELETE) {
            roleRequest = new RoleRequest();
            NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
            try {
                nullAwareBeanUtilsBean.copyProperties(roleRequest, role);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Set app id and app type
            roleRequest.setApplicationId(tenantAuthProvider.getClientId());
            roleRequest.setApplicationType("client");
            
            //role.setApplicationId(tenantAuthProvider.getClientId());
            //role.setTenantId(tenant);
            
            LoggerRequest.getInstance().logInfo("==========1==========");
            
            if(role.getRolePermissions()==null || role.getRolePermissions().isEmpty()) {
            	 LoggerRequest.getInstance().logInfo("==========IF==========");
                roleRequest.setPermissions(new String[0]);
            }else{
            	if (role.getRolePermissions()!=null) {
            		 LoggerRequest.getInstance().logInfo("==========ELSE==========");
            		 
                    List<RolePermission> rolePermissionList = role.getRolePermissions();
                    List<String> permissionList = new ArrayList<>();
                    for(RolePermission rolePermission : rolePermissionList){
                        Permission permission = rolePermission.getPermission();
                        if(providerPermissionRepository.existsByProviderPermissionIdClass_Permission(permission.getId())){
                            ProviderPermission providerPermission =
                                    providerPermissionRepository.findByProviderPermissionIdClass_Permission(permission.getId()).get();
                            permissionList.add(providerPermission.getProviderPermissionId());
                        }
                    }
                    System.out.println("==================Permission LIST size : " + permissionList.size());
                    String[] permissions = new String[permissionList.size()];
                    for(int i=0; i<permissionList.size(); i++){
                        permissions[i] = permissionList.get(i);
                    }
                    System.out.println("===================Permissions arr size : " + permissions.length);
                    roleRequest.setPermissions(permissions);
            	}
            }
            if (httpMethod.equals(HttpMethod.PUT)) {
            	if (role.getRolePermissions()!=null && !role.getRolePermissions().isEmpty()) {
            		LoggerRequest.getInstance().logInfo("==========ELSE PUT==========");
            		
                    List<RolePermission> rolePermissionList = role.getRolePermissions();
                    String[] permissionsArray = new String[rolePermissionList.size()];
                    System.out.println("====================list size " + rolePermissionList.size());
                    for (int i = 0; i < rolePermissionList.size(); i++) {
                        Long apiPermissionId = rolePermissionList.get(i).getPermission().getId();
                        logger.debug(apiPermissionId);
                        if (providerPermissionRepository.existsByProviderPermissionIdClass_Permission(apiPermissionId)) {
                            ProviderPermission providerPermission = providerPermissionRepository.findByProviderPermissionIdClass_Permission(apiPermissionId).get();
                            permissionsArray[i] = providerPermission.getProviderPermissionId();
                        }
                    }
                    System.out.println("===============permissionsArray arr size : " + permissionsArray.length);
                    List<String> validatedPermissions = new ArrayList<>();
                    for(String permission: permissionsArray){
                        if(permission != null){
                            validatedPermissions.add(permission);
                        }
                    }
                    String[] finalArr = new String[validatedPermissions.size()];
                    for(int i = 0; i< validatedPermissions.size() ; i++){
                        finalArr[i] = validatedPermissions.get(i);
                    }
                    roleRequest.setPermissions(finalArr);
                    System.out.println("============permissionsArray arr size : " + finalArr.length);
                    System.out.println("============FINAL ROLE REQUEST PERMISSIONS size : " + roleRequest.getPermissions().length);
               
            	}
            }
        }

        //get Auth0 response
        try {
            System.out.println("sending HTTP Request :"+ httpMethod + " "+subpath);
            responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, roleRequest, RoleResponse.class);
            
            LoggerRequest.getInstance().logInfo("==========ResponseEntity=========="+responseEntity);

            if(httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
                roleRepository.save(role);
                               
            }
            
            LoggerRequest.getInstance().logInfo("==========httpMethod=========="+httpMethod);

            ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
            providerRoleIdClass.setProvider(PROVIDER);
            providerRoleIdClass.setRole(role.getId());


            //Create ProviderRole record from POST response
            System.out.println("===============PROVIDER ID BEFORE SAVING"+ providerRoleIdClass.getRole());
            System.out.println("===============PROVIDER ID BEFORE SAVING PRO"+ providerRoleIdClass.getProvider());
            if (httpMethod.equals(HttpMethod.POST)) {
                roleResponse = (RoleResponse) responseEntity.getBody();
                ProviderRole providerRole = new ProviderRole();
                providerRole.setProviderRoleIdClass(providerRoleIdClass);
                providerRole.setProviderRoleId(roleResponse.get_id());
                System.out.println("=============provider saved :"+ providerRole.getRole());
                providerRoleRepository.save(providerRole);
                
                if(responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==200) {
                	updateRoleByName(role.getName());
                }
            }
            
            if (httpMethod.equals(HttpMethod.PUT) && responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==200 && operationType.equalsIgnoreCase("remove")) {
            	LoggerRequest.getInstance().logInfo("==========ResponseCode=========="+responseEntity.getStatusCodeValue());
            	rolePermissionRepository.deleteById(rolePermissionId);
            }
            
            meta.put("role", role);
            meta.put("roleId", role.getId());
            logger.debug("===========Data posted to Auth0");

        } catch (Exception ex) {
            logger.debug("Role with id " + role.getId() + " was unable to be updated in " + PROVIDER);
            ex.printStackTrace();

        }
        TenantHolder.clear();

}
    
    public void updateAuth0RoleWithPermission(Role role,Permission authPermission,HttpMethod httpMethod, String subpath, String tenant) {
        
        TenantHolder.setTenantId(tenant);
        System.out.println("====================Starting to update auth0 for role " + role.getId()+ " "+httpMethod);

        HashMap<String, Object> meta = new HashMap<>();

        RoleRequest roleRequest = null;
        ResponseEntity responseEntity = null;
        RoleResponse roleResponse = null;

        //Get clientId/ secret for default tenant from comn-system-info
        TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);
        System.out.println("=================ANRKR PROVIDER : "+ tenantAuthProvider.getAppTenant());

        //Create Request Entity for POST/PUT requests
        if (httpMethod != HttpMethod.DELETE) {
            roleRequest = new RoleRequest();
            NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
            try {
                nullAwareBeanUtilsBean.copyProperties(roleRequest, role);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Set app id and app type
            roleRequest.setApplicationId(tenantAuthProvider.getClientId());
            roleRequest.setApplicationType("client");
            
            //role.setApplicationId(tenantAuthProvider.getClientId());
            //role.setTenantId(tenant);
            
            if(role.getRolePermissions()==null) {
                roleRequest.setPermissions(new String[0]);
            }else{
            	if (role.getRolePermissions()!=null) {
                    List<RolePermission> rolePermissionList = role.getRolePermissions();
                    List<String> permissionList = new ArrayList<>();
                    for(RolePermission rolePermission : rolePermissionList){
                        Permission permission = rolePermission.getPermission();
                        if(providerPermissionRepository.existsByProviderPermissionIdClass_Permission(permission.getId())){
                            ProviderPermission providerPermission =
                                    providerPermissionRepository.findByProviderPermissionIdClass_Permission(permission.getId()).get();
                            permissionList.add(providerPermission.getProviderPermissionId());
                        }
                    }
                    System.out.println("==================Permission LIST size : " + permissionList.size());
                    String[] permissions = new String[permissionList.size()];
                    for(int i=0; i<permissionList.size(); i++){
                        permissions[i] = permissionList.get(i);
                    }
                    System.out.println("===================Permissions arr size : " + permissions.length);
                    roleRequest.setPermissions(permissions);
            	}
            }
            if (httpMethod.equals(HttpMethod.PUT)) {
            	if (role.getRolePermissions()!=null) {
                    List<RolePermission> rolePermissionList = role.getRolePermissions();
                    String[] permissionsArray = new String[rolePermissionList.size()];
                    System.out.println("====================list size " + rolePermissionList.size());
                    for (int i = 0; i < rolePermissionList.size(); i++) {
                        Long apiPermissionId = rolePermissionList.get(i).getPermission().getId();
                        logger.debug(apiPermissionId);
                        if (providerPermissionRepository.existsByProviderPermissionIdClass_Permission(apiPermissionId)) {
                            ProviderPermission providerPermission = providerPermissionRepository.findByProviderPermissionIdClass_Permission(apiPermissionId).get();
                            permissionsArray[i] = providerPermission.getProviderPermissionId();
                        }
                    }
                    System.out.println("===============permissionsArray arr size : " + permissionsArray.length);
                    List<String> validatedPermissions = new ArrayList<>();
                    for(String permission: permissionsArray){
                        if(permission != null){
                            validatedPermissions.add(permission);
                        }
                    }
                    String[] finalArr = new String[validatedPermissions.size()];
                    for(int i = 0; i< validatedPermissions.size() ; i++){
                        finalArr[i] = validatedPermissions.get(i);
                    }
                    roleRequest.setPermissions(finalArr);
                    System.out.println("============permissionsArray arr size : " + finalArr.length);
                    System.out.println("============FINAL ROLE REQUEST PERMISSIONS size : " + roleRequest.getPermissions().length);
               
            	}
            }
        }

        //get Auth0 response
        try {
            System.out.println("sending HTTP Request :"+ httpMethod + " "+subpath);
            responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, roleRequest, RoleResponse.class);
            
            LoggerRequest.getInstance().logInfo("==========ResponseEntity=========="+responseEntity);

            if(httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
                roleRepository.save(role);
                               
            }

            ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
            providerRoleIdClass.setProvider(PROVIDER);
            providerRoleIdClass.setRole(role.getId());


            //Create ProviderRole record from POST response
            System.out.println("===============PROVIDER ID BEFORE SAVING"+ providerRoleIdClass.getRole());
            System.out.println("===============PROVIDER ID BEFORE SAVING PRO"+ providerRoleIdClass.getProvider());
            if (httpMethod.equals(HttpMethod.POST)) {
                roleResponse = (RoleResponse) responseEntity.getBody();
                ProviderRole providerRole = new ProviderRole();
                providerRole.setProviderRoleIdClass(providerRoleIdClass);
                providerRole.setProviderRoleId(roleResponse.get_id());
                System.out.println("=============provider saved :"+ providerRole.getRole());
                providerRoleRepository.save(providerRole);
                
                if(responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==200) {
                	updateRoleByName(role.getName());
                }
            }
            meta.put("role", role);
            meta.put("roleId", role.getId());
            logger.debug("===========Data posted to Auth0");
            
            if(httpMethod.equals(HttpMethod.PUT) && responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==200) {
            	try {
            		roleResponse = (RoleResponse) responseEntity.getBody();
            		
            		Optional<ProviderPermission> providerPermissionOp =providerPermissionRepository.findByProviderPermissionIdClass_Permission(authPermission.getId());
            		
            		if(providerPermissionOp!=null && providerPermissionOp.isPresent() && roleResponse!=null) {
            			ProviderPermission providerPermission =providerPermissionOp.get();
            			List<String> permissionList= Arrays.asList(roleResponse.getPermissions());
            			
            			if(permissionList!=null && permissionList.contains(providerPermission.getProviderPermissionId())) {
            				updateRolePermission(role.getId(),authPermission.getId());
            			}
            		}
	            	
				} catch (Exception e) {
					LoggerRequest.getInstance().logInfo("Exception "+e.toString());
				}
            }

        } catch (Exception ex) {
        	LoggerRequest.getInstance().logInfo("Role with id " + role.getId() + " was unable to be updated in " + PROVIDER);
        }
        TenantHolder.clear();

}

    /*amal*/
    void updateOtherTenants(String tenant, Role role, HttpMethod httpMethod) {
            System.out.println("================updating other tenants");
            TenantHolder.setTenantId(role.getTenant());
            System.out.println("=================Updating other tenants");

            String entityType = "role";
            Object object = null;
            String subpath = null;

            HashMap<String, Object> meta = new HashMap<>();

            try {
                List<AppTenant> tenantList = httpService.getTenants();
                System.out.println("============OTHER TENANT COUNT for ROLE "+ role.getName() + "  "+tenantList.size());
                for (AppTenant appTenant : tenantList) {
                   // TimeUnit.SECONDS.sleep(2);
                    if (!appTenant.getTenantId().equalsIgnoreCase(tenant)) {
                    	 System.out.println("======================Updating tenant " + appTenant.getTenantId());
                        if (httpMethod == HttpMethod.POST) {
                            //Create addPermission object
                            System.out.println("============HTTP METHOD POST");
                            AddRole addRole = new AddRole();
                            addRole.setName(role.getName());
                            addRole.setDescription(role.getDescription());
                            object = addRole;

                        } else {
                            //send request to get permission by name (to get id and version
                            Role otherTenantRole = httpService.getOtherTenantEntity(appTenant.getTenantId(), Role.class, entityType, role.getName());
                            Long id = otherTenantRole.getId();
                            subpath = "/" + id.toString();
                            if (httpMethod == HttpMethod.PUT) {
                                //Create updatePermission object
                                UpdateRole updateRole = new UpdateRole();
                                updateRole.setName(role.getName());
                                updateRole.setDescription(role.getDescription());
                                updateRole.setVersion(otherTenantRole.getVersion());
                                object = updateRole;
                            }
                        }
                        //send request to update tenant
                        System.out.println("=========SENDING HTTPS FOR OTHER TENANTS");
                        httpService.sendOtherTenantUpdateRequest(appTenant.getTenantId(), entityType, object, httpMethod, subpath);
                    }
                }
                meta.put("role", role);
                meta.put("roleId", role.getId());
                logger.debug("================Data posted to update other tenants");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            TenantHolder.clear();
    }
    
    /*amal*/
    void updateOtherTenants(String tenant, Role role, Permission permission, HttpMethod httpMethod) {
        TenantHolder.setTenantId(role.getTenant());
        logger.debug("====================Updating other tenants");

        String entityType = "role";
        Long roleId = null, permissionId = null;

        try {
            List<AppTenant> tenantList = httpService.getTenants();

            for (AppTenant appTenant : tenantList) {
               // TimeUnit.SECONDS.sleep(4);
                if (!appTenant.getTenantId().equalsIgnoreCase(tenant)) {
                    logger.debug("===============Updating tenant " + appTenant.getTenantId());

                    //send request to get permission by name (to get id and version
                    Role otherTenantRole = httpService.getOtherTenantEntity(appTenant.getTenantId(), Role.class, "role", role.getName());
                    roleId = otherTenantRole.getId();

                    Permission otherTenantPermission = httpService.getOtherTenantEntity(appTenant.getTenantId(), Permission.class, "permission", permission.getName());
                    permissionId = otherTenantPermission.getId();

                    String subpath = "/" + roleId.toString() + "/permission/" + permissionId.toString();

                    //send request to update tenant
                    ResponseEntity responseEntity =httpService.sendOtherTenantUpdateRequest(appTenant.getTenantId(), entityType, null, httpMethod, subpath);
                    System.out.println("=================UPDATED OTHER TENANTS : " + subpath + " , tenant:" + tenant);
                    
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();

        }
        TenantHolder.clear();
    }
    
    void deleteLoginTenants(String tenant, Role role, Permission permission, HttpMethod httpMethod) {
        TenantHolder.setTenantId(tenant);
        logger.debug("====================Updating other tenants");

        String entityType = "role";
        Long roleId = null, permissionId = null;

        try {
            logger.debug("===============Updating tenant " + tenant);

            //send request to get permission by name (to get id and version
            Role otherTenantRole = httpService.getOtherTenantEntity(tenant, Role.class, "role", role.getName());
            roleId = otherTenantRole.getId();

            Permission otherTenantPermission = httpService.getOtherTenantEntity(tenant, Permission.class, "permission", permission.getName());
            permissionId = otherTenantPermission.getId();

            String subpath = "/" + roleId.toString() + "/permission/" + permissionId.toString();

            //send request to update tenant
            ResponseEntity responseEntity =httpService.sendOtherTenantUpdateRequest(tenant, entityType, null, httpMethod, subpath);
            System.out.println("=================UPDATED OTHER TENANTS : " + subpath + " , tenant:" + tenant);
            
            if (httpMethod.equals(HttpMethod.DELETE) && responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==200) {
            	deleteRoleAndPermission(role.getId(),permission.getId());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        TenantHolder.clear();
    }
    
    private void deleteRoleAndPermission(Long roleId, Long permissionId) {
    	Optional<RolePermission> rolePermission=rolePermissionRepository.findByRoleIdAndPermissionId(roleId,permissionId);
		if(rolePermission.isPresent()) {
			rolePermissionRepository.deleteByRoleAndPermissionId(roleId,permissionId);
		}
	}

	/*amal*/
    public RolePermission saveRolePermission(Long roleId, Long permissionId, String user, String tenent, boolean tenantUpdate) throws FXDefaultException{
        if (!roleRepository.existsById(roleId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (!permissionRepository.existsById(permissionId)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_PERM_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_ROLE_PERM"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Role role = roleRepository.findById(roleId).get();
        Permission permission = permissionRepository.findById(permissionId).get();

        RolePermission rolePermission = new RolePermission();
        RolePermissionId rolePermissionId = new RolePermissionId();
        rolePermissionId.setPermissionId(permissionId);
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermissionId.setRoleId(roleId);
        rolePermission.setRolePermissionId(rolePermissionId);

        //set created log
        rolePermission.setCreatedBy(user);
        //rolePermission.setCreatedOn(new Timestamp(new Date().getTime()));

        rolePermission = rolePermissionRepository.save(rolePermission);

        //temporary for async operations
        rolePermission.setTenant(tenent);
        if (role.getRolePermissions() != null) {
            role.getRolePermissions().add(rolePermission);
        } else {
            List<RolePermission> rolePermissionList = new ArrayList<>();
            rolePermissionList.add(rolePermission);
            role.setRolePermissions(rolePermissionList);
        }
        historyService.saveHistoryRecord(rolePermission, "CREATE");

        //Check if providerRole record is available to update
      /*  ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
        providerRoleIdClass.setRole(roleId);
        providerRoleIdClass.setProvider(PROVIDER);
        System.out.println("==========BEFORE ASYNC ROLE: "+ providerRoleRepository.existsById(providerRoleIdClass));
        System.out.println("==========PROVIDER ID: "+ providerRoleIdClass.getRole() + " : "+ providerRoleIdClass.getProvider());
        if (providerRoleRepository.existsById(providerRoleIdClass)) {
            ProviderRole providerRole =
                    providerRoleRepository.findById(providerRoleIdClass).get();

            String providerRoleId1 = providerRole.getProviderRoleId();

            String subPath = ROOT_PATH + "/" + providerRoleId1;
            //send async request to update auth0
          ***  asyncUpdateOperations(role, permission, HttpMethod.PUT, subPath, tenent, tenantUpdate, "assign", true);

        }*/

        return rolePermission;
    }
    
    /*added by Pasindu - 2021-11-29*/   
	/*@Override
	public List<String> getAllwithoutPageSize(Pageable pageable, String bookmarks, String search, Predicate predicate,
			String projection, String tenent) throws FXDefaultException {

		List<String> rolesList = new ArrayList<>();

		Iterable<Role> allRoles = roleRepository.findAll();
		for (Role role : allRoles) {
			rolesList.add(role.getName());
		}
		return rolesList;

	}*/
    
    @Override
	public Iterable<Role> getAllwithoutPageSize(Pageable pageable, String bookmarks, String search, Predicate predicate,
			String projection, String tenent) {

		Iterable<Role> allRoles = roleRepository.findByAuth0NotIn("Yes");

		return allRoles;

	}
	
	
    /*added by Pasindu - 2021-11-29 */
	
	/**
     * Get all Role as pageable from provider
     * Not used by any endpoint
     */
    @Override
    public List<?> getAllRoleFromAuth0(Pageable pageable, String bookmarks, String projection, String permissionId, String tenent) throws FXDefaultException {
        String dataType = ROOT_PATH;
        String subPath = dataType;
        subPath =   subPath;
        permissionId = null;
        
        //send get request for permissions and return page
        return httpService.sendProviderGetRequestAswithoutPage(subPath, pageable, RoleResponse.class, permissionId, dataType, RoleResponse.class);
    }

	@Override
	public void updateRole(Role role) {
		role.setAuth0("Yes");
		roleRepository.save(role);
	}

	@Override
	public Iterable<RolePermission> getPermissionByRoleId(Long roleId, String tenent) {
		List<RolePermission> list=rolePermissionRepository.findByRoleId(roleId);
		return list;
	}

	@Override
	public String findByProviderPermissionId(Long permissionId) {
		Optional<ProviderPermission> providerPermission =providerPermissionRepository.findByProviderPermissionIdClass_Permission(permissionId);
		return providerPermission.isPresent()?providerPermission.get().getProviderPermissionId():null;
	}

	@Override
	public ArrayList<String> assignAuto(String projection, String user, String tenent, boolean tenantUpdate, boolean notInAuth0)throws FXDefaultException {
		ArrayList<String> addedList=new ArrayList<>(); 
		
		List<RolePermission> list=rolePermissionRepository.findForAuth0();
		int listSize=list.size();
		
		for(RolePermission rolepermission:list) {
						
			Long roleId=rolepermission.getRole().getId();
			Long permissionId=rolepermission.getPermission().getId();
			
			System.out.print("roleid=   "+roleId+" permissionId= "+permissionId);
			
			logger.info("roleid=   "+roleId+" permissionId= "+permissionId);
			
			if (!roleRepository.existsById(roleId)) {
	            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_ROLE_ID"), new Date(), HttpStatus.BAD_REQUEST);
	        }

	        if (!permissionRepository.existsById(permissionId)) {
	            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_PERM_ID"), new Date(), HttpStatus.BAD_REQUEST);
	        }

	        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId) && !notInAuth0) {
	            throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_ROLE_PERM"), new Date(), HttpStatus.BAD_REQUEST);
	        }

	        Role role = roleRepository.findById(roleId).get();
	        Permission permission = permissionRepository.findById(permissionId).get();
	        
	        RolePermission rolePermission =null;
	        if(notInAuth0) {
	        	rolePermission=rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId).get();
	        }else {
	        	 rolePermission = new RolePermission();
	        }

	        RolePermissionId rolePermissionId = new RolePermissionId();
	        rolePermissionId.setPermissionId(permissionId);
	        rolePermission.setRole(role);
	        rolePermission.setPermission(permission);
	        rolePermissionId.setRoleId(roleId);
	        rolePermission.setRolePermissionId(rolePermissionId);

	        rolePermission.setCreatedBy(user);
	        rolePermission.setCreatedOn(new Timestamp(new Date().getTime()));

	        rolePermission = rolePermissionRepository.save(rolePermission);

	        rolePermission.setTenant(tenent);
	        if (role.getRolePermissions() != null) {
	            role.getRolePermissions().add(rolePermission);
	        } else {
	            List<RolePermission> rolePermissionList = new ArrayList<>();
	            rolePermissionList.add(rolePermission);
	            role.setRolePermissions(rolePermissionList);
	        }
	        historyService.saveRolePermissionHistoryRecord(rolePermission, "CREATE");

	        ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
	        providerRoleIdClass.setRole(roleId);
	        providerRoleIdClass.setProvider(PROVIDER);
	        System.out.println("==============assign permision BEFORE ASYNC ROLE: "+ providerRoleRepository.existsById(providerRoleIdClass));
	        System.out.println("==============assign permision PROVIDER ID: "+ providerRoleIdClass.getRole() + " : "+ providerRoleIdClass.getProvider());
	        if (providerRoleRepository.existsById(providerRoleIdClass)) {
	            ProviderRole providerRole =
	                    providerRoleRepository.findById(providerRoleIdClass).get();

	            String providerRoleId1 = providerRole.getProviderRoleId();

	            String subPath = ROOT_PATH + "/" + providerRoleId1;

	            updateOperationsForAuth0(role, permission, HttpMethod.PUT, subPath, tenent, tenantUpdate, "assign", true);
	            
	            /*try {
	            	updateRolePermission(roleId,permissionId);
				} catch (Exception e) {
					logger.info(e.toString());
				}*/

	        }
	        
	        StringBuffer sb=new StringBuffer();
			sb.append("Permission "+permissionId);
			sb.append(" / ");
			sb.append("Role "+roleId);
			sb.append(rolePermission.getPermission().getName()!=null?rolePermission.getPermission().getName():"");
			addedList.add(sb.toString());
		}
		return addedList;   
	}
	
	
	public void updateRolePermission(Long roleId,Long permissionId) {
		Optional<RolePermission> rolePermission=rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId);
		if(rolePermission.isPresent()) {
			RolePermission rolePerm=rolePermission.get();
			if(rolePerm.getAuth0()==null) {
				rolePerm.setAuth0("Yes");
				rolePermissionRepository.save(rolePerm);
			}
		}
	}

	@Override
	public Object createAutoRole(String user, String tenent, boolean tenantUpdate, boolean authUpdate, boolean notInAuth0)throws FXDefaultException {
		/*
		 * if (roleRepository.existsByName(addRole.getName()) && !notInAuth0) { throw
		 * new FXDefaultException("3002", "DUPLICATE",
		 * Translator.toLocale("DUPLICATE_ROLE"), new Date(), HttpStatus.BAD_REQUEST); }
		 */
		
		List<Role> roleList=roleRepository.findAllRole();
		
		for(Role role:roleList) {

	        if(notInAuth0) {
	        	role=roleRepository.findByName(role.getName()).get();
	        }else {
	        	role=new Role();
	        }
	        
	        role.setName(role.getName());
	        role.setDescription(role.getDescription());
	        role.setSearch(role.getName() + role.getDescription());
	        
	        System.out.println("=====================Create 1");
	        role.setCreatedBy(user);
	        role.setCreatedOn(new Timestamp(new Date().getTime()));

	        //save permission
	        //role = roleRepository.save(role);

	        //temporary - for async operations
	        role.setTenant(tenent);

	        System.out.println("=======================Updating tenants");
	        //send request to update auth0 and other tenants

	        //updateOperations(role, null, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, "crud", authUpdate);/*amal*/
	        
	        System.out.println("=========================DONE UPDATING PROCESS");
	        //save to history
	        
	        //historyService.saveHistoryRecord(role, "CREATE");
	        
	        System.out.println("END OF REQUEST");
		             
		}
		return null;
	}

	@Override
	public void updateRoleByName(String roleName) throws FXDefaultException {
		Optional<Role> role=roleRepository.findByName(roleName);
		if(role.isPresent()) {
			Role roleDate=role.get();
			roleDate.setAuth0("Yes");
			roleRepository.save(roleDate);
		}
	}
	
	@Override
	public void deleteRoleByName(String roleName) throws FXDefaultException {
		Optional<Role> role=roleRepository.findByName(roleName);
		if(role.isPresent()) {
			Role roleDate=role.get();
			roleDate.setDeleted("Yes");
			roleRepository.save(roleDate);
		}
	}

	@Override
	public Role getRoleByName(String roleName) throws FXDefaultException {
		Optional<Role> role=roleRepository.findByName(roleName);
		return role.isPresent()?role.get():null;
	}

	@Override
	public void updateRoleByuid(String correctId, String wrongId) throws FXDefaultException {
		providerRoleRepository.updateRoleByuid(correctId,wrongId);
	}

	@Override
	public void addProviderRecord(String providerId, Role role) throws FXDefaultException {
		try {
			ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass();
            providerRoleIdClass.setProvider(PROVIDER);
            providerRoleIdClass.setRole(role.getId());
            
            ProviderRole providerRole = new ProviderRole();
            providerRole.setProviderRoleIdClass(providerRoleIdClass);
            providerRole.setProviderRoleId(providerId);
            
            LoggerRequest.getInstance().logInfo("=============provider saved :"+ providerRole.getRole());
            
            providerRoleRepository.save(providerRole);
            
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		
	}

	@Override
	@Async
	public void saveAuth0Mapping(String tenent,String user) {
		try {
			List PermissionListFromAuth0 = getAllRoleFromAuth0(null, null, null, null, tenent);
    		
	    	String applicationId=null;
			try {
				applicationId = permissionService.getApplicationIdByTenant(tenent);
			} catch (Exception e) {
				LoggerRequest.getInstance().logInfo("Exception "+e.toString());
			}

			ListIterator<LinkedHashMap> RoleFromAuth0ListIterator = PermissionListFromAuth0.listIterator();

			while (RoleFromAuth0ListIterator.hasNext()) {
				LinkedHashMap rolePremissionFromAuth0 = RoleFromAuth0ListIterator.next();

				if (applicationId!=null && applicationId.equals(rolePremissionFromAuth0.get("applicationId"))) {	
					String providerRoleId=(String) rolePremissionFromAuth0.get("_id");
					
					Long providerId =providerRoleRepository.findByProviderRoleProviderRoleIdLong(providerRoleId);
					Long roleId=providerId!=null?providerId:null;
					
					ArrayList<String> providePermissionIdList=(ArrayList<String>) rolePremissionFromAuth0.get("permissions");
					
					for(String providerPermissionId:providePermissionIdList) {
						Long permissionId =providerPermissionRepository.findByProviderPermissionProviderPermissionIdLong(providerPermissionId);
						
						//if (rolePermissionDumpRepository.existsByRoleIdAndPermissionId(roleId, permissionId).isPresent()) {
				        //    continue;
				        //}
						
						PermissionRoleId rolePermissionId = new PermissionRoleId();
						RolePermissionDump rolePermissionDump=new RolePermissionDump();
						
				        rolePermissionId.setProviderPermissionId(providerPermissionId);
				        rolePermissionId.setProviderRoleId(providerRoleId);
				        
				        rolePermissionDump.setPermissionRoleId(rolePermissionId);
				        
				        rolePermissionDump.setRoleId(roleId);
				        rolePermissionDump.setPermissionId(permissionId);

				        rolePermissionDump.setCreatedBy(user);
				        rolePermissionDump.setCreatedOn(new Timestamp(new Date().getTime()));

				        rolePermissionDump = rolePermissionDumpRepository.save(rolePermissionDump);
					}
						
				}
			}
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		
	}

	@Override
	@Async
	@Transactional
	public void deleteAuth0Mapping(String tenent, String user) {
		try {
			rolePermissionDumpRepository.deleteAll();
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		
	}

	@Override
	public Iterable<Role> getAllDeletedRole() throws FXDefaultException {
		Iterable<Role> allRole = roleRepository.findByDeleted();
		return allRole;
	}

	@Override
	public void deleteRole(Role role) throws FXDefaultException {
		try {
			ProviderRoleIdClass providerRoleIdClass = new ProviderRoleIdClass(role.getId(), PROVIDER);
			providerRoleRepository.deleteById(providerRoleIdClass);
			roleRepository.deleteById(role.getId());
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		
	}

	@Override
	public Iterable<?> getAllForExport(String bookmarks, Predicate predicate, String projection, String tenent) {
		List<RoleDto> roleDto=new ArrayList<RoleDto>();
		
		List<Object[]> allRole=roleRepository.findByAllRole();
		
		for(Object[] role :allRole) {
			RoleDto dto=new RoleDto();
			dto.setName(role[0]!=null?String.valueOf(role[0]):null);
			dto.setDescription(role[1]!=null?String.valueOf(role[1]):null);
			dto.setSearch(role[2]!=null?String.valueOf(role[2]):null);
			dto.setCreatedBy(role[3]!=null?String.valueOf(role[3]):null);
			dto.setId(role[4]!=null?(Long)(role[4]):null);
			
			roleDto.add(dto);
			
		}
		 return roleDto;
	}

	@Override
	@Async
	public void rolePermissionMapping(String userName,String tenent,String processCode) {
		TenantHolder.setTenantId(tenent);
		
		moduleService.insertProcessLog(processCode,userName);
		
		saveAuth0Mapping(tenent,userName);
		
		Iterable<RolePermission> dbRolePermissionList = rolePermissionService.findForAuth0ISNull();
		
		String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		
		
		for(RolePermission rolePermission:dbRolePermissionList){
			//List PermissionListFromAuth0 = rolePermissionService.getAllPermissionFromAuth0(pageable, bookmarks,rolePermission.getRole().getId().toString(),tenent);
		//ListIterator<LinkedHashMap> PermissionFromAuth0ListIterator = PermissionListFromAuth0.listIterator();
			TenantHolder.setTenantId(tenent);
		
		if(rolePermission.getRole()!=null && rolePermission.getPermission()!=null && rolePermission.getRole().getId()!=null && rolePermission.getPermission().getId()!=null) {
			if(rolePermissionDumpRepository.existsByRoleIdAndPermissionId(rolePermission.getRole().getId(),rolePermission.getPermission().getId()).isPresent()) {
				try {
					updateRolePermission(rolePermission.getRole().getId(),rolePermission.getPermission().getId());
				} catch (Exception e) {
					LoggerRequest.getInstance().logInfo("Exception "+e.toString());
			}
		}else {
			/*StringBuffer sb=new StringBuffer();
		sb.append(rolePermission.getRole().getName()!=null?rolePermission.getRole().getName():"");
		sb.append(" / ");
		sb.append(rolePermission.getPermission().getName()!=null?rolePermission.getPermission().getName():"");
		missingPermissionList.add(sb.toString());*/
		
		try {
			
			assignAsync(null,rolePermission.getRole().getId(), rolePermission.getPermission().getId(), userName, tenent, false,true);
		} catch (FXDefaultException e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
				}
			}
		}
		
		/*while (PermissionFromAuth0ListIterator.hasNext()) {
			LinkedHashMap roleFromAuth0 = PermissionFromAuth0ListIterator.next();
			
			if (roleFromAuth0!=null && rolePermission.getRole()!=null && rolePermission.getRole().getName().equals(roleFromAuth0.get("name")) && applicationId!=null && applicationId.equals(roleFromAuth0.get("applicationId"))) {
				
				ArrayList<String> list=(ArrayList<String>) roleFromAuth0.get("permissions");
				
				String permissionId=rolePermission.getPermission()!=null?String.valueOf(rolePermission.getPermission().getId()):null;
				
				if(permissionId!=null) {
					String providerPermissionId=roleService.findByProviderPermissionId(Long.parseLong(permissionId));
				
					if (((list==null||list.isEmpty()) && rolePermission.getPermission().getId()!=null && rolePermission.getRole().getId()!=null) || !(list.contains(providerPermissionId))) {
						StringBuffer sb=new StringBuffer();
						sb.append(permissionId);
						sb.append(" / ");
						sb.append(rolePermission.getPermission().getName()!=null?rolePermission.getPermission().getName():"");
						missingPermissionList.add(sb.toString());
						
						roleService.assign(projection,rolePermission.getRole().getId(), Long.parseLong(permissionId), "sysUser", tenent, false,true);
						
					}else if (!(list==null || list.isEmpty()) && (list.contains(providerPermissionId))) {
						try {
							roleService.updateRolePermission(rolePermission.getRole().getId(),rolePermission.getPermission().getId());
						} catch (Exception e) {
							LoggerRequest.getInstance().logInfo("Exception "+e.toString());
						}
					}
				}
			}
		
		}*/
			
		}
		TenantHolder.setTenantId(tenent);
		
		deleteAuth0Mapping(tenent,userName);
		
		moduleService.deleteProcessLog(processCode);
		
		TenantHolder.clear();
	}

	@Override
	@Async
	public void syncRole(String userName, String tenent, String processCode) {
		TenantHolder.setTenantId(tenent);
		
		moduleService.insertProcessLog(processCode,userName!=null?userName:"sysUser");
		
		Iterable<Role> dbRoleList = getAllwithoutPageSize(null, null, null, null, null,tenent);

		List RoleListFromAuth0 =null;
		try {
			RoleListFromAuth0 = getAllRoleFromAuth0(null, null, null, null, tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		
		String applicationId=null;
		try {
			applicationId = permissionService.getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		
		List<AddRole> roleDtoList = new ArrayList<AddRole>();
		
		for(Role role:dbRoleList){

			boolean foundOnAuth0 = false;
			ListIterator<LinkedHashMap> RoleFromAuth0ListIterator = RoleListFromAuth0.listIterator();

			while (RoleFromAuth0ListIterator.hasNext()) {
				LinkedHashMap roleFromAuth0 = RoleFromAuth0ListIterator.next();
				if (roleFromAuth0 != null) {

					if (role.getName().equals(roleFromAuth0.get("name")) && applicationId!=null && applicationId.equals(roleFromAuth0.get("applicationId"))) {
						foundOnAuth0 = true;
						LoggerRequest.getInstance().logInfo("CHECK_TRUE" +"permission_name "+roleFromAuth0.get("name")+"application_Name"+roleFromAuth0.get("applicationId"));
						try {
							updateRole(role);
						} catch (Exception e) {
							LoggerRequest.getInstance().logInfo("Exception "+e.toString());
						}
					}
				}
			}

			if (!foundOnAuth0 && applicationId!=null) {
				AddRole roleRequest = new AddRole();
				roleRequest.setName(role.getName());
				roleRequest.setDescription(role.getDescription());
				
				/*List<RolePermission> list=role.getRolePermissions();
				List<String> permisionStrList = new ArrayList<>();
				for(RolePermission rolePermission:list) {
					if(rolePermission.getPermission()!=null)
						permisionStrList.add(String.valueOf(rolePermission.getPermission().getId()));
				}
				roleRequest.setPermissions(permisionStrList.size()>0?permisionStrList.toArray(new String[0]):null);*/
				roleDtoList.add(roleRequest);
			}

		}
		if(roleDtoList.size()>0) {
			for(AddRole role:roleDtoList) {
				TenantHolder.setTenantId(tenent);
				try {
					create(null, role, userName!=null?userName:"sysUser", tenent, false, true,true);
				} catch (Exception e) {
					LoggerRequest.getInstance().logInfo("Exception_CREATE "+e.toString());
				}
							
			}
		}
		TenantHolder.setTenantId(tenent);
		moduleService.deleteProcessLog(processCode);
		
	}

	@Override
	public void updateRolesAuth0Field(Long id, String username, String tenent) throws Exception {
		try {
			Optional<Role> role=roleRepository.findById(id);
			if(role.isPresent()) {
				Role roleDate=role.get();
				roleDate.setAuth0(null);
				roleRepository.save(roleDate);
			}
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("updateRolesAuth0Field "+e.toString());
		}
		
	}

	@Override
	public void updateRolesPermissionAuth0Field(Long roleId, Long permissionId, String username, String tenent) {
		try {
			Optional<RolePermission> rolePermission=rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId);
			if(rolePermission.isPresent()) {
				RolePermission rolePermissionObj=rolePermission.get();
				rolePermissionObj.setAuth0(null);
				rolePermissionRepository.save(rolePermissionObj);
			}
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("updateRolesAuth0Field "+e.toString());
		}
		
	}

	@Override
	@Async
	public void missingProviderRole(String userName, String tenent, String processCode) throws FXDefaultException {
		TenantHolder.setTenantId(tenent);
		
		moduleService.insertProcessLog(processCode,userName);
		
		ArrayList<String> providerRoleList=new ArrayList<>();
    	
		List RoleListFromAuth0=null;
		try {
			RoleListFromAuth0 = getAllRoleFromAuth0(null, null, null, null, tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
    	
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
				if (roleFromAuth0.get("_id")!=null && roleFromAuth0.get("name")!=null && applicationId!=null && applicationId.equals(roleFromAuth0.get("applicationId"))) {
					
					Role role=getRoleByName(roleFromAuth0.get("name").toString());
					
					if(role!=null && role.getId()!=null) {
						
						Optional<ProviderRole> providerRoleOp =providerRoleRepository.findByProviderRoleProviderRoleId(roleFromAuth0.get("_id").toString());
						
						if(!providerRoleOp.isPresent()) {
							providerRoleList.add(role.getName()!=null?role.getName():"");
							
							addProviderRecord(roleFromAuth0.get("_id").toString(),role);
							
						}
					}
					
				}
			}
		}
		moduleService.deleteProcessLog(processCode);
		TenantHolder.clear();
	}
	  
}
