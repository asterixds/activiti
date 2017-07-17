package com.activiti.license;

import static org.junit.Assert.fail;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.activiti.engine.test.util.TestProcessUtil;
import org.junit.Test;

/**
 * @author jbarrez
 */
public class LicenseExpirationTest {
  
  @Test
  public void testProcessInstanceLimit() {
    
    // The license at the location below is limited to 2013
    
    ProcessEngine processEngine = new StandaloneInMemProcessEngineConfiguration() {
        
      @Override
      protected void initLicenseHolder() {
        super.initLicenseHolder();
        licenseHolder.setCustomLocationClassPath("com/activiti/license/expired.lic");
      }
      
    }.setDatabaseSchemaUpdate("true").setProcessEngineName("expired").buildProcessEngine();
    
    processEngine.getRepositoryService().createDeployment().addBpmnModel(
        "onetask.bpmn", TestProcessUtil.createOneTaskBpmnModel()).deploy();
    
    try {
      processEngine.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");
      fail("expired license should prevent process from being started");
    } catch (Exception e) {
      // expected exception
    }
    
    processEngine.close();
    ProcessEngines.unregister(processEngine);
  }

}
