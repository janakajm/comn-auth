package com.loits.comn.auth.services.impl;

import com.loits.comn.auth.controller.RoleGroupController;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.RolePermission;
import com.loits.comn.auth.domain.ScheduledJobs;
import com.loits.comn.auth.dto.PermissionResponse;
import com.loits.comn.auth.repo.ScheduledJobRepository;
import com.loits.comn.auth.services.DataVerifyService;
import com.loits.comn.auth.services.HttpService;
import com.loits.comn.auth.services.PermissionService;
import com.loits.comn.auth.services.RolePermissionService;
import com.loits.comn.auth.services.RoleService;
import com.loits.comn.auth.services.ScheduledJobService;
import com.loits.comn.auth.services.SyncService;
import org.springframework.data.domain.Pageable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class ScheduledJobServiceImpl implements ScheduledJobService {

    @Autowired
    ScheduledJobRepository scheduledJobRepository;

    @Autowired
    SyncService syncService;

    @Autowired
    DataVerifyService dataVerifyService;
    
    @Autowired
	 PermissionService permissionService;
	 
	@Autowired
	HttpService httpService;
	 
	@Autowired
	RoleService roleService;
	 
	@Autowired
	RolePermissionService rolePermissionService;
	
	Logger logger = LogManager.getLogger(ScheduledJobServiceImpl.class);

    @Override
    public void startCronTasks(String user,String tenant) throws FXDefaultException {


        List<ScheduledJobs.JobType> jobTypes = Arrays.asList(ScheduledJobs.JobType.values());


        for(ScheduledJobs.JobType jobtype : jobTypes){
            ScheduledJobs scheduledJob = null;
            if(scheduledJobRepository.findTop1ByJobOrderByStartedTimeDesc(jobtype).isPresent()) {
                scheduledJob = scheduledJobRepository.findTop1ByJobOrderByStartedTimeDesc(jobtype).get();
            }
            if(scheduledJob == null){
                startCronJob(tenant,jobtype,user);
            }else{
                Date nowDate = new Date();
                if((nowDate.getTime() - scheduledJob.startedTime.getTime()) > 52){
                    startCronJob(tenant,jobtype,user);
                }
            }
        }

    }

    public void startCronJob(String tenant, ScheduledJobs.JobType jobType, String user) throws FXDefaultException {
        ScheduledJobs scheduledJob = new ScheduledJobs();
        scheduledJob.setStartedTime(new Timestamp(new Date().getTime()));
        scheduledJob.setJob(jobType);
        scheduledJob.setStatus(ScheduledJobs.StatusType.PENDING);
        scheduledJob.setCreatedBy(user);
        scheduledJobRepository.save(scheduledJob);

        switch (jobType.name()){
            case "SYNC_BRANCHES":
                syncService.synchBranches(tenant);
                break;
            case "SYNC_USER_PROFILES":
                syncService.syncUsers(tenant);
                break;
            case "SYNC_USER_GROUPS":
                syncService.syncUserGroups(tenant);
                break;
            case "SYNC_USER_GROUP_USERS":
                syncService.syncUserGroupUsers(tenant);
                break;
            case "VERIFY_PERMISSION":
                dataVerifyService.syncPermissions(tenant);
                break;
            case "VERIFY_ROLE_PERMISSIONS":
                dataVerifyService.syncGroups(tenant);
                break;
            case "VERIFY_GROUP_ROLES":
                dataVerifyService.syncRoles(tenant);
                break;
            case "VERIFY_USER_ROLES":
                dataVerifyService.syncUserRoles(tenant);
                break;

        }
    }

    @Override
    public String getCronExpression() {
        return null;
    }
}
