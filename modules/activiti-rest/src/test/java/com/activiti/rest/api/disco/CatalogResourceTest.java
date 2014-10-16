package com.activiti.rest.api.disco;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.activiti.engine.repository.Deployment;
import org.activiti.rest.service.BaseSpringRestTestCase;
import org.activiti.rest.service.api.RestUrls;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
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
public class CatalogResourceTest extends BaseSpringRestTestCase {
  
  /**
   * Test getting the catalog.
   * GET disco/catalog
   */
  public void testGetCatalog() throws Exception {
    Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource("com/activiti/rest/api/disco/oneTaskProcess.bpmn20.xml")
        .addClasspathResource("com/activiti/rest/api/disco/simpleProcess.bpmn20.xml")
        .deploy();
    
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("simple");
    }
    
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("oneTaskProcess");
    }
    
    HttpResponse response = executeXMLHttpRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(
        EnterpriseRestUrls.URL_DISCO_CATALOG)), HttpStatus.OK.value());
    
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(response.getEntity().getContent());
    Element catalog = doc.getDocumentElement();
    assertEquals("catalog", catalog.getNodeName());
    assertEquals("2", catalog.getAttribute("size"));
    assertNotNull(catalog.getAttribute("time"));
    assertTrue(catalog.getAttribute("href").contains("/disco/manifest.xml"));
    
    NodeList logList = catalog.getElementsByTagName("log");
    assertEquals(2, logList.getLength());
    Map<String, Element> logMap = new HashMap<String, Element>();
    for (int i = 0; i < logList.getLength(); i++) {
      Element log = (Element) logList.item(i);
      logMap.put(log.getAttribute("id"), log);
    }
    
    String simpleId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("simple").singleResult().getId();
    String oneTaskProcessId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult().getId();
    
    assertTrue(logMap.containsKey(simpleId));
    Element log = logMap.get(simpleId);
    assertEquals("Simple Process", log.getAttribute("name"));
    assertTrue(log.getAttribute("href").contains(simpleId));
    Element description = (Element) log.getElementsByTagName("description").item(0);
    assertEquals("Simple Process", description.getTextContent());
    Element sizeElement = (Element) log.getElementsByTagName("size").item(0);
    assertEquals("5", sizeElement.getAttribute("traces"));
    assertEquals("15", sizeElement.getAttribute("events"));
    assertNotNull(sizeElement.getAttribute("serial"));
    Element timeElement = (Element) log.getElementsByTagName("time").item(0);
    assertNotNull(timeElement.getAttribute("start"));
    assertNotNull(timeElement.getAttribute("end"));
    assertNotNull(timeElement.getAttribute("timezone"));
    
    assertTrue(logMap.containsKey(oneTaskProcessId));
    log = logMap.get(oneTaskProcessId);
    assertEquals("The One Task Process", log.getAttribute("name"));
    description = (Element) log.getElementsByTagName("description").item(0);
    assertEquals("This is a process for testing purposes", description.getTextContent());
    sizeElement = (Element) log.getElementsByTagName("size").item(0);
    assertEquals("3", sizeElement.getAttribute("traces"));
    assertEquals("6", sizeElement.getAttribute("events"));
    assertNotNull(sizeElement.getAttribute("serial"));
    timeElement = (Element) log.getElementsByTagName("time").item(0);
    assertNotNull(timeElement.getAttribute("start"));
    assertNotNull(timeElement.getAttribute("end"));
    assertNotNull(timeElement.getAttribute("timezone"));
    
    repositoryService.deleteDeployment(deployment.getId(), true);
  }
  
  public void testGetCatalogUnauthenticated() throws Exception {
    HttpClient client = HttpClientBuilder.create().build();
    HttpResponse response = client.execute(new HttpGet("http://localhost:" + HTTP_SERVER_PORT + 
        "/service/" + RestUrls.createRelativeResourceUrl(EnterpriseRestUrls.URL_DISCO_CATALOG)));
    assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusLine().getStatusCode());
  }
}
