package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapScopeGroups {

    @NotBlank(message = "common.not-null")
    private String realm;
    
    @NotBlank(message = "common.not-null")
    private String client;
    
    @NotBlank(message = "common.not-null")
    private String clientScopeId;

    @NotBlank(message = "common.not-null")
    private String httpMethod;
    

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getClientScopeId() {
		return clientScopeId;
	}

	public void setClientScopeId(String clientScopeId) {
		this.clientScopeId = clientScopeId;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}
    
}
