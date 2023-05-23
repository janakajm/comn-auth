package com.loits.comn.auth.services.impl;


import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.services.AuthUserService;
import com.loits.comn.auth.services.HttpService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class AuthUserServiceImpl implements AuthUserService {

    Logger logger = LogManager.getLogger(AuthUserServiceImpl.class);

    @Autowired
    HttpService httpService;

    @Override
    public String getAll(Pageable pageable, String search, String projection) throws FXDefaultException {
        return httpService.getAuthUsers(pageable, search, projection);
    }
    
    @Override
    public String getAllUser(String search, String projection) throws FXDefaultException {
        return httpService.getAllAuthUsers(search, projection);
    }
}
