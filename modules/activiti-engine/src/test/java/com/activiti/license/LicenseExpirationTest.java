package com.activiti.license;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author jbarrez
 */
public class LicenseExpirationTest {
  
  @Test
  public void testProcessInstanceLimit() {
    
    // The license at the location below is limited to 2013
    
    try {
      new StandaloneInMemProcessEngineConfiguration() {
        
        @Override
        protected void initLicenseHolder() {
          super.initLicenseHolder();
          licenseHolder.setCustomLocationClassPath("com/activiti/license/expired.lic");
        }
        
      }.buildProcessEngine();
      Assert.fail("Expecting exception due to invalid license");
    } catch (ActivitiException e) { 
      Assert.assertEquals(e.getMessage(), "Cannot create process engine: invalid license");
    }
  }

}
