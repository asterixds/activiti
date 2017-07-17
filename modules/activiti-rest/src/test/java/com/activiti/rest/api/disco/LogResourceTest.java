package com.activiti.rest.api.disco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.rest.service.BaseSpringRestTestCase;
import org.activiti.rest.service.api.RestUrls;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.activiti.rest.service.api.EnterpriseRestUrls;


/**
 * Test for all REST-operations related to a Disco catalog resource.
 * 
 * @author Tijs Rademakers
 */
public class LogResourceTest extends BaseSpringRestTestCase {
  
  /**
   * Test getting the log with summary traces.
   * GET disco/log/{processDefinitionId}.dxml
   */
  public void testGetLogSummary() throws Exception {
    Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource("com/activiti/rest/api/disco/oneTaskProcess.bpmn20.xml")
        .addClasspathResource("com/activiti/rest/api/disco/anotherOneTaskProcess.bpmn20.xml")
        .addClasspathResource("com/activiti/rest/api/disco/simpleProcess.bpmn20.xml")
        .deploy();
    
    for (int i = 0; i < 5; i++) {
      Map<String, Object> variableMap = new HashMap<String, Object>();
      variableMap.put("name", "test");
      variableMap.put("number", 123);
      if (i == 0) {
        variableMap.put("extraName", "test2");
      }
      runtimeService.startProcessInstanceByKey("simple", variableMap);
    }
    
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("oneTaskProcess");
    }
    
    String simpleId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("simple").singleResult().getId();
    String oneTaskProcessId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult().getId();
    String anotherOneTaskProcessId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("anotherOneTaskProcess").singleResult().getId();
    
    HttpGet request = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(
        EnterpriseRestUrls.URL_DISCO_LOG, simpleId) + "?scope=summary");
    request.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/xml"));
    CloseableHttpResponse response = executeBinaryRequest(request, HttpStatus.OK.value());
    
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(response.getEntity().getContent());
    closeResponse(response);
    
    Element log = doc.getDocumentElement();
    assertEquals("log", log.getNodeName());
    assertEquals("Simple Process", log.getAttribute("name"));
    assertEquals("5", log.getAttribute("traces"));
    assertEquals("15", log.getAttribute("events"));
    Element description = (Element) log.getElementsByTagName("description").item(0);
    assertEquals("Simple Process", description.getTextContent());
    
    Element globalsElement = (Element) log.getElementsByTagName("globals").item(0);
    NodeList traceAttributeList = globalsElement.getElementsByTagName("traceAttribute");
    Map<String, String> traceMap = new HashMap<String, String>();
    for (int i = 0; i < traceAttributeList.getLength(); i++) {
      Element attributeElement = (Element) traceAttributeList.item(i);
      traceMap.put(attributeElement.getAttribute("key"), attributeElement.getAttribute("type"));
    }
    
    assertEquals("string", traceMap.get("name"));
    assertEquals("string", traceMap.get("extraName"));
    assertEquals("integer", traceMap.get("number"));
    
    NodeList eventAttributeList = globalsElement.getElementsByTagName("eventAttribute");
    Map<String, Element> eventAttributeMap = new HashMap<String, Element>();
    for (int i = 0; i < eventAttributeList.getLength(); i++) {
      Element attributeElement = (Element) eventAttributeList.item(i);
      eventAttributeMap.put(attributeElement.getAttribute("key"), attributeElement);
    }
    
    Element eventAttributeElement = eventAttributeMap.get("activityId");
    assertEquals("string", eventAttributeElement.getAttribute("type"));
    assertEquals("", eventAttributeElement.getAttribute("classifier"));
    eventAttributeElement = eventAttributeMap.get("activityName");
    assertEquals("string", eventAttributeElement.getAttribute("type"));
    assertEquals("activity", eventAttributeElement.getAttribute("classifier"));
    eventAttributeElement = eventAttributeMap.get("assignee");
    assertEquals("string", eventAttributeElement.getAttribute("type"));
    assertEquals("resource", eventAttributeElement.getAttribute("classifier"));
    eventAttributeElement = eventAttributeMap.get("activityType");
    assertEquals("string", eventAttributeElement.getAttribute("type"));
    assertEquals("", eventAttributeElement.getAttribute("classifier"));
    
    NodeList traceList = log.getElementsByTagName("trace");
    assertEquals(0, traceList.getLength());
    
    request = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(
        EnterpriseRestUrls.URL_DISCO_LOG, oneTaskProcessId));
    request.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/xml"));
    response = executeBinaryRequest(request, HttpStatus.OK.value());
    
    dbFactory = DocumentBuilderFactory.newInstance();
    dBuilder = dbFactory.newDocumentBuilder();
    doc = dBuilder.parse(response.getEntity().getContent());
    closeResponse(response);
    
    log = doc.getDocumentElement();
    assertEquals("log", log.getNodeName());
    assertEquals("The One Task Process", log.getAttribute("name"));
    assertEquals("3", log.getAttribute("traces"));
    assertEquals("6", log.getAttribute("events"));
    description = (Element) log.getElementsByTagName("description").item(0);
    assertEquals("This is a process for testing purposes", description.getTextContent());
    
    globalsElement = (Element) log.getElementsByTagName("globals").item(0);
    traceAttributeList = globalsElement.getElementsByTagName("traceAttribute");
    assertEquals(0, traceAttributeList.getLength());
    
    request = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(
        EnterpriseRestUrls.URL_DISCO_LOG, anotherOneTaskProcessId));
    request.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/xml"));
    response = executeBinaryRequest(request, HttpStatus.OK.value());
    
    dbFactory = DocumentBuilderFactory.newInstance();
    dBuilder = dbFactory.newDocumentBuilder();
    doc = dBuilder.parse(response.getEntity().getContent());
    closeResponse(response);
    
    log = doc.getDocumentElement();
    assertEquals("log", log.getNodeName());
    assertEquals("The One Task Process", log.getAttribute("name"));
    assertEquals("0", log.getAttribute("traces"));
    assertEquals("0", log.getAttribute("events"));
    description = (Element) log.getElementsByTagName("description").item(0);
    assertEquals("This is a process for testing purposes", description.getTextContent());
    
    globalsElement = (Element) log.getElementsByTagName("globals").item(0);
    traceAttributeList = globalsElement.getElementsByTagName("traceAttribute");
    assertEquals(0, traceAttributeList.getLength());
    
    repositoryService.deleteDeployment(deployment.getId(), true);
  }
  
  /**
   * Test getting the log with scope traces.
   * GET disco/log/{processDefinitionId}.dxml?scope=traces
   */
  public void testGetLogTraces() throws Exception {
    Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource("com/activiti/rest/api/disco/oneTaskProcess.bpmn20.xml")
        .addClasspathResource("com/activiti/rest/api/disco/anotherOneTaskProcess.bpmn20.xml")
        .addClasspathResource("com/activiti/rest/api/disco/simpleProcess.bpmn20.xml")
        .deploy();
    
    for (int i = 0; i < 5; i++) {
      Map<String, Object> variableMap = new HashMap<String, Object>();
      variableMap.put("name", "test");
      variableMap.put("number", 123);
      if (i == 0) {
        variableMap.put("extraName", "test2");
      }
      runtimeService.startProcessInstanceByKey("simple", variableMap);
    }
    
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("oneTaskProcess");
    }
    
    String simpleId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("simple").singleResult().getId();
    
    HttpGet request = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(
        EnterpriseRestUrls.URL_DISCO_LOG, simpleId) + "?scope=traces");
    request.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/xml"));
    CloseableHttpResponse response = executeBinaryRequest(request, HttpStatus.OK.value());
    
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(response.getEntity().getContent());
    closeResponse(response);
    
    Element log = doc.getDocumentElement();
    assertEquals("log", log.getNodeName());
    assertEquals("Simple Process", log.getAttribute("name"));
    assertEquals("5", log.getAttribute("traces"));
    assertEquals("15", log.getAttribute("events"));
    Element description = (Element) log.getElementsByTagName("description").item(0);
    assertEquals("Simple Process", description.getTextContent());
    
    Element globalsElement = (Element) log.getElementsByTagName("globals").item(0);
    NodeList traceAttributeList = globalsElement.getElementsByTagName("traceAttribute");
    Map<String, String> traceAttributeMap = new HashMap<String, String>();
    for (int i = 0; i < traceAttributeList.getLength(); i++) {
      Element traceElement = (Element) traceAttributeList.item(i);
      traceAttributeMap.put(traceElement.getAttribute("key"), traceElement.getAttribute("type"));
    }
    
    assertEquals("string", traceAttributeMap.get("name"));
    assertEquals("string", traceAttributeMap.get("extraName"));
    assertEquals("integer", traceAttributeMap.get("number"));
    
    NodeList traceList = log.getElementsByTagName("trace");
    Map<String, Element> traceMap = new HashMap<String, Element>();
    for (int i = 0; i < traceList.getLength(); i++) {
      Element traceElement = (Element) traceList.item(i);
      traceMap.put(traceElement.getAttribute("id"), traceElement);
    }
    
    List<HistoricProcessInstance> instanceList = historyService.createHistoricProcessInstanceQuery().processDefinitionId(simpleId).list();
    assertEquals(5, instanceList.size());
    for (HistoricProcessInstance historicProcessInstance : instanceList) {
      Element traceElement = traceMap.get(historicProcessInstance.getId());
      assertNotNull(traceElement);
      assertEquals(historicProcessInstance.getId(), traceElement.getAttribute("id"));
      assertEquals("" + historyService.createHistoricActivityInstanceQuery().processInstanceId(historicProcessInstance.getId()).count(), traceElement.getAttribute("size"));
      assertNotNull(traceElement.getAttribute("start"));
      assertNotNull(traceElement.getAttribute("end"));
      if (historicProcessInstance.getEndTime() != null) {
        assertTrue(Boolean.valueOf(traceElement.getAttribute("completed")));
      } else {
        assertFalse(Boolean.valueOf(traceElement.getAttribute("completed")));
      }
      
      NodeList attributeList = traceElement.getElementsByTagName("attribute");
      assertEquals(0, attributeList.getLength());
      
      NodeList eventList = traceElement.getElementsByTagName("event");
      assertEquals(0, eventList.getLength());
    }
    
    repositoryService.deleteDeployment(deployment.getId(), true);
  }
  
  /**
   * Test getting the log with scope all.
   * GET disco/log/{processDefinitionId}.dxml?scope=all
   */
  public void testGetLogAll() throws Exception {
    Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource("com/activiti/rest/api/disco/oneTaskProcess.bpmn20.xml")
        .addClasspathResource("com/activiti/rest/api/disco/anotherOneTaskProcess.bpmn20.xml")
        .addClasspathResource("com/activiti/rest/api/disco/simpleProcess.bpmn20.xml")
        .deploy();
    
    Map<String, Map<String, Object>> startVariableMap = new HashMap<String, Map<String, Object>>();
    for (int i = 0; i < 5; i++) {
      Map<String, Object> variableMap = new HashMap<String, Object>();
      variableMap.put("name", "test" + i);
      variableMap.put("number", 123 + i);
      if (i == 0) {
        variableMap.put("extraName", "test2" + i);
      }
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simple", variableMap);
      startVariableMap.put(processInstance.getId(),variableMap);
    }
    
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("oneTaskProcess");
    }
    
    String simpleId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("simple").singleResult().getId();
    
    HttpGet request = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(
        EnterpriseRestUrls.URL_DISCO_LOG, simpleId) + "?scope=all");
    request.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/xml"));
    CloseableHttpResponse response = executeBinaryRequest(request, HttpStatus.OK.value());
    
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(response.getEntity().getContent());
    closeResponse(response);
    
    Element log = doc.getDocumentElement();
    assertEquals("log", log.getNodeName());
    assertEquals("Simple Process", log.getAttribute("name"));
    assertEquals("5", log.getAttribute("traces"));
    assertEquals("15", log.getAttribute("events"));
    Element description = (Element) log.getElementsByTagName("description").item(0);
    assertEquals("Simple Process", description.getTextContent());
    
    Element globalsElement = (Element) log.getElementsByTagName("globals").item(0);
    NodeList traceAttributeList = globalsElement.getElementsByTagName("traceAttribute");
    Map<String, String> traceAttributeMap = new HashMap<String, String>();
    for (int i = 0; i < traceAttributeList.getLength(); i++) {
      Element attributeElement = (Element) traceAttributeList.item(i);
      traceAttributeMap.put(attributeElement.getAttribute("key"), attributeElement.getAttribute("type"));
    }
    
    assertEquals("string", traceAttributeMap.get("name"));
    assertEquals("string", traceAttributeMap.get("extraName"));
    assertEquals("integer", traceAttributeMap.get("number"));
    
    NodeList eventAttributeList = globalsElement.getElementsByTagName("eventAttribute");
    Map<String, Element> eventAttributeMap = new HashMap<String, Element>();
    for (int i = 0; i < eventAttributeList.getLength(); i++) {
      Element attributeElement = (Element) eventAttributeList.item(i);
      eventAttributeMap.put(attributeElement.getAttribute("key"), attributeElement);
    }
    
    Element eventAttributeElement = eventAttributeMap.get("activityId");
    assertEquals("string", eventAttributeElement.getAttribute("type"));
    assertEquals("", eventAttributeElement.getAttribute("classifier"));
    eventAttributeElement = eventAttributeMap.get("activityName");
    assertEquals("string", eventAttributeElement.getAttribute("type"));
    assertEquals("activity", eventAttributeElement.getAttribute("classifier"));
    eventAttributeElement = eventAttributeMap.get("assignee");
    assertEquals("string", eventAttributeElement.getAttribute("type"));
    assertEquals("resource", eventAttributeElement.getAttribute("classifier"));
    eventAttributeElement = eventAttributeMap.get("activityType");
    assertEquals("string", eventAttributeElement.getAttribute("type"));
    assertEquals("", eventAttributeElement.getAttribute("classifier"));
    
    NodeList traceList = log.getElementsByTagName("trace");
    Map<String, Element> traceMap = new HashMap<String, Element>();
    for (int i = 0; i < traceList.getLength(); i++) {
      Element traceElement = (Element) traceList.item(i);
      traceMap.put(traceElement.getAttribute("id"), traceElement);
    }
    
    List<HistoricProcessInstance> instanceList = historyService.createHistoricProcessInstanceQuery().processDefinitionId(simpleId).list();
    assertEquals(5, instanceList.size());
    for (int i = 0; i < instanceList.size(); i++) {
      HistoricProcessInstance historicProcessInstance = instanceList.get(i);
      Element traceElement = traceMap.get(historicProcessInstance.getId());
      assertNotNull(traceElement);
      assertEquals(historicProcessInstance.getId(), traceElement.getAttribute("id"));
      List<HistoricActivityInstance> activityInstanceList = historyService.createHistoricActivityInstanceQuery().processInstanceId(historicProcessInstance.getId()).list();
      assertEquals("" + activityInstanceList.size(), traceElement.getAttribute("size"));
      assertNotNull(traceElement.getAttribute("start"));
      assertNotNull(traceElement.getAttribute("end"));
      if (historicProcessInstance.getEndTime() != null) {
        assertTrue(Boolean.valueOf(traceElement.getAttribute("completed")));
      } else {
        assertFalse(Boolean.valueOf(traceElement.getAttribute("completed")));
      }
      
      NodeList childList = traceElement.getChildNodes();
      List<Element> attributeList = new ArrayList<Element>();
      for (int j = 0; j < childList.getLength(); j++) {
        if ("attribute".equals(childList.item(j).getNodeName())) {
          attributeList.add((Element) childList.item(j));
        }
      }
      
      assertEquals(3, attributeList.size());
      Map<String, String> attributeMap = new HashMap<String, String>();
      for (int j = 0; j < attributeList.size(); j++) {
        Element attributeElement = attributeList.get(j);
        attributeMap.put(attributeElement.getAttribute("key"), attributeElement.getAttribute("value"));
      }
      
      Map<String, Object> startVariableInstanceMap = startVariableMap.get(historicProcessInstance.getId());
      assertEquals(startVariableInstanceMap.get("name"), attributeMap.get("name"));
      String extraNameValue = "";
      if (startVariableInstanceMap.get("extraName") != null) {
        extraNameValue = (String) startVariableInstanceMap.get("extraName");
      }
      assertEquals(extraNameValue, attributeMap.get("extraName"));
      assertEquals(String.valueOf(startVariableInstanceMap.get("number")), attributeMap.get("number"));
      
      NodeList eventList = traceElement.getElementsByTagName("event");
      assertEquals(3, eventList.getLength());
      
      Map<String, String[]> instanceActivityMap = new HashMap<String, String[]>();
      for (int j = 0; j < eventList.getLength(); j++) {
        String[] attributeArray = new String[4];
        Element eventElement = (Element) eventList.item(j);
        NodeList eventInstanceAttributeList = eventElement.getElementsByTagName("attribute");
        for (int k = 0; k < eventInstanceAttributeList.getLength(); k++) {
          Element attributeElement = (Element) eventInstanceAttributeList.item(k);
          if ("activityId".equals(attributeElement.getAttribute("key"))) {
            attributeArray[0] = attributeElement.getAttribute("value");
          
          } else if ("activityName".equals(attributeElement.getAttribute("key"))) {
            attributeArray[1] = attributeElement.getAttribute("value");
          
          } else if ("assignee".equals(attributeElement.getAttribute("key"))) {
            attributeArray[2] = attributeElement.getAttribute("value");
          
          } else if ("activityType".equals(attributeElement.getAttribute("key"))) {
            attributeArray[3] = attributeElement.getAttribute("value");
          }
        }
        instanceActivityMap.put(attributeArray[0], attributeArray);
      }
      
      for (HistoricActivityInstance activity : activityInstanceList) {
        String[] attributeArray = instanceActivityMap.get(activity.getActivityId());
        assertEquals(activity.getActivityId(), attributeArray[0]);
        String activityName = "";
        if (activity.getActivityName() != null) {
          activityName = activity.getActivityName();
        }
        assertEquals(activityName, attributeArray[1]);
        String assignee = "";
        if (activity.getAssignee() != null) {
          assignee = activity.getAssignee();
        }
        assertEquals(assignee, attributeArray[2]);
        assertEquals(activity.getActivityType(), attributeArray[3]);
      }
      
    }
    
    repositoryService.deleteDeployment(deployment.getId(), true);
  }
  
  public void testGetLogUnauthenticated() throws Exception {
    Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource("com/activiti/rest/api/disco/oneTaskProcess.bpmn20.xml")
        .addClasspathResource("com/activiti/rest/api/disco/simpleProcess.bpmn20.xml")
        .deploy();
    
    String simpleId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("simple").singleResult().getId();
    
    HttpClient client = HttpClientBuilder.create().build();
    HttpResponse response = client.execute(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(EnterpriseRestUrls.URL_DISCO_LOG, simpleId)));
    assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusLine().getStatusCode());
    
    repositoryService.deleteDeployment(deployment.getId(), true);
  }
}
