package com.loits.comn.auth.services.impl;

import com.loits.comn.auth.config.Translator;
import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.core.RestTemplateResponseErrorHandler;
import com.loits.comn.auth.dto.RHSSOTokenResponse;
import com.loits.comn.auth.dto.Token;
import com.loits.comn.auth.dto.TokenRequest;
import com.loits.comn.auth.services.TokenService;
//import org.apache.http.HttpHeaders;
import org.codehaus.groovy.syntax.TokenException;
import org.springframework.http.HttpHeaders;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;


@Service
public class TokenServiceImpl implements TokenService {

    @Value("${auth0.authorization.extension.api.token.url}")
    private String TOKEN_URL;

    @Value("${auth0.authorization.extension.app.client-id}")
    private String CLIENT_ID;

    @Value("${auth0.authorization.extension.app.client-secret}")
    private String CLIENT_SECRET;

    @Value("${auth0.authorization.extension.api.token.grant-type}")
    private String GRANT_TYPE;

    @Value("${auth0.authorization.extension.api.identifier}")
    private String EXTENSION_API_AUDIENCE;

    @Value("http://localhost:8081/auth/realms/master/protocol/openid-connect/token")
    private String tokenUrl;

    @Value("${rh-sso-client-id}")
    private String clientId;

    @Value("${rh-sso-username}")
    private String userName;

    @Value("${rh-sso-password}")
    private String password;

    @Value("${rh-sso-grant-type}")
    private String grantType;

    @Autowired
    RestTemplateBuilder builder;

    Logger logger = LogManager.getLogger(TokenServiceImpl.class);

    @PostConstruct
    public void init() throws FXDefaultException {
        //Create instance of token (singleton) on startup
        getExtensionAppToken();
    }

    @Override
    public void getExtensionAppToken() throws FXDefaultException {
        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();
        //Create new token request for Authorization Extension M-to-M Application
        TokenRequest tokenRequest = new TokenRequest(CLIENT_ID,
                CLIENT_SECRET, GRANT_TYPE, EXTENSION_API_AUDIENCE);

        HttpEntity<TokenRequest> httpEntity = new HttpEntity<>(tokenRequest);

        //Get token by POST request to auth extension API
        Token.token = restTemplate.postForObject(TOKEN_URL, httpEntity, Token.class);

        logger.debug("TOKEN_SERVICE",Token.token.getAccess_token());
        logger.info("TOKEN_SERVICE",Token.token.getAccess_token());
        logger.warn("TOKEN_SERVICE",Token.token.getAccess_token());

        if(Token.token.getAccess_token()==null){
            throw new FXDefaultException("-1","TOKEN_ERROR", Translator.toLocale("TOKEN_ERROR"), new Date(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public Token getTokenByRequest(TokenRequest tokenRequest) throws FXDefaultException {
        RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        HttpEntity<TokenRequest> httpEntity = new HttpEntity<>(tokenRequest);

        //Get token by POST request to auth extension API
        Token token = restTemplate.postForObject(TOKEN_URL, httpEntity, Token.class);

        if(token.getAccess_token()==null){
            throw new FXDefaultException("-1","TOKEN_ERROR", Translator.toLocale("TOKEN_ERROR"), new Date(), HttpStatus.BAD_REQUEST);
        }
        return token;
    }

    @Override
    public String getRHSSOToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id",clientId);
        map.add("username",userName);
        map.add("password",password);
        map.add("grant_type",grantType);
        map.add("Content-Type","application/x-www-form-urlencoded");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
        //RestTemplate restTemplate = this.builder.errorHandler(new RestTemplateResponseErrorHandler()).build();
        
        RestTemplate restTemplate =null;
		try {
			TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
			 
		    SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
		                    .loadTrustMaterial(null, acceptingTrustStrategy)
		                    .build();
		
		    SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
		
		    CloseableHttpClient httpClient = HttpClients.custom()
		                    .setSSLSocketFactory(csf)
		                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
		                    .build();
		
		    HttpComponentsClientHttpRequestFactory requestFactory =
		                    new HttpComponentsClientHttpRequestFactory();
		    
		  //FIXME this should be for SBA client only
			HttpsURLConnection.setDefaultHostnameVerifier(
					//SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER
					// * @deprecated (4.4) Use {@link org.apache.http.conn.ssl.NoopHostnameVerifier}
					new NoopHostnameVerifier()
			);
		
		    requestFactory.setHttpClient(httpClient);
		    restTemplate = new RestTemplate(requestFactory);
		} catch (Exception e) {
			// TODO: handle exception
		}
        RHSSOTokenResponse rhssoTokenResponse=restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, RHSSOTokenResponse.class).getBody();
        if( rhssoTokenResponse != null )
            return rhssoTokenResponse.getToken();
        else return null;
    }


}
