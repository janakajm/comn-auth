package com.loits.comn.auth.dto;

import lombok.Data;

@Data
public class TokenRequest {
    private String client_id;
    private String client_secret;
    private String grant_type;
    private String audience;

    public TokenRequest(String client_id, String client_secret, String grant_type, String audience) {
        this.client_id = client_id;
        this.client_secret = client_secret;
        this.grant_type = grant_type;
        this.audience = audience;
    }

    public TokenRequest() {
    }
}
