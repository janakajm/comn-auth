package com.loits.comn.auth.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.google.gson.JsonArray;
import com.loits.comn.auth.commons.RestResponsePage;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.RestTemplateResponseErrorHandler;
import com.loits.comn.auth.dto.*;
import com.loits.comn.auth.services.HttpService;
import com.loits.comn.auth.services.TokenService;

import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;


@Service
public class HttpServiceImpl implements HttpService {

    Logger logger = LogManager.getLogger(HttpServiceImpl.class);

    @Autowired
    RestTemplateBuilder builder;

    @Autowired
    TokenService tokenService;

    @Value("${auth0.authorization.extension.api.url}")
    private String EXTENSION_API_URL;

    @Value("${auth0.authorization.extension.api.token.grant-type}")
    private String GRANT_TYPE;

    @Value("${comn.service.info.url.tenant-auth-provider}")
    private String COMN_SERVICE_INFO_SERVIC_URL_TENANT_AUTH_PROVIDER;

    @Value("${comn.service.info.url.app-tenant}")
    private String COMN_SERVICE_INFO_SERVIC_URL_APP_TENANT;

    @Value("${comn.auth.url}")
    private String COMN_AUTH_URL;

    @Value("${auth0.authorization.extension.app.client-id}")
    private String CLIENT_ID;

    @Value("${auth0.authorization.extension.app.client-secret}")
    private String CLIENT_SECRET;

    @Value("${auth0.management.api.identifier}")
    private String MANAGEMENT_API_AUDIENCE;

    @Value("${auth0.management.api.resource.servers.url}")
    private String MANAGEMENT_API_RESOURCE_UPDATE_URL;

    @Value("${auth0.application.api.app.id}")
    private String APPLICATION_API_APP_ID;

    /**
     * To get objects from provider as page
     *
     * @param subPath
     * @param pageable
     * @param classType
     * @param id
     * @param type
     * @param projectionClass
     * @param <T>
     * @return
     * @throws FXDefaultException
     */
    @Override
    public <T> Page<?> sendProviderGetRequestAsPage(String subPath, Pageable pageable, Class<T> classType, String id, String type, Class<?> projectionClass) throws FXDefaultException {
        ResponseEntity responseEntity = null;
        int tokenRetryCount = 0;
        List<T> responseContentList = null;

        //Repeat request on code expiry
        do {
            RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                    .build();

            //Set headers for request
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Authorization", "Bearer " + Token.token.getAccess_token());

            //Create Entity with headers
            HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

            //Send GET request with entity
            responseEntity =
                    restTemplate.exchange(String.format(EXTENSION_API_URL, subPath),
                            HttpMethod.GET, httpEntity, String.class);
            if (responseEntity.getStatusCodeValue() == 401) {
                tokenService.getExtensionAppToken();
                logger.debug("Token expired, retrying request");
                tokenRetryCount += 1;
            }
        } while (responseEntity.getStatusCodeValue() == 401 && tokenRetryCount < 3);

        //Throw exception after 3 tries to get token
        if (tokenRetryCount == 3) {
            throw new FXDefaultException("", "", "Unable to process Request due to invalid token", new Date(), HttpStatus.BAD_REQUEST);
        }

        //map response body to type
        responseContentList = convertResponseToList(id, responseEntity, classType, type);

        //convert responseList to page
        return convertListToPage(pageable, responseContentList, projectionClass);
    }

    @Override
    public <T> List<?> sendProviderGetRequestAsList(String subPath, Class<T> classType, String id, String type) throws FXDefaultException {
        ResponseEntity responseEntity = null;
        int tokenRetryCount = 0;
        List<T> responseContentList = null;

        //Repeat request on code expiry
        do {
            RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                    .build();

            //Set headers for request
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Authorization", "Bearer " + Token.token.getAccess_token());

            //Create Entity with headers
            HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

            //Send GET request with entity
            responseEntity =
                    restTemplate.exchange(String.format(EXTENSION_API_URL, subPath),
                            HttpMethod.GET, httpEntity, String.class);
            if (responseEntity.getStatusCodeValue() == 401) {
                tokenService.getExtensionAppToken();
                logger.debug("Token expired, retrying request");
                tokenRetryCount += 1;
            }
        } while (responseEntity.getStatusCodeValue() == 401 && tokenRetryCount < 3);

        //Throw exception after 3 tries to get token
        if (tokenRetryCount == 3) {
            throw new FXDefaultException("", "", "Unable to process Request due to invalid token", new Date(), HttpStatus.BAD_REQUEST);
        }

        //map response body to type
        responseContentList = convertResponseToList(id, responseEntity, classType, type);
        return responseContentList;
    }
    
    private void disableCertificateVerification() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) {
            }
        } };

        try {
        	// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        				
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            HttpsURLConnection.setDefaultHostnameVerifier(
                    (hostname, session) -> hostname.equals("IPADDRESS"));
        } catch (NoSuchAlgorithmException | KeyManagementException e) {

        }
    }



    /**
     * Create/update/delete entities in provider (permissions/roles/groups)
     *
     * @param httpMethod
     * @param subPath
     * @param object
     * @param classType
     * @param <T>
     * @return
     * @throws FXDefaultException
     */
    @Override
    public <T> ResponseEntity<T> sendProviderRestRequest(HttpMethod httpMethod, String subPath, Object object, Class<T> classType) throws FXDefaultException {
        int tokenRetryCount = 0;
        ResponseEntity responseEntity = null;
        HttpEntity<Object> httpEntity = null;
        //Repeat request on code expiry
        do {
        	//disableCertificateVerification();
        	
            RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                    .build();

            //Set headers for request
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Authorization", "Bearer " + Token.token.getAccess_token());
            logger.debug("TOKEN",Token.token.getAccess_token());
            logger.info("TOKEN",Token.token.getAccess_token());
            logger.warn("TOKEN",Token.token.getAccess_token());
            System.out.println("TOKEN "+ Token.token.getAccess_token());
            System.out.println("obj"+ object);
            //Create Entity with headers
            if (object != null) {
                httpEntity = new HttpEntity<>(object, httpHeaders);
            } else {
                httpEntity = new HttpEntity<>(httpHeaders);
            }
            //Send GET request with entity
            System.out.println(String.format(EXTENSION_API_URL, subPath));
            responseEntity =restTemplate.exchange(String.format(EXTENSION_API_URL, subPath),httpMethod, httpEntity, classType);
            if (responseEntity.getStatusCodeValue() == 401) {
                tokenService.getExtensionAppToken();
                logger.debug("Token expired, retrying request");
                tokenRetryCount += 1;
            }
        } while (responseEntity.getStatusCodeValue() == 401 && tokenRetryCount < 3);

        //Throw exception after 3 tries to get token
        if (tokenRetryCount == 3) {
            throw new FXDefaultException("", "", "Unable to process Request due to invalid token", new Date(), HttpStatus.BAD_REQUEST);
        }
        //Throw exception on conversion to output type
        System.out.println("RESULT for subpath"+subPath +"  "+ responseEntity.getBody() + responseEntity.getStatusCode());
        return responseEntity;

    }

    /**
     * Get AuthProvider by tenant from comn-system-info service
     *
     * @param tenant
     * @param provider
     * @return
     */
    @Override
    public TenantAuthProvider getTenantAuthProvider(String tenant, String provider) {
        logger.debug("Sending request to Comn-System-Info service to get tenant-provider details");
        logger.debug("tenant: "+ tenant + " provider: " + provider);

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        //Set Query Params
        UriComponents builder = UriComponentsBuilder.fromHttpUrl(COMN_SERVICE_INFO_SERVIC_URL_TENANT_AUTH_PROVIDER)
                .queryParam("tenant", tenant)
                .queryParam("provider", provider).build();

        String url = builder.toUriString();

        //Send GET request
        try {
            TenantAuthProvider tenantAuthProvider = restTemplate.getForObject(url, TenantAuthProvider.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        TenantAuthProvider tenantAuthProvider = restTemplate.getForObject(url, TenantAuthProvider.class);

        logger.debug("Recieved response Comn-System-Info service to get tenant-provider details");

        return tenantAuthProvider;
    }

    @Override
    public <T> T getOtherTenantEntity(String tenant, Class<T> classType, String entityType, String name) {
        logger.debug("Sending request to get " + entityType + " from " + tenant);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        //Set Query Params to filter by name
        UriComponents builder = UriComponentsBuilder.fromHttpUrl(String.format(COMN_AUTH_URL, entityType, tenant, "")).
                queryParam("name", name).build();

        String url = builder.toUriString();

        //Send GET request
        RestResponsePage page = restTemplate.getForObject(url, RestResponsePage.class);

        //convert page to list to object
        JavaType itemType = objectMapper.getTypeFactory().constructCollectionType(List.class, classType);
        List<T> objectList = objectMapper.convertValue(page.getContent(), itemType);
        T object = objectList.get(0);

        logger.debug("Recieved response for " + entityType + " from " + tenant);

        return object;
    }

    /**
     * Get all tenants from Comn-System-Info Service
     *
     * @return
     */
    @Override
    public List<AppTenant> getTenants() {
        logger.debug("Sending request to Comn-System-Info service to get tenant details");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        UriComponents builder = UriComponentsBuilder.fromHttpUrl(COMN_SERVICE_INFO_SERVIC_URL_APP_TENANT).
                queryParam("page", 0).queryParam("size", Integer.MAX_VALUE).build();

        String url = builder.toUriString();

        //Send GET request

        RestResponsePage page = restTemplate.getForObject(url, RestResponsePage.class);
        List<AppTenant> appTenantList = objectMapper.convertValue(page.getContent(), new TypeReference<List<AppTenant>>() {
        });
        logger.debug("Recieved response Comn-System-Info service to get tenant details");

        return appTenantList;
    }

    /**
     * Create/update/delete entities in other tenants (locally)
     *
     * @param tenant
     * @param entityType
     * @param object
     * @param httpMethod
     */
   /* @Override
    public void sendOtherTenantUpdateRequest(String tenant, String entityType, Object object, HttpMethod httpMethod, String subPath) {
        logger.debug("Sending request to tenant " + tenant);
        HttpEntity<Object> httpEntity = null;
        String url = null;

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        if (httpMethod == HttpMethod.POST) {
            url = String.format(COMN_AUTH_URL, entityType, tenant, "");
            System.out.println("URL : " +url);
        } else {
            url = String.format(COMN_AUTH_URL, entityType, tenant, subPath);
            System.out.println("URL : " +url);
        }
        //Set Query Params
        UriComponents builder = UriComponentsBuilder.fromHttpUrl(url).
                queryParam("tenantUpdate", false).build();

        url = builder.toUriString();
        if (object != null) {
            httpEntity = new HttpEntity<>(object);
        }
        //Send POST request
        restTemplate.exchange(url, httpMethod, httpEntity, String.class);
    }*/
    
    /**
     * Create/update/delete entities in other tenants (locally)
     *
     * @param tenant
     * @param entityType
     * @param object
     * @param httpMethod
     */
    @Override
    public ResponseEntity sendOtherTenantUpdateRequest(String tenant, String entityType, Object object, HttpMethod httpMethod, String subPath) {
        logger.debug("===============Sending request to tenant " + tenant);
        HttpEntity<Object> httpEntity = null;
        ResponseEntity responseEntity;
        String url = null;

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        if (httpMethod == HttpMethod.POST) {
            url = String.format(COMN_AUTH_URL, entityType, tenant, "");
            System.out.println("=================URL : " +url);
        } else {
            url = String.format(COMN_AUTH_URL, entityType, tenant, subPath);
            System.out.println("=================URL : " +url);
        }
        //Set Query Params
        UriComponents builder = UriComponentsBuilder.fromHttpUrl(url).
                queryParam("tenantUpdate", false).build();

        url = builder.toUriString();
        if (object != null) {
            httpEntity = new HttpEntity<>(object);
        }
        //Send POST request
        responseEntity = restTemplate.exchange(url, httpMethod, httpEntity, String.class);

        return responseEntity;
    }


    @Override
    public ResponseEntity updatePermissionsinProviderApi(ScopesRequest scopesRequest) throws FXDefaultException {
        int tokenRetryCount = 0;
        ResponseEntity responseEntity = null;
        HttpEntity<Object> httpEntity = null;

        //Repeat request on code expiry

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setClient_id(CLIENT_ID);
        tokenRequest.setClient_secret(CLIENT_SECRET);
        tokenRequest.setGrant_type(GRANT_TYPE);
        tokenRequest.setAudience(MANAGEMENT_API_AUDIENCE);

        Token token = tokenService.getTokenByRequest(tokenRequest);

        //Set headers for request
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + token.getAccess_token());

        //Create Entity with headers
        httpEntity = new HttpEntity<>(scopesRequest, httpHeaders);

        //Send GET request with entity
        responseEntity =
                restTemplate.exchange(String.format(MANAGEMENT_API_RESOURCE_UPDATE_URL, APPLICATION_API_APP_ID),
                        HttpMethod.PATCH, httpEntity, String.class);


        return responseEntity;
    }

    @Override
    public String getAuthUsers(Pageable pageable,String search, String projection) throws FXDefaultException {
        int tokenRetryCount = 0;
        ResponseEntity<String> responseEntity = null;
        HttpEntity<Object> httpEntity = null;

        //Repeat request on code expiry

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setClient_id(CLIENT_ID);
        tokenRequest.setClient_secret(CLIENT_SECRET);
        tokenRequest.setGrant_type(GRANT_TYPE);
        tokenRequest.setAudience(MANAGEMENT_API_AUDIENCE);

        Token token = tokenService.getTokenByRequest(tokenRequest);

        // Set headers for request
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + token.getAccess_token());

        // Create Entity with headers
        httpEntity = new HttpEntity<>(null, httpHeaders);

        //create url
        /* UriComponents builder = UriComponentsBuilder.fromHttpUrl(MANAGEMENT_API_AUDIENCE.concat("users")).
                 queryParam("q", search)
                 .build();*/
         /*amal*/
         UriComponents builder = UriComponentsBuilder.fromHttpUrl(MANAGEMENT_API_AUDIENCE.concat("users")).
         		 queryParam("q", search)
         		 .queryParam("page",pageable.getPageNumber())
         		 .queryParam("per_page",pageable.getPageSize())
         		 .build();

        String url = builder.toUriString();

        logger.debug(url);
        logger.debug("Calling auth0 to fetch all users");

        //Send GET request with entity
        responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);

        return responseEntity.getBody();
    }
    
    @Override
    public String getAllAuthUsers(String search, String projection) throws FXDefaultException {
        int tokenRetryCount = 0;
        ResponseEntity<String> responseEntity = null;
        HttpEntity<Object> httpEntity = null;

        //Repeat request on code expiry

        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setClient_id(CLIENT_ID);
        tokenRequest.setClient_secret(CLIENT_SECRET);
        tokenRequest.setGrant_type(GRANT_TYPE);
        tokenRequest.setAudience(MANAGEMENT_API_AUDIENCE);

        Token token = tokenService.getTokenByRequest(tokenRequest);

        // Set headers for request
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + token.getAccess_token());

        // Create Entity with headers
        httpEntity = new HttpEntity<>(null, httpHeaders);

        //create url
        /* UriComponents builder = UriComponentsBuilder.fromHttpUrl(MANAGEMENT_API_AUDIENCE.concat("users")).
                 queryParam("q", search)
                 .build();*/
         /*amal*/
         UriComponents builder = UriComponentsBuilder.fromHttpUrl(MANAGEMENT_API_AUDIENCE.concat("users")).build();

        String url = builder.toUriString();

        logger.debug(url);
        logger.debug("Calling auth0 to fetch all users");

        //Send GET request with entity
        responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);

        return responseEntity.getBody();
    }

    /**
     * Sub method to convert response ro lisr
     *
     * @param id
     * @param responseEntity
     * @param classType
     * @param type
     * @param <T>
     * @return
     * @throws FXDefaultException
     */
    
    <T> List<T> convertResponseToList(String id, ResponseEntity responseEntity, Class<T> classType, String type) throws FXDefaultException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        JsonNode subNode = null;
        List<T> responseContentList = null;
        try {
            JsonNode root = objectMapper.readTree(responseEntity.getBody().toString());
            if (id != null) {
                responseContentList = new ArrayList<>();
                responseContentList.add(objectMapper.readValue(root.toString(), classType));
            } else {
                subNode = root.get(type);
                responseContentList = objectMapper.readValue(subNode.toString(), new TypeReference<List<T>>() {
                });
            }
        } catch (IOException e) {
            logger.debug("Unable to convert response body to object type");
            e.printStackTrace();
            throw new FXDefaultException("-1", "CONVERSION ERROR", e.getMessage(), new Date(), HttpStatus.BAD_REQUEST);
        }
        return responseContentList;
    }
    
    /**
     * Sub method to convert response ro lisr
     *
     * @param id
     * @param responseEntity
     * @param classType
     * @param type
     * @param <T>
     * @return
     * @throws FXDefaultException
     */
    <T> List<T> convertResponse(String id, ResponseEntity responseEntity, Class<T> classType, String type) throws FXDefaultException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        JsonNode subNode = null;
        List<T> responseContentList = null;
        try {
            JsonNode root = objectMapper.readTree(responseEntity.getBody().toString());
            if (id != null) {
                responseContentList = new ArrayList<>();
                responseContentList = objectMapper.readValue(root.toString(), new TypeReference<List<T>>() {
                });
            } else {
                subNode = root.get(type);
                responseContentList = objectMapper.readValue(subNode.toString(), new TypeReference<List<T>>() {
                });
            }
        } catch (IOException e) {
            logger.debug("Unable to convert response body to object type");
            e.printStackTrace();
            throw new FXDefaultException("-1", "CONVERSION ERROR", e.getMessage(), new Date(), HttpStatus.BAD_REQUEST);
        }
        return responseContentList;
    }

    /**
     * Sub method to convert list to page
     *
     * @param pageable
     * @param responseContentList
     * @param projectionClass
     * @param <T>
     * @return
     */
    <T> Page<?> convertListToPage(Pageable pageable, List<T> responseContentList, Class projectionClass) {

        //Convert to page and return
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), responseContentList.size());

        if (start <= end) {
            responseContentList = responseContentList.subList(start, end);
        }
        return new PageImpl<T>(responseContentList, pageable, responseContentList.size());
    }
    
    /**
     * 
     * create by Pasindu 2021-11-26
     * 
     * To get objects from auth0 provider with out page concept
     *
     * @param subPath
     * @param pageable
     * @param classType
     * @param id
     * @param type
     * @param projectionClass
     * @param <T>
     * @return
     * @throws FXDefaultException
     */
    
    @Override
    public <T> List<?> sendProviderGetRequestAswithoutPage(String subPath, Pageable pageable, Class<T> classType, String id, String type, Class<?> projectionClass) throws FXDefaultException {
        ResponseEntity responseEntity = null;
        int tokenRetryCount = 0;
        List<?> responseContentList = null;
        
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        //Repeat request on code expiry
        do {
            RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                    .build();

            //Set headers for request
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Authorization", "Bearer " + Token.token.getAccess_token());

            //Create Entity with headers
            HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

            //Send GET request with entity
            responseEntity =
                    restTemplate.exchange(String.format(EXTENSION_API_URL, subPath),
                            HttpMethod.GET, httpEntity, String.class);
            if (responseEntity.getStatusCodeValue() == 401) {
                tokenService.getExtensionAppToken();
                logger.debug("Token expired, retrying request");
                tokenRetryCount += 1;
            }
        } while (responseEntity.getStatusCodeValue() == 401 && tokenRetryCount < 3);

        //Throw exception after 3 tries to get token
        if (tokenRetryCount == 3) {
            throw new FXDefaultException("", "", "Unable to process Request due to invalid token", new Date(), HttpStatus.BAD_REQUEST);
        }

        //map response body to type
        responseContentList = convertResponseToList(id, responseEntity, classType, type);

        
        return responseContentList;
    }
    
    /**
     * 
     * 
     * To get objects from auth0 provider with out page concept
     *
     * @param subPath
     * @param pageable
     * @param classType
     * @param id
     * @param type
     * @param projectionClass
     * @param <T>
     * @return
     * @throws FXDefaultException
     */
    @Override
    public <T> List<?> sendProviderGetRequest(String subPath, Pageable pageable, Class<T> classType, String id, String type, Class<?> projectionClass) throws FXDefaultException {
        ResponseEntity responseEntity = null;
        int tokenRetryCount = 0;
        List<?> responseContentList = null;

        //Repeat request on code expiry
        do {
            RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                    .build();

            //Set headers for request
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Authorization", "Bearer " + Token.token.getAccess_token());

            //Create Entity with headers
            HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

            //Send GET request with entity
            responseEntity =
                    restTemplate.exchange(String.format(EXTENSION_API_URL, subPath),
                            HttpMethod.GET, httpEntity, String.class);
            if (responseEntity.getStatusCodeValue() == 401) {
                tokenService.getExtensionAppToken();
                logger.debug("Token expired, retrying request");
                tokenRetryCount += 1;
            }
        } while (responseEntity.getStatusCodeValue() == 401 && tokenRetryCount < 3);

        //Throw exception after 3 tries to get token
        if (tokenRetryCount == 3) {
            throw new FXDefaultException("", "", "Unable to process Request due to invalid token", new Date(), HttpStatus.BAD_REQUEST);
        }

        //map response body to type
        responseContentList = convertResponse(id, responseEntity, classType, type);

        
        return responseContentList;
    }

}
