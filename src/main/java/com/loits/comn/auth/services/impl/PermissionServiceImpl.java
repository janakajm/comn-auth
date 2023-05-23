package com.loits.comn.auth.services.impl;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.loits.comn.auth.commons.AsyncTaskDef;
import com.loits.comn.auth.commons.NullAwareBeanUtilsBean;
import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.LoggerRequest;
import com.loits.comn.auth.domain.AsyncSubTask;
import com.loits.comn.auth.domain.AsyncTask;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.domain.ProviderPermission;
import com.loits.comn.auth.domain.ProviderPermissionIdClass;
import com.loits.comn.auth.domain.QPermission;
import com.loits.comn.auth.domain.Role;
import com.loits.comn.auth.dto.AddPermission;
import com.loits.comn.auth.dto.AppTenant;
import com.loits.comn.auth.dto.PermissionRequest;
import com.loits.comn.auth.dto.PermissionResponse;
import com.loits.comn.auth.dto.Scope;
import com.loits.comn.auth.dto.ScopesRequest;
import com.loits.comn.auth.dto.TenantAuthProvider;
import com.loits.comn.auth.dto.UpdatePermission;
import com.loits.comn.auth.helper.BulkUploadResponse;
import com.loits.comn.auth.mt.TenantHolder;
import com.loits.comn.auth.repo.AsyncTaskRepository;
import com.loits.comn.auth.repo.PermissionRepository;
import com.loits.comn.auth.repo.ProviderPermissionRepository;
import com.loits.comn.auth.repo.RolePermissionRepository;
import com.loits.comn.auth.services.AsyncTaskService;
import com.loits.comn.auth.services.HistoryService;
import com.loits.comn.auth.services.HttpService;
import com.loits.comn.auth.services.ModuleService;
import com.loits.comn.auth.services.PermissionService;
import com.loits.comn.auth.services.projections.PermissionProjection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

@Service
public class PermissionServiceImpl implements PermissionService {

    Logger logger = LogManager.getLogger(PermissionServiceImpl.class);

    @Autowired
    ProjectionFactory projectionFactory;

    @Autowired
    HttpService httpService;

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    HistoryService historyService;

    @Autowired
    RolePermissionRepository rolePermissionRepository;

    @Autowired
    ProviderPermissionRepository providerPermissionRepository;

    @Autowired
    Executor executor;

    @Autowired
    AsyncTaskRepository asyncTaskRepository;

    @Autowired
    AsyncTaskService asyncTaskService;

    @Value("${auth.provider}")
    private String PROVIDER;
    
    @Autowired
    ModuleService moduleService;

    private static final String ROOT_PATH = "permissions";

    @Override
    public Page<?> getAll(Pageable pageable, String bookmarks, String search, Predicate predicate, String projection, String tenent) throws FXDefaultException {
        BooleanBuilder bb = new BooleanBuilder(predicate);
        QPermission permission = QPermission.permission;

        //split and separate ids sent as a string
        if (!StringUtils.isEmpty(bookmarks)) {
            ArrayList<Long> ids = new ArrayList<>();
            for (String id : bookmarks.split(",")) {
                ids.add(Long.parseLong(id));
            }
            bb.and(permission.id.in(ids));
        }

        //filter permissions with comn module
        bb.and(permission.name.toLowerCase().notLike("comn:branch:%"));

        //search by name on demand
        if (search != null && !search.isEmpty()) {
            bb.and(permission.name.containsIgnoreCase(search)).or(permission.meta1.containsIgnoreCase(search));
        }

        //update pageable object to ignorecase in sorting
        Sort sort = pageable.getSort();
        sort.forEach(order -> order.ignoreCase());
        PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return permissionRepository.findAll(bb.getValue(), pageRequest).map(
                permission1 -> projectionFactory.createProjection(PermissionProjection.class, permission1)
        );
    }

    /**
     * Get all permissions as pageable from provider
     * Not used by any endpoint
     */
    @Override
    public Page<?> getAllFromExtension(Pageable pageable, String bookmarks, String projection, String permissionId, String tenent) throws FXDefaultException {
        String dataType = ROOT_PATH;
        String subPath = dataType;
        subPath = permissionId != null ? dataType + "/" + permissionId : subPath;

        //send get request for permissions and return page
        return httpService.sendProviderGetRequestAsPage(subPath, pageable, PermissionResponse.class, permissionId, dataType, PermissionResponse.class);
    }

    @Override
    public Object getOne(String projection, String tenent, Long id) throws FXDefaultException {
        if (!permissionRepository.existsById(id)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_PERM_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        //Get permission by id from database
        Permission permission = permissionRepository.findById(id).get();
        return projectionFactory.createProjection(PermissionProjection.class, permission);
    }

    @Override
    public Object create(String projection, AddPermission addPermission, String user, String tenent, boolean tenantUpdate,boolean notExistInAuth0) throws FXDefaultException {

        if (permissionRepository.findByNameContainingIgnoreCase(addPermission.getName()).isPresent() && !notExistInAuth0) {
            throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_PERM"), new Date(), HttpStatus.BAD_REQUEST);
        }
        
        Permission permission=null;
        if(notExistInAuth0) {
        	LoggerRequest.getInstance().logInfo("EXIST");
        	permission=permissionRepository.findByName(addPermission.getName());
        }else {
        	permission = new Permission();
        	LoggerRequest.getInstance().logInfo("NOT_EXIST");
        }
        //copy properties from dto to Permission object
  
        permission.setName(addPermission.getName());
        permission.setDescription(addPermission.getDescription());
        if (addPermission.getMeta() != null) {
        	LoggerRequest.getInstance().logInfo("META_DATA "+addPermission.getMeta());
            permission.setSearch(addPermission.getName() + addPermission.getDescription() + addPermission.getMeta().getSearchq());
        }
        permission.setCreatedBy(user);
        permission.setCreatedOn(new Timestamp(new Date().getTime()));
        
        
        //TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenent, PROVIDER);
        
        //permission.setApplicationId(tenantAuthProvider.getClientId());
        //permission.setTenantId(tenent);

        //save permission to db
        permission = permissionRepository.save(permission);

        if (addPermission.getMeta() != null) {
            permission.setMeta1(addPermission.getMeta().getSearchq());
        }
        //temporary - for async operations
        permission.setTenant(tenent);

        //send async request to update auth0
        //asyncUpdateOperations(permission, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, true);
        
        try {/*amal*/
            updateOperations(permission,user, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, true);
         } catch (Exception exception) {
             exception.printStackTrace();
         }

        //save to history
        historyService.saveHistoryRecord(permission, "CREATE");

        return projectionFactory.createProjection(PermissionProjection.class, permission);
    }
    
    @Override
    public Object create(String projection, Permission permission, String user, String tenent, boolean tenantUpdate,boolean notExistInAuth0) throws FXDefaultException {

        if (permissionRepository.existsByName(permission.getName()) && !notExistInAuth0) {
            throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_PERM"), new Date(), HttpStatus.BAD_REQUEST);
        }
        
        permission.setCreatedBy(user);
        permission.setCreatedOn(new Timestamp(new Date().getTime()));
        permission.setTenant(tenent);
        
        //save permission to db
        permission = permissionRepository.save(permission);
   
        try {
            updateOperations(permission,user, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, true);
        	System.out.println("");
         } catch (Exception exception) {
        	 LoggerRequest.getInstance().logInfo("Exception "+exception.toString());
         }
        //save to history
        historyService.saveHistoryRecord(permission, "CREATE");

        return projectionFactory.createProjection(PermissionProjection.class, permission);
    }
    
    @Override
    public Object createPermission(String projection, Permission permission, String user, String tenent, boolean tenantUpdate,boolean notExistInAuth0) throws FXDefaultException {

        if (permissionRepository.existsByName(permission.getName()) && !notExistInAuth0) {
            throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_PERM"), new Date(), HttpStatus.BAD_REQUEST);
        }
        
        permission.setCreatedBy(user);
        permission.setCreatedOn(new Timestamp(new Date().getTime()));
        permission.setTenant(tenent);
        
        //save permission to db
        permission = permissionRepository.save(permission);
   
        try {
            updateOperations(permission,user, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, true);
        	System.out.println("");
         } catch (Exception exception) {
        	 LoggerRequest.getInstance().logInfo("Exception "+exception.toString());
         }
        //save to history
        historyService.savePermissionHistoryRecord(permission, "CREATE",tenent);

        return projectionFactory.createProjection(PermissionProjection.class, permission);
    }

    @Override
    public Object update(String projection, Long id, UpdatePermission updatePermission, String user, String tenent, boolean tenantUpdate) throws FXDefaultException {
        if (!permissionRepository.existsById(id)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_PERM_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Permission permission = permissionRepository.findById(id).get();

        if (!updatePermission.getVersion().equals(permission.getVersion()) && tenantUpdate) {
            throw new FXDefaultException("3003", "VERSION_MISMATCH", Translator.toLocale("VERSION_MISMATCH"), new Date(), HttpStatus.BAD_REQUEST);
        }

        //add new description to db object
        permission.setDescription(updatePermission.getDescription());
        permission.setSearch(permission.getName() + updatePermission.getDescription() + permission.getMeta1());

        //update audit log
        permission.setCreatedOn(new Timestamp(new Date().getTime()));
        permission.setCreatedBy(user);


        permission = permissionRepository.save(permission);

        //temporary - for async operations
        permission.setTenant(tenent);

        historyService.saveHistoryRecord(permission, "UPDATE");

        //If provider-permission exists, get provider-permission id
        ProviderPermissionIdClass providerPermissionIdClass = new ProviderPermissionIdClass();
        providerPermissionIdClass.setPermission(id);
        providerPermissionIdClass.setProvider(PROVIDER);
        if (providerPermissionRepository.existsById(providerPermissionIdClass)) {
            ProviderPermission providerPermission =
                    providerPermissionRepository.findById(providerPermissionIdClass).get();

            String providerPermissionId1 = providerPermission.getProviderPermissionId();

            String subPath = ROOT_PATH + "/" + providerPermissionId1;
            
            //send async request to update auth0
            //asyncUpdateOperations(permission, HttpMethod.PUT, subPath, tenent, tenantUpdate, true);
            try {/*amal*/
                updateOperations(permission,user, HttpMethod.PUT, subPath, tenent, tenantUpdate, true);
             } catch (Exception exception) {
                 exception.printStackTrace();
             }
        }
        return projectionFactory.createProjection(PermissionProjection.class, permission);
    }

    @Override
    public Object delete(String projection, Long id, String user, String tenent, boolean tenantUpdate) throws FXDefaultException {
        if (!permissionRepository.existsById(id)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("INVALID_PERM_ID"), new Date(), HttpStatus.BAD_REQUEST);
        }

        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw new FXDefaultException("3001", "INVALID_ATTEMPT", Translator.toLocale("PERM_FK"), new Date(), HttpStatus.BAD_REQUEST);
        }

        Permission permission = permissionRepository.findById(id).get();

        //permissionRepository.deleteById(permission.getId());

        //temporary - for async operations
        permission.setTenant(tenent);

        historyService.saveHistoryRecord(permission, "DELETE");

        //If provider-permission exists, get provider-permission id
        ProviderPermissionIdClass providerPermissionIdClass = new ProviderPermissionIdClass(id, PROVIDER);
        if (providerPermissionRepository.existsById(providerPermissionIdClass)) {
            ProviderPermission providerPermission =providerPermissionRepository.findById(providerPermissionIdClass).get();

            String providerPermissionId1 = providerPermission.getProviderPermissionId();

            String subPath = ROOT_PATH + "/" + providerPermissionId1;
            //asyncUpdateOperations(permission, HttpMethod.DELETE, subPath, tenent, tenantUpdate, true);
            try {/*amal*/
                updateOperations(permission,user, HttpMethod.DELETE, subPath, tenent, tenantUpdate, true);
             } catch (Exception exception) {
                 exception.printStackTrace();
             }
            
            //providerPermissionRepository.deleteById(providerPermissionIdClass);
        }
        return projectionFactory.createProjection(PermissionProjection.class, permission);
    }

    @Override
    public Iterable<?> getAllForExport(String bookmarks, Predicate predicate, String projection, String tenent) {

        return permissionRepository.findAll();
    }

    @Override
    public ResponseEntity updateAuthAPI(String projection, String user, String tenent) throws FXDefaultException {
        //TODO optimize to get from pageable?
        //get all permissions for tenant
        Iterable<Permission> permissions = permissionRepository.findAll();

        //Create list of scopes
        List<Scope> scopes = new ArrayList<>();
        permissions.forEach(permission -> {
            Scope scope = new Scope();
            scope.setValue(permission.getName());
            scope.setDescription(permission.getDescription());
            scopes.add(scope);
        });

        //Create RequestBody entity
        ScopesRequest scopesRequest = new ScopesRequest();
        scopesRequest.setScopes(scopes);
        //update Auth0 API
        return httpService.updatePermissionsinProviderApi(scopesRequest);

    }

    @Override
    public Object bulkCreate(String projection, List<AddPermission> addPermissionList, String user, String tenent, boolean tenantUpdate, boolean authUpdate,boolean notExistInAuth0) {
    	 System.out.println("========================Bulk Permission creation process started");
        BulkUploadResponse response = new BulkUploadResponse();
        List<Permission> permissionList = new ArrayList<>();
        List<BulkUploadResponse.Error> errorData = new ArrayList<>();

        HashSet<String> nameSet = new HashSet<>();

        addPermissionList.forEach(addPermission -> {
            if (!nameSet.contains(addPermission.getName())) {
                nameSet.add(addPermission.getName());

                try {
                    if (permissionRepository.findByNameContainingIgnoreCase(addPermission.getName()).isPresent() && !notExistInAuth0) {
                        throw new FXDefaultException("3002", "DUPLICATE", Translator.toLocale("DUPLICATE_PERM"), new Date(), HttpStatus.BAD_REQUEST);
                    }
                    Permission permission=null;
                    
                    if(notExistInAuth0) {
                    	permission=permissionRepository.findByName(addPermission.getName());
                    }else {
                    	permission = new Permission();
                    }
                    
                    //copy properties from response to Permission object
                    
                    permission.setName(addPermission.getName());
                    permission.setDescription(addPermission.getDescription());
                    permission.setCreatedBy(user);
                    permission.setCreatedOn(new Timestamp(new Date().getTime()));
                    if (addPermission.getMeta() != null) {
                        permission.setSearch(addPermission.getName() + addPermission.getDescription() + addPermission.getMeta().getSearchq());
                    }

                    //temporary - for async operations
                    permission.setTenant(tenent);
                    
                    TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenent, PROVIDER);
                    
                    //permission.setApplicationId(tenantAuthProvider.getClientId());
                    //permission.setTenantId(tenent);

                    permissionList.add(permission);

                } catch (FXDefaultException e) {
                	 System.out.println("===============Bulk creation record failed for permission with name " + addPermission.getName());
                    e.printStackTrace();
                    BulkUploadResponse.Error err = new BulkUploadResponse.Error();
                    err.setData(addPermission);
                    err.setError(e.getErrorCode());
                    err.setErrorDescription(e.getMessage());
                    errorData.add(err);
                }
            }
        });

        // save all permissions
        Iterable<Permission> resp = permissionRepository.saveAll(permissionList);
        // save autdit trail
        historyService.saveBulkHistoryRecord(permissionList, "CREATE");
        List<Object> exportData = new ArrayList<>();
        resp.forEach(permission -> {
            // prep export list by adding projection
            exportData.add(projectionFactory.createProjection(PermissionProjection.class, permission));

            //update the authorization extension
            //asyncUpdateOperations(permission, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, authUpdate);
            try {/*amal*/
                updateOperations(permission,user, HttpMethod.POST, ROOT_PATH, tenent, tenantUpdate, authUpdate);
             } catch (Exception exception) {
                 exception.printStackTrace();
             }

        });

        response.setData(exportData);
        response.setErrorData(errorData);
        response.setTotalRecords(addPermissionList.size());
        response.setSuccessCount(exportData.size());
        System.out.println("==============Permissions bulk creation completed. No of successful records : "
                + exportData.size());

        return response;
    }

//    @Async
//    CompletableFuture<?> asyncUpdateOperations(List<Permission> permissionList, HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate) {
//        return CompletableFuture.runAsync(() -> {
//            TenantHolder.setTenantId(tenant);
//            ArrayList<CompletableFuture<?>> futureList = new ArrayList<>();
//
//            // Start new Async task
//            AsyncTask asyncTask =
//                    this.asyncTaskService.saveTask(new AsyncTask(), String.valueOf(""),
//                            AsyncTaskDef.TASK_INITIATED,
//                            AsyncTaskDef.Task.NEW_PERMISSION_CREATION, null);
//
//
//            futureList.add(updateAuth0(permissionList, httpMethod, subpath, tenant, asyncTask));
//
//            //Create/Delete/Update permission in other tenants if tenantUpdate is enabled
//            if (tenantUpdate) {
//                futureList.add(updateOtherTenants(tenant, permissionList, httpMethod, asyncTask));
//            }
//
//            CompletableFuture.allOf(
//                    futureList.toArray(new CompletableFuture[futureList.size()]))
//                    .whenComplete((result, ex) -> {
//                        try {
//                            final AsyncTask finishedTask =
//                                    asyncTaskRepository.findById(asyncTask.getId()).get();
//                            if (ex != null) {
//                                logger.error("Exception in Notification API invocation");
//                                ex.printStackTrace();
//                                this.asyncTaskService.saveTaskLog(finishedTask, "Notification API " +
//                                                "Invocation error",
//                                        ex.getMessage(), "APPROVE-NOTIFICATION",
//                                        "APPROVE-NOTIFICATION", "APPROVE-NOTIFICATION", new Exception(ex));
//                                this.asyncTaskService.saveTask(finishedTask, null, AsyncTaskDef.TASK_ERROR,
//                                        null,
//                                        null);
//                            } else {
//                                this.asyncTaskService.saveTask(finishedTask, null, AsyncTaskDef.TASK_COMPLETED
//                                        , null
//                                        , null);
//                            }
//
//                        } catch (Exception e) {
//                            logger.error("Exception in Async Updates Auth0/ other tenants");
//                            this.asyncTaskService.saveTaskLog(asyncTask, "PERMISSION API " +
//                                            "handling async process failed", e.getMessage(), "AUTH0/TENANT" +
//                                            "-UPDATE ASYNC",
//                                    "AUTH/TENANT-UPDATE ASYNC", "AUTH/TENANT-UPDATE ASYNC", e);
//                            this.asyncTaskService.saveTask(asyncTask, null, AsyncTaskDef.TASK_ERROR, null,
//                                    null);
//                            e.printStackTrace();
//                        }
//                    });
//
//            TenantHolder.clear();
//        }, executor);
//    }

    @Override
    @Async
    public CompletableFuture<?> asyncUpdateOperations(Permission permission, HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate, boolean authUpdate) {
        System.out.println("TENANT "+ tenant);
        System.out.println("PERM "+ permission.getName());
        System.out.println("subpath "+ subpath);
        System.out.println("HTTP "+ httpMethod);
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenant);
            ArrayList<CompletableFuture<?>> futureList = new ArrayList<>();

            // Start new Async task
            AsyncTask asyncTask =
                    this.asyncTaskService.saveTask(new AsyncTask(), "Perm_"+String.valueOf(permission.getId()),
                            AsyncTaskDef.TASK_INITIATED,
                            AsyncTaskDef.Task.NEW_ASYNC_OPERATION, null);

            if(authUpdate) {
                System.out.println("update tenant");
                futureList.add(updateAuth0(permission, httpMethod, subpath, tenant, asyncTask));
            }
            //Create/Delete/Update permission in other tenants if tenantUpdate is enabled
            if (tenantUpdate) {
                System.out.println("update other tenants");
                futureList.add(updateOtherTenants(tenant, permission, httpMethod, asyncTask));
            }

            CompletableFuture.allOf(
                    futureList.toArray(new CompletableFuture[futureList.size()]))
                    .whenComplete((result, ex) -> {
                        try {
                            final AsyncTask finishedTask =
                                    asyncTaskRepository.findById(asyncTask.getId()).get();
                            if (ex != null) {
                                logger.error("Exception in Notification API invocation");
                                ex.printStackTrace();
                                this.asyncTaskService.saveTaskLog(finishedTask, "Notification API " +
                                                "Invocation error",
                                        ex.getMessage(), "APPROVE-NOTIFICATION",
                                        "APPROVE-NOTIFICATION", "APPROVE-NOTIFICATION", new Exception(ex));
                                this.asyncTaskService.saveTask(finishedTask, null, AsyncTaskDef.TASK_ERROR,
                                        null,
                                        null);
                            } else {
                                this.asyncTaskService.saveTask(finishedTask, null, AsyncTaskDef.TASK_COMPLETED
                                        , null
                                        , null);
                            }

                        } catch (Exception e) {
                            logger.error("Exception in Async Updates Auth0/ other tenants");
                            this.asyncTaskService.saveTaskLog(asyncTask, "PERMISSION API " +
                                            "handling async process failed", e.getMessage(), "AUTH0/TENANT" +
                                            "-UPDATE ASYNC",
                                    "AUTH/TENANT-UPDATE ASYNC", "AUTH/TENANT-UPDATE ASYNC", e);
                            this.asyncTaskService.saveTask(asyncTask, null, AsyncTaskDef.TASK_ERROR, null,
                                    null);
                            e.printStackTrace();
                        }
                    });

            TenantHolder.clear();
        }, executor);
    }

    @Override
    @Async
    public CompletableFuture<?> updateAuth0(Permission permission, HttpMethod httpMethod, String subpath, String tenant, AsyncTask at) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenant);
            logger.debug("Starting to update auth0 for permission " + permission.getId());

            HashMap<String, Object> meta = new HashMap<>();
            AsyncSubTask asyncSubTask =
                    this.asyncTaskService.saveSubTask(new AsyncSubTask(), at,
                            AsyncTaskDef.TASK_INITIATED,
                            AsyncTaskDef.Task.NEW_UPDATE_PROVIDER, null);

            PermissionRequest permissionRequest = null;
            ResponseEntity responseEntity = null;
            PermissionResponse permissionResponse = null;

            //Get clientId/ secret for default tenant from comn-system-info
            TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);

            //Create Request Entity for POST/PUT requests
            if (httpMethod != HttpMethod.DELETE) {
                permissionRequest = new PermissionRequest();
                NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
                try {
                    nullAwareBeanUtilsBean.copyProperties(permissionRequest, permission);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Set app id and app type
                permissionRequest.setApplicationId(tenantAuthProvider.getClientId());
                permissionRequest.setApplicationType("client");
            }

            //get Auth0 response
            try {
                System.out.println("SENDING HTTP REQUEST PERMISSIONS");
                responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, permissionRequest, PermissionResponse.class);

                ProviderPermissionIdClass providerPermissionIdClass = new ProviderPermissionIdClass();
                providerPermissionIdClass.setProvider(PROVIDER);
                providerPermissionIdClass.setPermission(permission.getId());

                //Create ProviderPermission record from POST response
                System.out.println("NEW PERMISSION PROVIDER ID for "+ providerPermissionIdClass.getPermission());
                System.out.println("NEW PERMISSION ID for "+ providerPermissionIdClass.getPermission());
                if (httpMethod.equals(HttpMethod.POST)) {
                    permissionResponse = (PermissionResponse) responseEntity.getBody();
                    ProviderPermission providerPermission = new ProviderPermission();
                    providerPermission.setProviderPermissionIdClass(providerPermissionIdClass);
                    providerPermission.setProviderPermissionId(permissionResponse.get_id());
                    System.out.println("SAVING PERMISSION PROVIDER ID : "+ providerPermissionIdClass.getPermission());
                    providerPermissionRepository.save(providerPermission);
                }
                logger.debug("Permission with id " + permission.getId() + ", "+permission.getName() + " was successfully updated in " + PROVIDER);
                meta.put("permission", permission);
                meta.put("permissionId", permission.getId());
                logger.debug("Data posted to Auth0");
                this.asyncTaskService.saveSubTask(asyncSubTask,
                        null,
                        AsyncTaskDef.TASK_COMPLETED, null, meta);
            } catch (Exception ex) {
                logger.debug("Permission with id " + permission.getId() + " was unable to be updated in " + PROVIDER);
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
    CompletableFuture<?> updateAuth0(List<Permission> permissionList, HttpMethod httpMethod, String subpath, String tenant, AsyncTask at) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenant);
            logger.debug("Starting to update auth0 for bulk permission");

            HashMap<String, Object> meta = new HashMap<>();


            //Get clientId/ secret for default tenant from comn-system-info
            TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);
            permissionList.forEach(permission -> {
                PermissionRequest permissionRequest = null;
                ResponseEntity responseEntity = null;
                PermissionResponse permissionResponse = null;
                //Create Request Entity for POST/PUT requests
                permissionRequest = new PermissionRequest();
                NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
                try {
                    nullAwareBeanUtilsBean.copyProperties(permissionRequest, permission);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Set app id and app type
                permissionRequest.setApplicationId(tenantAuthProvider.getClientId());
                permissionRequest.setApplicationType("client");

                AsyncSubTask asyncSubTask = null;
                //get Auth0 response
                try {
                    asyncSubTask =
                            this.asyncTaskService.saveSubTask(new AsyncSubTask(), at,
                                    AsyncTaskDef.TASK_INITIATED,
                                    AsyncTaskDef.Task.NEW_UPDATE_PROVIDER, null);
                    responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, permissionRequest, PermissionResponse.class);

                    ProviderPermissionIdClass providerPermissionIdClass = new ProviderPermissionIdClass();
                    providerPermissionIdClass.setProvider(PROVIDER);
                    providerPermissionIdClass.setPermission(permission.getId());

                    //Create ProviderPermission record from POST response
                    if (httpMethod.equals(HttpMethod.POST)) {
                        permissionResponse = (PermissionResponse) responseEntity.getBody();
                        ProviderPermission providerPermission = new ProviderPermission();
                        providerPermission.setProviderPermissionIdClass(providerPermissionIdClass);
                        providerPermission.setProviderPermissionId(permissionResponse.get_id());
                        providerPermissionRepository.save(providerPermission);
                    }
                    logger.debug("Permission with id " + permission.getId() + " was successfully updated in " + PROVIDER);
                    meta.put("permission", permission);
                    meta.put("permissionId", permission.getId());
                    logger.debug("Data posted to Auth0");


                    this.asyncTaskService.saveSubTask(asyncSubTask,
                            null,
                            AsyncTaskDef.TASK_COMPLETED, null, meta);
                } catch (Exception ex) {
                    logger.debug("Permission with id " + permission.getId() + " was unable to be updated in " + PROVIDER);
                    ex.printStackTrace();
                    this.asyncTaskService.saveSubTask(asyncSubTask,
                            null,
                            AsyncTaskDef.TASK_ERROR, null, null);
                    this.asyncTaskService.saveSubTaskLog(asyncSubTask, "Auth0 update error",
                            ex.getMessage(), "",
                            ex.getMessage(), "Auth0 update error",

                            new Exception(ex.getMessage()));
                }
            });
            TenantHolder.clear();
        }, executor);
    }


    @Async
    CompletableFuture<?> updateOtherTenants(String tenant, Permission permission, HttpMethod httpMethod, AsyncTask at) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(permission.getTenant());
            logger.debug("Updating other tenants");

            String entityType = "permission";
            Object object = null;
            String subpath = null;

            HashMap<String, Object> meta = new HashMap<>();

            AsyncSubTask asyncSubTask =
                    this.asyncTaskService.saveSubTask(new AsyncSubTask(), at,
                            AsyncTaskDef.TASK_INITIATED,
                            AsyncTaskDef.Task.NEW_UPDATE_OTHER_TENANTS, null);


            try {
                List<AppTenant> tenantList = httpService.getTenants();

                for (AppTenant appTenant : tenantList) {
                    if (!appTenant.getTenantId().equalsIgnoreCase(tenant)) {
                        logger.debug("Updating tenant " + appTenant.getTenantId());
                        if (httpMethod == HttpMethod.POST) {
                            //Create addPermission object
                            AddPermission addPermission = new AddPermission();
                            addPermission.setName(permission.getName());
                            addPermission.setDescription(permission.getDescription());
                            object = addPermission;

                        } else {
                            //send request to get permission by name (to get id and version
                            Permission otherTenantPermission = httpService.getOtherTenantEntity(appTenant.getTenantId(), Permission.class, entityType, permission.getName());
                            Long id = otherTenantPermission.getId();
                            subpath = "/" + id.toString();
                            if (httpMethod == HttpMethod.PUT) {
                                //Create updatePermission object
                                UpdatePermission updatePermission = new UpdatePermission();
                                updatePermission.setName(permission.getName());
                                updatePermission.setDescription(permission.getDescription());
                                updatePermission.setVersion(otherTenantPermission.getVersion());
                                object = updatePermission;
                            }
                        }

                        //send request to update tenant
                        httpService.sendOtherTenantUpdateRequest(appTenant.getTenantId(), entityType, object, httpMethod, subpath);

                    }
                }
                meta.put("permission", permission);
                meta.put("permissionId", permission.getId());
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
    CompletableFuture<?> updateOtherTenants(String tenant, List<Permission> permissionList, HttpMethod httpMethod, AsyncTask at) {
        return CompletableFuture.runAsync(() -> {
            TenantHolder.setTenantId(tenant);
            logger.debug("Updating other tenants");


            String entityType = "permission";
            Object object = null;
            String subpath = null;

            HashMap<String, Object> meta = new HashMap<>();

            AsyncSubTask asyncSubTask =
                    this.asyncTaskService.saveSubTask(new AsyncSubTask(), at,
                            AsyncTaskDef.TASK_INITIATED,
                            AsyncTaskDef.Task.NEW_UPDATE_OTHER_TENANTS, null);

            List<AppTenant> tenantList = httpService.getTenants();
            try {
                for (AppTenant appTenant : tenantList) {
                    if (!appTenant.getTenantId().equalsIgnoreCase(tenant)) {
                        logger.debug("Updating tenant " + appTenant.getTenantId());
                        List<AddPermission> addPermissionList = new ArrayList<>();
                        subpath = "/bulk-create";

                        permissionList.forEach(permission -> {
                            //Create addPermission object
                            AddPermission addPermission = new AddPermission();
                            addPermission.setName(permission.getName());
                            addPermission.setDescription(permission.getDescription());
                            if (addPermission.getMeta() != null) {
                                permission.setSearch(addPermission.getName() + addPermission.getDescription() + addPermission.getMeta().getSearchq());
                            }

                        });
                        object = addPermissionList;
                        //send request to update tenant
                        httpService.sendOtherTenantUpdateRequest(appTenant.getTenantId(), entityType, object, httpMethod, subpath);

                    }
                }

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

    /*amal*/
	@Override
	public Object updateAuth0(Permission permission, HttpMethod httpMethod, String subpath, String tenant)throws Exception {
		
		 System.out.println("=====================Starting to update auth0 for permission " + permission.getId());

        HashMap<String, Object> meta = new HashMap<>();

        PermissionRequest permissionRequest = null;
        ResponseEntity responseEntity = null;
        PermissionResponse permissionResponse = null;

        //Get clientId secret for default tenant from comn-system-info
        TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);

        //Create Request Entity for POST/PUT requests
        if (httpMethod != HttpMethod.DELETE) {
            permissionRequest = new PermissionRequest();
            NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
            try {
                nullAwareBeanUtilsBean.copyProperties(permissionRequest, permission);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Set app id and app type
            permissionRequest.setApplicationId(tenantAuthProvider.getClientId());
            permissionRequest.setApplicationType("client");
            
            //permission.setApplicationId(tenantAuthProvider.getClientId());
            //permission.setTenantId(tenant);
        }

           //get Auth0 response
            System.out.println("=====================SENDING HTTP REQUEST PERMISSIONS");
            responseEntity = httpService.sendProviderRestRequest(httpMethod, subpath, permissionRequest, PermissionResponse.class);
            
            LoggerRequest.getInstance().logInfo("=============Auth0 response " + responseEntity);


            if(httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
                permissionRepository.save(permission);
            }

            if (httpMethod.equals(HttpMethod.POST)) {
                ProviderPermissionIdClass providerPermissionIdClass = new ProviderPermissionIdClass();
                providerPermissionIdClass.setProvider(PROVIDER);
                providerPermissionIdClass.setPermission(permission.getId());
                //Create ProviderPermission record from POST response
                System.out.println("===================NEW PERMISSION PROVIDER ID for "+ providerPermissionIdClass.getPermission());
                System.out.println("===================NEW PERMISSION ID for "+ providerPermissionIdClass.getPermission());
                permissionResponse = (PermissionResponse) responseEntity.getBody();
                ProviderPermission providerPermission = new ProviderPermission();
                providerPermission.setProviderPermissionIdClass(providerPermissionIdClass);
                providerPermission.setProviderPermissionId(permissionResponse.get_id());
                System.out.println("====================SAVING PERMISSION PROVIDER ID : "+ providerPermissionIdClass.getPermission());
                providerPermissionRepository.save(providerPermission);
                
                if(responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==200) {
                	updatePermissionByName(permission.getName());
                }
            }
            
            if (httpMethod.equals(HttpMethod.DELETE) && responseEntity!=null && responseEntity.getStatusCode()!=null && responseEntity.getStatusCodeValue()==204) {
            	deletePermissionByName(permission.getName());
            }
            
            System.out.println("===============Permission with id " + permission.getId() + ", "+permission.getName() + " was successfully updated in " + PROVIDER);
            meta.put("permission", permission);
            meta.put("permissionId", permission.getId());
            System.out.println("==================Data posted to Auth0");
        return null;
}

 /*amal*/
	@Override
	public void updateOperations(Permission permission, String user,HttpMethod httpMethod, String subpath, String tenant, boolean tenantUpdate, boolean authUpdate) throws Exception {
	      
    	logger.debug("=============tenant " + tenant);
    	logger.debug("=============permission " + permission.getName());
    	logger.debug("=============subpath " + subpath);
    	logger.debug("=============httpMethod " + httpMethod);
    	
        TenantHolder.setTenantId(tenant);

        TenantAuthProvider tenantAuthProvider = httpService.getTenantAuthProvider(tenant, PROVIDER);
        if(tenant.equals("AnRkr")){
            tenantAuthProvider.setPrimary(true);
        }
        if (tenantAuthProvider.isPrimary()) {
            try {
                updateAuth0(permission, httpMethod, subpath, tenant);
            }catch (Exception e){
                System.out.println(e);
                //delete in auth0 //primary tenant related exceptions
            }
            if (tenantUpdate) {
                updateOtherTenants(tenant, permission, user,httpMethod);
            }
        } else {
            try {
                updateAuth0(permission,httpMethod, subpath, tenant);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
    }
	
	/*amal*/
	void updateOtherTenants(String tenant, Permission permission,String user, HttpMethod httpMethod) {
        
 	  
 	     TenantHolder.setTenantId(permission.getTenant());
         logger.debug("==================Updating other tenants");

         String entityType = "permission";
         Object object = null;
         String subpath = null;

         HashMap<String, Object> meta = new HashMap<>();

         try {
             List<AppTenant> tenantList = httpService.getTenants();

             for (AppTenant appTenant : tenantList) {
                 if (!appTenant.getTenantId().equalsIgnoreCase(tenant)) {
                     logger.debug("=======================Updating tenant " + appTenant.getTenantId());
                     if (httpMethod == HttpMethod.POST) {
                         //Create addPermission object
                         AddPermission addPermission = new AddPermission();
                         addPermission.setName(permission.getName());
                         addPermission.setDescription(permission.getDescription());
                         
                         AddPermission.Meta innerObject = addPermission.new Meta();
                         innerObject.setSearchq(permission.getSearch());
                         addPermission.setMeta(innerObject);
                         object = addPermission;
                         
                        // create("defaultProjection", addPermission, user, appTenant.getTenantId(), false);/*XXX*/

                     } else {
                         //send request to get permission by name (to get id and version
                         Permission otherTenantPermission = httpService.getOtherTenantEntity(appTenant.getTenantId(), Permission.class, entityType, permission.getName());
                         Long id = otherTenantPermission.getId();
                         subpath = "/" + id.toString();
                         if (httpMethod == HttpMethod.PUT) {
                             //Create updatePermission object
                             UpdatePermission updatePermission = new UpdatePermission();
                             updatePermission.setName(permission.getName());
                             updatePermission.setDescription(permission.getDescription());
                             updatePermission.setVersion(otherTenantPermission.getVersion());
                             
                             UpdatePermission.Meta innerObject = updatePermission.new Meta();
                             innerObject.setSearchq(permission.getSearch());
                             updatePermission.setMeta(innerObject);
                             
                             object = updatePermission;
                             
                            // update("defaultProjection", id, updatePermission, user, appTenant.getTenantId(), false);/*XXX*/
                         }
                     }

                     //send request to update tenant
                     ResponseEntity responseEntity = httpService.sendOtherTenantUpdateRequest(appTenant.getTenantId(), entityType, object, httpMethod, subpath);

                     if(responseEntity.getStatusCodeValue() == 200){
                         System.out.println("SUCCESS");
                     }
                    /*==========================*/
                    
                     
                 }
             }
             meta.put("permission", permission);
             meta.put("permissionId", permission.getId());
             logger.debug("=================Data posted to update other tenants");

         } catch (Exception ex) {
             ex.printStackTrace();
         }
         TenantHolder.clear();
 }
	
	
	/*added by Pasindu - 2021-11-26*/   
	/*@Override
	public List<String> getAllwithoutPageSize() throws FXDefaultException {

		List<String> permissionList = new ArrayList<>();

		Iterable<Permission> allPermissions = permissionRepository.findAll();
		for (Permission permission : allPermissions) {
			permissionList.add(permission.getName());
		}
		return permissionList;

	}*/
	
	@Override
	public Iterable<Permission> getAllwithoutPageSize() {
		Iterable<Permission> allPermissions = permissionRepository.findByAuth0NotIn("Yes");
		return allPermissions;
	}
	
	@Override
	public Iterable<Permission> getAllDeletedPermission() throws FXDefaultException {
		Iterable<Permission> allPermissions = permissionRepository.findByDeleted();
		return allPermissions;
	}
	
	

	/*added by Pasindu - 2021-11-26 */
	
	/**
     * Get all permissions as pageable from provider
     * Not used by any endpoint
     */
	
	@Override
	public List<?> getAllPermissionFromAuth0(Pageable pageable, String bookmarks, String search,
			String permissionId, String tenent) throws FXDefaultException {
		String dataType = ROOT_PATH;
		String subPath = dataType;
		subPath = subPath;
		//permissionId = null;
		
		if(permissionId!=null) {
			 ProviderPermissionIdClass providerPermissionIdClass = new ProviderPermissionIdClass();
		        providerPermissionIdClass.setPermission(Long.parseLong(permissionId));
		        providerPermissionIdClass.setProvider(PROVIDER);
		        if (providerPermissionRepository.existsById(providerPermissionIdClass)) {
		            ProviderPermission providerPermission =providerPermissionRepository.findById(providerPermissionIdClass).get();
		            
		            subPath=subPath+"/"+providerPermission.getProviderPermissionId();
		        }
		}

		// send get request for permissions and return page
		return httpService.sendProviderGetRequestAswithoutPage(subPath, pageable, PermissionResponse.class,
				permissionId, dataType, PermissionResponse.class);
	}

	@Override
	public List<?> getAllPermissionById(Long id) throws FXDefaultException {
		List<Permission> list=new ArrayList<Permission>();
		Optional<Permission> allPermissions = permissionRepository.findById(id);
		
		Permission permissiona=null;
		if(allPermissions.isPresent())
			 permissiona=allPermissions.get();
		list.add(permissiona);
		
		return list;
	}

	@Override
	public void updatePermission(Permission permission) throws Exception {
		permission.setAuth0("Yes");
		permissionRepository.save(permission);
	}

	@Override
	public String getApplicationIdByTenant(String tenent) throws Exception {
		 //Get clientId secret for default tenant from comn-system-info
		TenantAuthProvider tenantAuthProvider=null;
		try {
			tenantAuthProvider = httpService.getTenantAuthProvider(tenent, PROVIDER);
		} catch (Exception e) {
			// TODO: handle exception
		}
        
		return tenantAuthProvider!=null?tenantAuthProvider.getClientId():null;
	}

	@Override
	public void updatePermissionByName(String name) throws Exception {
		Permission permission=permissionRepository.findByName(name);
		if(permission!=null) {
			permission.setAuth0("Yes");
			permissionRepository.save(permission);
		}
	
	}
	
	@Override
	public void deletePermissionByName(String name) throws Exception {
		Permission permission=permissionRepository.findByName(name);
		if(permission!=null) {
			permission.setDeleted("Yes");
			permissionRepository.save(permission);
		}
	
	}
	
	@Override
	public void deletePermission(Permission permission) throws Exception {
		try {
			ProviderPermissionIdClass providerPermissionIdClass = new ProviderPermissionIdClass(permission.getId(), PROVIDER);
			providerPermissionRepository.deleteById(providerPermissionIdClass);
			permissionRepository.deleteById(permission.getId());
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
	}
	
	@Override
	public Permission getPermissionByName(String name) throws Exception {
		Permission permission=null;
		try {
			permission=permissionRepository.findByName(name);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		return permission;
	}

	@Override
	public void updatePermissionByuid(String correctId, String wrongId) throws Exception {
		providerPermissionRepository.updatePermissionByuid(correctId,wrongId);
	}

	@Override
	public void addProviderRecord(String providerId, Permission permission) throws Exception {
		try {
			ProviderPermissionIdClass providerPermissionIdClass = new ProviderPermissionIdClass();
            providerPermissionIdClass.setProvider(PROVIDER);
            providerPermissionIdClass.setPermission(permission.getId());
            
			ProviderPermission providerPermission = new ProviderPermission();
            providerPermission.setProviderPermissionIdClass(providerPermissionIdClass);
            providerPermission.setProviderPermissionId(providerId);
            
            LoggerRequest.getInstance().logInfo("====================SAVING PERMISSION PROVIDER ID : "+ providerPermissionIdClass.getPermission());
            providerPermissionRepository.save(providerPermission);
            
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
	}

	@Override
	public Future<List<AddPermission>> syncPermission() {
		List<AddPermission> diffPermissionList = new ArrayList<AddPermission>();
		return new AsyncResult<List<AddPermission>>(diffPermissionList);
	}

	@Override
	@Async
	public void syncPermission(String userName, String tenent, String processCode) {
		TenantHolder.setTenantId(tenent);
		
		moduleService.insertProcessLog(processCode,userName!=null?userName:"sysUser");

		Iterable<Permission> dbPermissionList = getAllwithoutPageSize();

		List PermissionListFromAuth0=null;
		try {
			PermissionListFromAuth0 = getAllPermissionFromAuth0(null, null, null,null, tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
		
		
		String applicationId=null;
		try {
			applicationId = getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
			
		LoggerRequest.getInstance().logInfo("START_LOOP");
		for(Permission permissionFromDB:dbPermissionList){

			boolean foundOnAuth0 = false;
			int foundTenant = 0;

			ListIterator<LinkedHashMap> PermissionFromAuth0ListIterator = PermissionListFromAuth0.listIterator();

			while (PermissionFromAuth0ListIterator.hasNext()) {
				LinkedHashMap premissionFromAuth0 = PermissionFromAuth0ListIterator.next();

				if (permissionFromDB.getName().equals(premissionFromAuth0.get("name")) && applicationId!=null && applicationId.equals(premissionFromAuth0.get("applicationId"))) {
					foundOnAuth0 = true;
					foundTenant++;
					LoggerRequest.getInstance().logInfo("CHECK_TRUE" +"permission_name "+premissionFromAuth0.get("name")+"application_Name"+premissionFromAuth0.get("applicationId"));
					try {
						updatePermission(permissionFromDB);
					} catch (Exception e) {
						LoggerRequest.getInstance().logInfo("Exception_UPDATE "+e.toString());
					}
					
				}
			}
			// if permission not found in Auth0 side ,then added here
			if ((!foundOnAuth0) && applicationId!=null) {
				//addPermission.setDescription(permissionFromDB.getDescription());
				//addPermission.setName(permissionFromDB.getName());
				
				/*AddPermission.Meta meta = addPermission.new Meta();
				meta.setSearchq(permissionFromDB.getSearch());
				
				addPermission.setMeta(meta);*/
				
				
				//**************************
				try {
					TenantHolder.setTenantId(tenent);
					createPermission(null, permissionFromDB, userName!=null?userName:"sysUser", tenent, false,true);
				} catch (Exception e) {
					LoggerRequest.getInstance().logInfo("Exception_CREATE "+e.toString());
				}
				
				
				//**************************
			}
		}
		TenantHolder.setTenantId(tenent);
		moduleService.deleteProcessLog(processCode);
		
	}

	@Override
	public void updatePermissionsAuth0Field(Long id, String username, String tenent) throws Exception {
		try {
			Optional<Permission> permissionOp=permissionRepository.findById(id);
			if(permissionOp.isPresent()) {
				permissionOp.get().setAuth0(null);
				
				permissionRepository.save(permissionOp.get());
			}
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("updatePermissionsAuth0Field "+e.toString());
		}
		
	}

	@Override
	@Async
	public void missingProviderPermission(String userName, String tenent, String processCode) throws FXDefaultException{
		TenantHolder.setTenantId(tenent);
		
		moduleService.insertProcessLog(processCode,userName);
		
    	ArrayList<String> permissionList=new ArrayList<>();
    	List PermissionListFromAuth0 =null;
    	try {
    		PermissionListFromAuth0 = getAllPermissionFromAuth0(null, null, null,null, tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}
    		
    	String applicationId=null;
		try {
			applicationId = getApplicationIdByTenant(tenent);
		} catch (Exception e) {
			LoggerRequest.getInstance().logInfo("Exception "+e.toString());
		}

		ListIterator<LinkedHashMap> PermissionFromAuth0ListIterator = PermissionListFromAuth0.listIterator();

		while (PermissionFromAuth0ListIterator.hasNext()) {
			LinkedHashMap premissionFromAuth0 = PermissionFromAuth0ListIterator.next();

			if (premissionFromAuth0.get("_id")!=null && premissionFromAuth0.get("name")!=null && applicationId!=null && applicationId.equals(premissionFromAuth0.get("applicationId"))) {
				
				try {
					Optional<ProviderPermission> providerPermissionOp =providerPermissionRepository.findByProviderPermissionProviderPermissionId(premissionFromAuth0.get("_id").toString());
					Permission permission = getPermissionByName(premissionFromAuth0.get("name").toString());
					
					if(!providerPermissionOp.isPresent() && permission!=null) {
						permissionList.add(permission.getName()!=null?permission.getName():"");
						
						addProviderRecord(premissionFromAuth0.get("_id").toString(),permission);
					}
					
				} catch (Exception e) {
					LoggerRequest.getInstance().logInfo("Exception "+e.toString());
				}
				
			}
		}
		moduleService.deleteProcessLog(processCode);
		
		TenantHolder.clear();
	}

}
