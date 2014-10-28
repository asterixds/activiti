package com.activiti.rest.api.disco;

import org.activiti.rest.service.BaseSpringRestTestCase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.springframework.http.HttpStatus;

import com.activiti.rest.service.api.EnterpriseLicenseResponse;

public class EnterpriseLicenseResourceTest extends BaseSpringRestTestCase {
  
  /**
   * Test getting the license.
   */
  public void testGetLicense() throws Exception {
    HttpGet request = new HttpGet(SERVER_URL_PREFIX + "management/engine/license");
    CloseableHttpResponse response = executeRequest(request, HttpStatus.OK.value());
    
    EnterpriseLicenseResponse license = objectMapper.readValue(
        response.getEntity().getContent(), EnterpriseLicenseResponse.class);
    assertNotNull(license.getHolder());
    assertNotNull(license.getLicenseCheck());
    closeResponse(response);
  }
}
