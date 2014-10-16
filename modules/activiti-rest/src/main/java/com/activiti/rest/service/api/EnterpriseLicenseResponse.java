package com.activiti.rest.service.api;


/**
 * @author Frederik Heremans
 */
public class EnterpriseLicenseResponse {

  private String holder;
  private String licenseCheck;
  
  public void setHolder(String holder) {
    this.holder = holder;
  }
  
  public void setLicenseCheck(String lisenceCheck) {
    this.licenseCheck = lisenceCheck;
  }
  
  public String getHolder() {
    return holder;
  }
  
  
  public String getLicenseCheck() {
    return licenseCheck;
  }
}
