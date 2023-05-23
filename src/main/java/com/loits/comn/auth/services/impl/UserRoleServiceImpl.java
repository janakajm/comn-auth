package com.loits.comn.auth.services.impl;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loits.comn.auth.core.RestTemplateResponseErrorHandler;
import com.loits.comn.auth.dto.Token;
import com.loits.comn.auth.services.HttpService;
import com.loits.comn.auth.services.TokenService;
import com.loits.comn.auth.services.UserRoleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    Logger logger = LogManager.getLogger(UserRoleServiceImpl.class);

    @Autowired
    RestTemplateBuilder builder;

    @Value("${auth0.authorization.extension.api.url}")
    private String EXTENSION_API_URL;


    @Override
    public Object delete(String projection, String userId, String roleId, String user, String tenent) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        ResponseEntity responseEntity = null;

        String subPath = "users/"+userId+"/roles";
        //Repeat request on code expiry
        do {
            RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                    .build();

            //Set headers for request
            HttpHeaders httpHeaders = new HttpHeaders();
            Token token = Token.getInstance();
            httpHeaders.add("Authorization", "Bearer " + token.getAccess_token());

            //Create Entity with headers
            String[] rolesArray = new String[1];
            rolesArray[0]= roleId;
            HttpEntity<String[]> httpEntity = new HttpEntity<>(rolesArray, httpHeaders);

            //Send GET request with entity
            responseEntity =
                    restTemplate.exchange(String.format(EXTENSION_API_URL, subPath),
                            HttpMethod.DELETE, httpEntity, String.class);
            if (responseEntity.getStatusCodeValue() == 401) {
                logger.debug("Token expired, retrying request");
            }
        } while (responseEntity.getStatusCodeValue() == 401);


        return responseEntity.getStatusCode();
    }

    @Override
    public Object update(String projection, String userId, String roleId, String user, String tenent) {
        ObjectMapper objectMapper = new ObjectMapper();
        ResponseEntity responseEntity = null;

        //TODO check delegable?
        String subPath = "users/"+userId+"/roles";
        //Repeat request on code expiry
        do {
            RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                    .build();

            //Set headers for request
            HttpHeaders httpHeaders = new HttpHeaders();
            Token token = Token.getInstance();
            httpHeaders.add("Authorization", "Bearer " + token.getAccess_token());

            //Create Entity with headers
            String[] rolesArray = new String[1];
            rolesArray[0]= roleId;
            HttpEntity<String[]> httpEntity = new HttpEntity<>(rolesArray, httpHeaders);

            //Send GET request with entity
            responseEntity =
                    restTemplate.exchange(String.format(EXTENSION_API_URL, subPath),
                            HttpMethod.PATCH, httpEntity, String.class);
            if (responseEntity.getStatusCodeValue() == 401) {
                logger.debug("Token expired, retrying request");
            }
        } while (responseEntity.getStatusCodeValue() == 401);


        return responseEntity.getStatusCode();
    }
}
