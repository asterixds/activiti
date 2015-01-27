package com.activiti.test.events;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.activiti.bpmn.model.BoundaryEvent;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.EventDefinition;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.TimerEventDefinition;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.task.Task;

/**
 * Simple main that can be run to get quickly events in the admin app 
 */
public class EventSendingMain {

	public static void main(String[] args) throws Exception {
	  ProcessEngineConfigurationImpl processEngineConfig = 
	  		(ProcessEngineConfigurationImpl) new StandaloneInMemProcessEngineConfiguration();
	  
//	  processEngineConfig.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/act-perf?characterEncoding=UTF-8");
//	  processEngineConfig.setJdbcDriver("com.mysql.jdbc.Driver");
//	  processEngineConfig.setJdbcUsername("alfresco");
//	  processEngineConfig.setJdbcPassword("alfresco");
//	  processEngineConfig.setDatabaseSchema("drop-create");
//	  
	  processEngineConfig.setJobExecutorActivate(false);
	  processEngineConfig.setAsyncExecutorEnabled(false);
	  processEngineConfig.setAsyncExecutorActivate(false);
	  
	  
	  processEngineConfig.enableClusterConfig();
	  processEngineConfig.setEnterpriseClusterName("development");
	  processEngineConfig.setEnterpriseClusterUserName("dev");
	  processEngineConfig.setEnterpriseClusterPassword("dev");
	  processEngineConfig.setEnterpriseMasterConfigurationRequired(false);
	  processEngineConfig.setEnterpriseMetricSendingInterval(10);
	  
	  final ProcessEngine processEngine = processEngineConfig.buildProcessEngine();
	  
	  Runtime.getRuntime().addShutdownHook(new Thread() {
	  	@Override
	  	public void run() {
	  		processEngine.close();
	  	}
	  });
	  
	  RepositoryService repositoryService = processEngine.getRepositoryService();
	  RuntimeService runtimeService = processEngine.getRuntimeService();
	  TaskService taskService = processEngine.getTaskService();
	  ManagementService managementService = processEngine.getManagementService();
	  
	  deployProcessDefinition(repositoryService);
	  
//	  performanceTest(runtimeService, taskService, managementService);
	  
	  // To start with something
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);
	  
	  Random random = new Random();
	  while (true) {
	  	
	  	Date time = new Date(new Date().getTime() - (  ((long)random.nextInt(30)) * ((long)random.nextInt(24 * 60 * 60 * 1000)) ));
	  	processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(time);
	  	int nr = random.nextInt(3);
	  	if (nr == 0) {
	  		System.out.println("Starting process instance");
	  		startProcessInstance(runtimeService);
	  	} else if (nr == 1) {
	  		System.out.println("Executing task");
	  		executeTask(taskService);
	  	} else if (nr == 2) {
	  		System.out.println("executing job");
	  		executeJob(managementService);
	  	}
	  	
	  	
	  	Thread.sleep(random.nextInt(10000));
	  }
	  
  }

	private static void performanceTest(RuntimeService runtimeService,
      TaskService taskService, ManagementService managementService) {
	  int iterations = 50;
	  long start = System.currentTimeMillis();
	  for (int j=0; j<iterations; j++) {
	  	System.out.println("Iteration " + j + "/" + iterations);
	  
		  for (int i=0; i<250; i++) {
		  	startProcessInstance(runtimeService);
		  }
		  
		  for (int i=0; i<10; i++) {
		  	executeTask(taskService);
		  }
		  
		  for (int i=0; i<5; i++) {
		  	executeJob(managementService);
		  }
	  }
	  long end = System.currentTimeMillis();
	  System.out.println("-->" + (end-start) + " ms in total");
  }
	
	private static void deployProcessDefinition(RepositoryService repositoryService) {
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
		
		BoundaryEvent boundaryEvent = new BoundaryEvent();
		boundaryEvent.setId("timer");
		boundaryEvent.setAttachedToRef(userTask);
		boundaryEvent.setAttachedToRefId("theTask");
		TimerEventDefinition timerEventDefinition = new TimerEventDefinition();
		timerEventDefinition.setTimeDuration("PT5S");
		List<EventDefinition> eventDefinitions = new ArrayList<EventDefinition>();
		eventDefinitions.add(timerEventDefinition);
		boundaryEvent.setEventDefinitions(eventDefinitions);
		process.addFlowElement(boundaryEvent);

		EndEvent endEvent = new EndEvent();
		endEvent.setId("theEnd");
		process.addFlowElement(endEvent);

		process.addFlowElement(new SequenceFlow("start", "theTask"));
		process.addFlowElement(new SequenceFlow("theTask", "theEnd"));
		process.addFlowElement(new SequenceFlow("timer", "theEnd"));
		
  	Deployment deployment = repositoryService.createDeployment()
  			.addBpmnModel("oneTasktest.bpmn20.xml", model).deploy();

	}
	
	private static void startProcessInstance(RuntimeService runtimeService) {
		runtimeService.startProcessInstanceByKey("oneTaskProcess");
	}
	
	private static void executeTask(TaskService taskService) {
		List<Task> tasks = taskService.createTaskQuery().list();
		if (tasks.size() > 0) {
			taskService.complete(tasks.get(0).getId());
		}
	}
	
	private static void executeJob(ManagementService managementService) {
		List<Job> jobs = managementService.createJobQuery().list();
		if (jobs.size() > 0) {
			managementService.executeJob(jobs.get(0).getId());
		}
	}
	
}
