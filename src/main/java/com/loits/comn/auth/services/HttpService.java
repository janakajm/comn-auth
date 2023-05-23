package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.domain.Permission;
import com.loits.comn.auth.dto.AppTenant;
import com.loits.comn.auth.dto.ScopesRequest;
import com.loits.comn.auth.dto.TenantAuthProvider;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface HttpService {

    <T> Page<?> sendProviderGetRequestAsPage(String subPath, Pageable pageable, Class<T> classType, String id, String type, Class<?> projectionClass) throws FXDefaultException;

    <T> List<?> sendProviderGetRequestAsList(String subPath, Class<T> classType, String id, String type) throws FXDefaultException;

    <T> ResponseEntity<T> sendProviderRestRequest(HttpMethod httpMethod, String subPath, Object object, Class<T> classType) throws FXDefaultException;

    TenantAuthProvider getTenantAuthProvider(String tenant, String provider);

    <T> T getOtherTenantEntity(String tenant, Class<T> classType, String entityType, String name);

    List<AppTenant> getTenants();

   // void sendOtherTenantUpdateRequest(String tenant, String entityType, Object object, HttpMethod httpMethod, String subPath);
    ResponseEntity sendOtherTenantUpdateRequest(String tenant, String entityType, Object object, HttpMethod httpMethod, String subPath);
    //for auth0 //TODO move to a different service to separate?
    ResponseEntity updatePermissionsinProviderApi(ScopesRequest scopesRequest) throws FXDefaultException;

    String getAuthUsers(Pageable pageable, String s, String search) throws FXDefaultException;
    
    /*added by Pasindu 2021-11-26*/
    <T> List<?> sendProviderGetRequestAswithoutPage(String subPath, Pageable pageable, Class<T> classType, String id, String type, Class<?> projectionClass) throws FXDefaultException;
    
    
    <T> List<?> sendProviderGetRequest(String subPath, Pageable pageable, Class<T> classType, String id, String type, Class<?> projectionClass) throws FXDefaultException;

	String getAllAuthUsers(String search, String projection) throws FXDefaultException;
}
