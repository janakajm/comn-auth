package com.loits.comn.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserMap {
	private AppMetaData app_metadata;

	public AppMetaData getApp_metadata() {
		return app_metadata;
	}

	public void setApp_metadata(AppMetaData app_metadata) {
		this.app_metadata = app_metadata;
	}
	
}
