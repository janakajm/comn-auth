package com.loits.comn.auth.services;

import com.loits.comn.auth.core.FXDefaultException;
import com.loits.comn.auth.dto.Token;
import com.loits.comn.auth.dto.TokenRequest;
import org.codehaus.groovy.syntax.TokenException;
import org.springframework.data.domain.Pageable;

import javax.validation.Valid;

public interface TokenService {

    void getExtensionAppToken() throws FXDefaultException;

    Token getTokenByRequest(TokenRequest tokenRequest) throws FXDefaultException;

    String getRHSSOToken() throws TokenException;
}
