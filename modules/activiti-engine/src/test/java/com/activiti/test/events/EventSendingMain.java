package com.activiti.test.events;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

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
	  processEngineConfig.setAsyncExecutorEnabled(true);
	  processEngineConfig.setAsyncExecutorActivate(true);
	  
	  
	  processEngineConfig.enableClusterConfig();
	  processEngineConfig.setEnterpriseAdminAppUrl("http://localhost:8081/activiti-admin");
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
	  
	  deployProcessDefinitions(repositoryService);
	  
//	  performanceTest(runtimeService, taskService, managementService);
	  
	  // To start with something
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);
	  startProcessInstance(runtimeService);

	  // Type anything to stop
	  new Thread(new Runnable() {
			public void run() {
			  Scanner scanner = new Scanner(System.in);
			  scanner.nextLine();
			  System.exit(0);
			}
		}).start();
	  
	  Random random = new Random();
	  while (true) {
	  	
	  	Date time = new Date(new Date().getTime() - (  ((long)random.nextInt(30)) * ((long)random.nextInt(24 * 60 * 60 * 1000)) ));
	  	processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(time);
	  	
	  	try {
		  	int nr = random.nextInt(4);
		  	if (nr == 0) {
		  		System.out.println("Starting process instance");
		  		startProcessInstance(runtimeService);
		  	} else if (nr == 1) {
		  		System.out.println("Executing task");
		  		executeTask(taskService);
		  	} else if (nr == 2) {
		  		System.out.println("executing job");
		  		executeJob(managementService);
		  	} else if (nr == 3) {
		  		if (random.nextBoolean()) { // not TOO many of them
		  			System.out.println("Executing failing job");
		  			executeFailingJob(runtimeService);
		  		}
		  	}
	  	} catch (Exception e) {
	  		
	  	}
	  	
	  	
	  	Thread.sleep(random.nextInt(1000));
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
	
	private static void deployProcessDefinitions(RepositoryService repositoryService) {
		
		// Regular proc
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
  			.addBpmnModel("oneTasktest.bpmn20.xml", model)
  			.addString("asyncFail.bpmn20.xml", FAILING_AYNC_TASK)
  			.deploy();

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
	
	private static void executeFailingJob(RuntimeService runtimeService) {
		runtimeService.startProcessInstanceByKey("asyncScript");
	}
	
	private static final String FAILING_AYNC_TASK = "<?xml version='1.0' encoding='UTF-8'?>\n" + 
			"<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:activiti=\"http://activiti.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://www.activiti.org/processdef\" xmlns:modeler=\"http://activiti.com/modeler\" modeler:version=\"1.0en\" modeler:exportDateTime=\"20141216191911527\" modeler:modelId=\"923565\" modeler:modelVersion=\"1\" modeler:modelLastUpdated=\"1418757549140\">\n" + 
			"  <process id=\"asyncScript\" name=\"asyncScript\" isExecutable=\"true\">\n" + 
			"    <startEvent id=\"startEvent1\"/>\n" + 
			"    <sequenceFlow id=\"sid-1A341B40-2063-4D35-A607-4E0DBD35C3A0\" sourceRef=\"startEvent1\" targetRef=\"sid-B1E9D667-AB66-444E-9346-36A8FA71C68E\"/>\n" + 
			"    <scriptTask id=\"sid-B1E9D667-AB66-444E-9346-36A8FA71C68E\" name=\"The Script task\" activiti:async=\"true\" activiti:exclusive=\"false\" scriptFormat=\"javascript\" activiti:autoStoreVariables=\"false\">\n" + 
			"      <script>this should really fail!</script>\n" + 
			"    </scriptTask>\n" + 
			"    <userTask id=\"sid-9F529DCE-73D6-4281-BD31-B002F5DF8808\" name=\"Task after script\"/>\n" + 
			"    <sequenceFlow id=\"sid-C839C9DA-E1B4-4CAB-A0FD-1022101384A3\" sourceRef=\"sid-B1E9D667-AB66-444E-9346-36A8FA71C68E\" targetRef=\"sid-9F529DCE-73D6-4281-BD31-B002F5DF8808\"/>\n" + 
			"    <endEvent id=\"sid-586727F5-599B-41E8-BA3B-7FF739A74A18\"/>\n" + 
			"    <sequenceFlow id=\"sid-E35E9BA7-5BC4-4B04-A3B9-7B47492E25CD\" sourceRef=\"sid-9F529DCE-73D6-4281-BD31-B002F5DF8808\" targetRef=\"sid-586727F5-599B-41E8-BA3B-7FF739A74A18\"/>\n" + 
			"  </process>\n" + 
			"  <bpmndi:BPMNDiagram id=\"BPMNDiagram_asyncScript\">\n" + 
			"    <bpmndi:BPMNPlane bpmnElement=\"asyncScript\" id=\"BPMNPlane_asyncScript\">\n" + 
			"      <bpmndi:BPMNShape bpmnElement=\"startEvent1\" id=\"BPMNShape_startEvent1\">\n" + 
			"        <omgdc:Bounds height=\"30.0\" width=\"30.0\" x=\"100.0\" y=\"163.0\"/>\n" + 
			"      </bpmndi:BPMNShape>\n" + 
			"      <bpmndi:BPMNShape bpmnElement=\"sid-B1E9D667-AB66-444E-9346-36A8FA71C68E\" id=\"BPMNShape_sid-B1E9D667-AB66-444E-9346-36A8FA71C68E\">\n" + 
			"        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"175.0\" y=\"138.0\"/>\n" + 
			"      </bpmndi:BPMNShape>\n" + 
			"      <bpmndi:BPMNShape bpmnElement=\"sid-9F529DCE-73D6-4281-BD31-B002F5DF8808\" id=\"BPMNShape_sid-9F529DCE-73D6-4281-BD31-B002F5DF8808\">\n" + 
			"        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"320.0\" y=\"138.0\"/>\n" + 
			"      </bpmndi:BPMNShape>\n" + 
			"      <bpmndi:BPMNShape bpmnElement=\"sid-586727F5-599B-41E8-BA3B-7FF739A74A18\" id=\"BPMNShape_sid-586727F5-599B-41E8-BA3B-7FF739A74A18\">\n" + 
			"        <omgdc:Bounds height=\"28.0\" width=\"28.0\" x=\"465.0\" y=\"164.0\"/>\n" + 
			"      </bpmndi:BPMNShape>\n" + 
			"      <bpmndi:BPMNEdge bpmnElement=\"sid-1A341B40-2063-4D35-A607-4E0DBD35C3A0\" id=\"BPMNEdge_sid-1A341B40-2063-4D35-A607-4E0DBD35C3A0\">\n" + 
			"        <omgdi:waypoint x=\"130.0\" y=\"178.0\"/>\n" + 
			"        <omgdi:waypoint x=\"175.0\" y=\"178.0\"/>\n" + 
			"      </bpmndi:BPMNEdge>\n" + 
			"      <bpmndi:BPMNEdge bpmnElement=\"sid-C839C9DA-E1B4-4CAB-A0FD-1022101384A3\" id=\"BPMNEdge_sid-C839C9DA-E1B4-4CAB-A0FD-1022101384A3\">\n" + 
			"        <omgdi:waypoint x=\"275.0\" y=\"178.0\"/>\n" + 
			"        <omgdi:waypoint x=\"320.0\" y=\"178.0\"/>\n" + 
			"      </bpmndi:BPMNEdge>\n" + 
			"      <bpmndi:BPMNEdge bpmnElement=\"sid-E35E9BA7-5BC4-4B04-A3B9-7B47492E25CD\" id=\"BPMNEdge_sid-E35E9BA7-5BC4-4B04-A3B9-7B47492E25CD\">\n" + 
			"        <omgdi:waypoint x=\"420.0\" y=\"178.0\"/>\n" + 
			"        <omgdi:waypoint x=\"465.0\" y=\"178.0\"/>\n" + 
			"      </bpmndi:BPMNEdge>\n" + 
			"    </bpmndi:BPMNPlane>\n" + 
			"  </bpmndi:BPMNDiagram>\n" + 
			"</definitions>";
	
}
