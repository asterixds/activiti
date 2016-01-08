package com.activiti.rest.service.api;


/**
 * @author Frederik Heremans
 * @author Erik Winlof
 */
public class EnterpriseLicenseResponse {

  public static final String STATUS_VALID = "valid";
  public static final String STATUS_NOT_FOUND = "not-found";
  public static final String STATUS_INVALID_DATE = "invalid-date";

  private String status;
  private String holder;
  private String licenseCheck;
  
  public void setHolder(String holder) {
    this.holder = holder;
  }

  public String getHolder() {
    return holder;
  }

  public void setLicenseCheck(String lisenceCheck) {
    this.licenseCheck = lisenceCheck;
  }

  public String getLicenseCheck() {
    return licenseCheck;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
