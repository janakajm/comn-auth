package com.loits.comn.auth.services;

import java.util.List;

import com.loits.comn.auth.core.FXDefaultException;

public interface DataVerifyService {

    Object syncPermissions(String tenent) throws FXDefaultException;

    Object syncRoles(String tenent) throws FXDefaultException;

    Object syncGroups(String tenent) throws FXDefaultException;

    Object syncUserRoles(String tenent) throws FXDefaultException;

}
