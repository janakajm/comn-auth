package com.loits.comn.auth.services.impl;

import com.loits.comn.auth.commons.NullAwareBeanUtilsBean;
import com.loits.comn.auth.domain.*;
import com.loits.comn.auth.mt.TenantHolder;
import com.loits.comn.auth.repo.*;
import com.loits.comn.auth.services.HistoryService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service
public class HistoryServiceImpl implements HistoryService {

    Logger logger = LogManager.getLogger(HistoryServiceImpl.class);

    @Autowired
    PermissionHistoryRepository permissionHistoryRepository;

    @Autowired
    RoleHistoryRepository roleHistoryRepository;

    @Autowired
    RoleGroupHistoryRepository roleGroupHistoryRepository;

    @Autowired
    RolePermissionHistoryRepository rolePermissionHistoryRepository;

    @Autowired
    RoleGroupRoleHistoryRepository roleGroupRoleHistoryRepository;

    /**
     * Save history record
     *
     * @param permissionList
     */
    @Override
    public CompletableFuture<?> saveBulkHistoryRecord(List<Permission> permissionList, String recordType) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Saving Permissions to history in bulk");
            List<PermissionHistory> permissionHistoryList = new ArrayList<>();
            permissionList.forEach(permission -> {
                TenantHolder.setTenantId(permission.getTenant());

                PermissionHistory permissionHistory = new PermissionHistory();

                //set id field to ignore when copying
                HashSet<String> ignoreFields = new HashSet<String>();
                ignoreFields.add("id");

                try {
                    NullAwareBeanUtilsBean utilsBean = new NullAwareBeanUtilsBean();
                    utilsBean.setIgnoreFields(ignoreFields);
                    utilsBean.copyProperties(permissionHistory, permission);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                permissionHistory.setPermissionId(permission.getId());
                permissionHistory.setRecordType(recordType);
                permissionHistoryList.add(permissionHistory);
            });

            try {
                permissionHistoryRepository.saveAll(permissionHistoryList);
                logger.debug("Permissions saved to history successfully");
            }catch(Exception e){
                logger.debug("Permissions saving to history failed");
                e.printStackTrace();
            }finally {
                TenantHolder.clear();
            }
        });
    }

    /**
     * Save history record
     *
     * @param permission
     */
    @Override
    public CompletableFuture<?> saveHistoryRecord(Permission permission, String recordType) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Saving Permission to history. Tenent " + permission.getTenant());
            TenantHolder.setTenantId(permission.getTenant());

            PermissionHistory permissionHistory = new PermissionHistory();

            //set id field to ignore when copying
            HashSet<String> ignoreFields = new HashSet<String>();
            ignoreFields.add("id");

            try {
                NullAwareBeanUtilsBean utilsBean = new NullAwareBeanUtilsBean();
                utilsBean.setIgnoreFields(ignoreFields);
                utilsBean.copyProperties(permissionHistory, permission);
            } catch (Exception e) {
                e.printStackTrace();
            }

            permissionHistory.setPermissionId(permission.getId());

            permissionHistory.setRecordType(recordType);
            try {
                permissionHistoryRepository.save(permissionHistory);
                logger.debug("Permission with Id " + permission.getId() + " saved to history successfully");
            } catch (Exception e) {
                logger.debug("Permission with Id " + permission.getId() + " saving to history failed");
                e.printStackTrace();
            } finally {
                TenantHolder.clear();
            }
        });
    }

    /**
     * Save history record
     *
     * @param role
     */
    @Override
    public CompletableFuture<?> saveHistoryRecord(Role role, String recordType) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Saving Role to history. Tenent " + role.getTenant());
            TenantHolder.setTenantId(role.getTenant());

            RoleHistory roleHistory = new RoleHistory();

            //set id field to ignore when copying
            HashSet<String> ignoreFields = new HashSet<String>();
            ignoreFields.add("id");

            try {
                NullAwareBeanUtilsBean utilsBean = new NullAwareBeanUtilsBean();
                utilsBean.setIgnoreFields(ignoreFields);
                utilsBean.copyProperties(roleHistory, role);
            } catch (Exception e) {
                e.printStackTrace();
            }

            roleHistory.setRoleId(role.getId());

            roleHistory.setRecordType(recordType);
    
            try {
                roleHistoryRepository.save(roleHistory);
                logger.debug("Role with Id " + role.getId() + " saved to history successfully");
            } catch (Exception e) {
                logger.debug("Role with Id " + role.getId() + " saving to history failed");
                e.printStackTrace();
            } finally {
                TenantHolder.clear();
            }
        });
    }

    /**
     * Save history record
     *
     * @param roleGroup
     */
    @Override
    public CompletableFuture<?> saveHistoryRecord(RoleGroup roleGroup, String recordType) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Saving Role-Template to history. Tenent " + roleGroup.getTenant());
            TenantHolder.setTenantId(roleGroup.getTenant());

            RoleGroupHistory roleGroupHistory = new RoleGroupHistory();

            //set id field to ignore when copying
            HashSet<String> ignoreFields = new HashSet<String>();
            ignoreFields.add("id");

            try {
                NullAwareBeanUtilsBean utilsBean = new NullAwareBeanUtilsBean();
                utilsBean.setIgnoreFields(ignoreFields);
                utilsBean.copyProperties(roleGroupHistory, roleGroup);
            } catch (Exception e) {
                e.printStackTrace();
            }

            roleGroupHistory.setRoleTemplateId(roleGroup.getId());

            roleGroupHistory.setRecordType(recordType);
            try {
                roleGroupHistoryRepository.save(roleGroupHistory);
                logger.debug("Role-Template with Id " + roleGroup.getId() + " saved to history successfully");
            }catch(Exception e){
                logger.debug("Role-Template with Id "+ roleGroup.getId()+" saving to history failed");
                e.printStackTrace();
            }finally {
                TenantHolder.clear();
            }
        });
    }

    /**
     * Save history record
     *
     * @param rolePermission
     */
    @Override
    public CompletableFuture<?> saveHistoryRecord(RolePermission rolePermission, String recordType) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Saving Role-Permission to history. Tenent " + rolePermission.getTenant());
            TenantHolder.setTenantId(rolePermission.getTenant());

            RolePermissionHistory rolePermissionHistory = new RolePermissionHistory();

            //set id field to ignore when copying
            HashSet<String> ignoreFields = new HashSet<String>();
            ignoreFields.add("id");

            try {
                NullAwareBeanUtilsBean utilsBean = new NullAwareBeanUtilsBean();
                utilsBean.setIgnoreFields(ignoreFields);
                utilsBean.copyProperties(rolePermissionHistory, rolePermission);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(rolePermission.getRole()!=null){
                rolePermissionHistory.setRoleId(rolePermission.getRole().getId());
            }

            if(rolePermission.getPermission()!=null){
                rolePermissionHistory.setPermissionId(rolePermission.getPermission().getId());
            }

            rolePermissionHistory.setRecordType(recordType);
            try {
                rolePermissionHistoryRepository.save(rolePermissionHistory);
                logger.debug("Role-Permission with Id " + rolePermission.getRolePermissionId() + " saved to history successfully");
            }catch(Exception e){
                logger.debug("Role-Permission with Id "+ rolePermission.getRolePermissionId()+" saving to history failed");
                e.printStackTrace();
            }finally {
                TenantHolder.clear();
            }
        });
    }
    
    @Override
    public void saveRolePermissionHistoryRecord(RolePermission rolePermission, String recordType) {
            logger.debug("Saving Role-Permission to history. Tenent " + rolePermission.getTenant());
            TenantHolder.setTenantId(rolePermission.getTenant());

            RolePermissionHistory rolePermissionHistory = new RolePermissionHistory();

            rolePermissionHistory.setCreatedBy(rolePermission.getCreatedBy());
            //rolePermissionHistory.setCreatedOn(new Timestamp(new Date().getTime()).toString());

            if(rolePermission.getRole()!=null){
                rolePermissionHistory.setRoleId(rolePermission.getRole().getId());
            }

            if(rolePermission.getPermission()!=null){
                rolePermissionHistory.setPermissionId(rolePermission.getPermission().getId());
            }

            rolePermissionHistory.setRecordType(recordType);
            try {
                rolePermissionHistoryRepository.save(rolePermissionHistory);
                logger.debug("Role-Permission with Id " + rolePermission.getRolePermissionId() + " saved to history successfully");
            }catch(Exception e){
                logger.debug("Role-Permission with Id "+ rolePermission.getRolePermissionId()+" saving to history failed");
                e.printStackTrace();
            }finally {
                TenantHolder.clear();
            }

    }
    
    @Override
    public void saveRolePermissionHistory(RolePermission rolePermission, String recordType) {
            logger.debug("Saving Role-Permission to history. Tenent " + rolePermission.getTenant());

            RolePermissionHistory rolePermissionHistory = new RolePermissionHistory();

            rolePermissionHistory.setCreatedBy(rolePermission.getCreatedBy());
            //rolePermissionHistory.setCreatedOn(new Timestamp(new Date().getTime()).toString());

            if(rolePermission.getRole()!=null){
                rolePermissionHistory.setRoleId(rolePermission.getRole().getId());
            }

            if(rolePermission.getPermission()!=null){
                rolePermissionHistory.setPermissionId(rolePermission.getPermission().getId());
            }

            rolePermissionHistory.setRecordType(recordType);
            try {
                rolePermissionHistoryRepository.save(rolePermissionHistory);
                logger.debug("Role-Permission with Id " + rolePermission.getRolePermissionId() + " saved to history successfully");
            }catch(Exception e){
                logger.debug("Role-Permission with Id "+ rolePermission.getRolePermissionId()+" saving to history failed");
                e.printStackTrace();
            }

    }

    @Override
    public CompletableFuture<?> saveHistoryRecord(RoleGroupRole roleGroupRole, String recordType) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Saving RoleGroupRole to history. Tenent " + roleGroupRole.getTenant());
            TenantHolder.setTenantId(roleGroupRole.getTenant());

            RoleGroupRoleHistory roleGroupRoleHistory = new RoleGroupRoleHistory();

            //set id field to ignore when copying
            HashSet<String> ignoreFields = new HashSet<String>();
            ignoreFields.add("id");

            try {
                NullAwareBeanUtilsBean utilsBean = new NullAwareBeanUtilsBean();
                utilsBean.setIgnoreFields(ignoreFields);
                utilsBean.copyProperties(roleGroupRoleHistory, roleGroupRole);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(roleGroupRole.getRole()!=null){
                roleGroupRoleHistory.setRoleId(roleGroupRole.getRole().getId());
            }

            if(roleGroupRole.getRoleGroup()!=null){
                roleGroupRoleHistory.setRoleTemplateId(roleGroupRole.getRoleGroup().getId());
            }

            roleGroupRoleHistory.setRecordType(recordType);
            try {
                roleGroupRoleHistoryRepository.save(roleGroupRoleHistory);
                logger.debug("RoleGroupRole with Id " + roleGroupRole.getRoleGroupRoleId() + " saved to history successfully");
            }catch(Exception e){
                logger.debug("RoleGroupRole with Id "+ roleGroupRole.getRoleGroupRoleId()+" saving to history failed");
                e.printStackTrace();
            }finally {
                TenantHolder.clear();
            }
        });
    }

	@Override
	public void savePermissionHistoryRecord(Permission permission, String recordType, String tenent) {
            logger.debug("Saving Permission to history. Tenent " + tenent);
            TenantHolder.setTenantId(tenent);

            PermissionHistory permissionHistory = new PermissionHistory();

            //set id field to ignore when copying
            HashSet<String> ignoreFields = new HashSet<String>();
            ignoreFields.add("id");

            try {
                NullAwareBeanUtilsBean utilsBean = new NullAwareBeanUtilsBean();
                utilsBean.setIgnoreFields(ignoreFields);
                utilsBean.copyProperties(permissionHistory, permission);
            } catch (Exception e) {
                e.printStackTrace();
            }

            permissionHistory.setPermissionId(permission.getId());

            permissionHistory.setRecordType(recordType);
            try {
                permissionHistoryRepository.save(permissionHistory);
                logger.debug("Permission with Id " + permission.getId() + " saved to history successfully");
            } catch (Exception e) {
                logger.debug("Permission with Id " + permission.getId() + " saving to history failed");
                e.printStackTrace();
            }
		
	}

}
