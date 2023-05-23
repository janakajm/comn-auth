package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Token {
    public static Token token;

    private String access_token;
    private String scope;
    private Long expires_in;
    private String token_type;

    public Token() {

    }

    public Token(String access_token, String scope, Long expires_in, String token_type) {
        this.access_token = access_token;
        this.scope = scope;
        this.expires_in = expires_in;
        this.token_type = token_type;
    }

    public static Token getInstance(){
        if(token==null){
            token = new Token();
        }
        return token;
    }

}
