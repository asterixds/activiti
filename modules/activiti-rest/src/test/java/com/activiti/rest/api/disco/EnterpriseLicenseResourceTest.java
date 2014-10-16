package com.activiti.rest.api.disco;

import org.activiti.rest.service.BaseSpringRestTestCase;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.springframework.http.HttpStatus;

import com.activiti.rest.service.api.EnterpriseLicenseResponse;

public class EnterpriseLicenseResourceTest extends BaseSpringRestTestCase {
  
  /**
   * Test getting the license.
   */
  public void testGetLicense() throws Exception {
    HttpResponse response = executeHttpRequest(new HttpGet(SERVER_URL_PREFIX + 
        "management/engine/license"), HttpStatus.OK.value());
    
    EnterpriseLicenseResponse license = objectMapper.readValue(
        response.getEntity().getContent(), EnterpriseLicenseResponse.class);
    assertNotNull(license.getHolder());
    assertNotNull(license.getLicenseCheck());
  }
}
