package com.activiti.license;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author jbarrez
 */
public class DepartementalProcessInstanceLimitTest {
  
  private static ProcessEngine processEngine;
  
  @BeforeClass
  public static void setupProcessEngine() {
    
    // The license at the location below is limited to 150 process instances
    
    // Build engine
    processEngine = new StandaloneInMemProcessEngineConfiguration() {
      
      @Override
      protected void initLicenseHolder() {
        super.initLicenseHolder();
        licenseHolder.setCustomLocationClassPath("com/activiti/license/limited.lic");
      }
      
    }.buildProcessEngine();
    
    // Deploy test process
    deployOneTaskTestProcess();
  }
  
  @AfterClass
  public static void cleanup() {
    for (Deployment deployment : processEngine.getRepositoryService().createDeploymentQuery().list()) {
      processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
    }
  }
  
  @Test
  public void testExpiredLicense() {
    
    RuntimeService runtimeService = processEngine.getRuntimeService();
    TaskService taskService = processEngine.getTaskService();
    
    // Starting 100 processes should be ok
    for (int i=0; i<150; i++) {
      runtimeService.startProcessInstanceByKey("oneTaskProcess");
    }
    
    // Completing 50 tasks
    for (int i=0; i<50; i++) {
      taskService.complete(taskService.createTaskQuery().list().get(0).getId());
    }
    
    // Starting 50 should be fine again
    for (int i=0; i<50; i++) {
      runtimeService.startProcessInstanceByKey("oneTaskProcess");
    }
    
    // But starting a 151th should fail
    try {
      runtimeService.startProcessInstanceByKey("oneTaskProcess");
      Assert.fail("Expected failure due to departemental license limiting number of process instances");
    } catch (ActivitiException e) {
      Assert.assertEquals(e.getMessage(), "License exception: process instance limit (150) reached");
    }
  }
  
  private static String deployOneTaskTestProcess() {
    BpmnModel bpmnModel = createOneTaskTestProcess();
    Deployment deployment = processEngine.getRepositoryService().createDeployment()
        .addBpmnModel("oneTasktest.bpmn20.xml", bpmnModel).deploy();
    
    ProcessDefinition processDefinition = processEngine.getRepositoryService().createProcessDefinitionQuery()
        .deploymentId(deployment.getId()).singleResult();
    return processDefinition.getId(); 
  }
  
  private static BpmnModel createOneTaskTestProcess() {
    BpmnModel model = new BpmnModel();
    org.activiti.bpmn.model.Process process = new org.activiti.bpmn.model.Process();
    model.addProcess(process);
    process.setId("oneTaskProcess");
    process.setName("The one task process");
   
    StartEvent startEvent = new StartEvent();
    startEvent.setId("start");
    process.addFlowElement(startEvent);
    
    UserTask userTask = new UserTask();
    userTask.setName("The Task");
    userTask.setId("theTask");
    userTask.setAssignee("kermit");
    process.addFlowElement(userTask);
    
    EndEvent endEvent = new EndEvent();
    endEvent.setId("theEnd");
    process.addFlowElement(endEvent);;
    
    process.addFlowElement(new SequenceFlow("start", "theTask"));
    process.addFlowElement(new SequenceFlow("theTask", "theEnd"));
    
    return model;
  }

}
