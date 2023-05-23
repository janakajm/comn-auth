package com.loits.comn.auth.commons;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class HealthCheck {

  @Value("${api-version}")
  private String apiVersion;

  private HealthStatus status;


  public HealthCheck() {
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public HealthStatus getStatus() {
    this.status = new HealthStatus(new Date(), this.apiVersion);
    return status;
  }

  public void setStatus(HealthStatus status) {
    this.status = status;
  }

  public class HealthStatus {
    private Date serverTime;
    private String apiVersion;

    public HealthStatus(Date serverTime, String apiVersion) {
      this.serverTime = serverTime;
      this.apiVersion = apiVersion;
    }

    public Date getServerTime() {
      return serverTime;
    }

    public void setServerTime(Date serverTime) {
      this.serverTime = serverTime;
    }

    public String getApiVersion() {
      return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
      this.apiVersion = apiVersion;
    }
  }
}