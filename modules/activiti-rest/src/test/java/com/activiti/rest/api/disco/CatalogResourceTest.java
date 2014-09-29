package com.activiti.rest.api.disco;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.activiti.engine.repository.Deployment;
import org.activiti.rest.common.api.ActivitiUtil;
import org.activiti.rest.service.BaseRestTestCase;
import org.activiti.rest.service.api.RestUrls;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.activiti.rest.service.api.EnterpriseRestUrls;


/**
 * Test for all REST-operations related to a Disco catalog resource.
 * 
 * @author Tijs Rademakers
 */
public class CatalogResourceTest extends BaseRestTestCase {
  
  /**
   * Test getting the catalog.
   * GET disco/catalog
   */
  public void testGetCatalog() throws Exception {
    Deployment deployment = ActivitiUtil.getRepositoryService().createDeployment()
        .addClasspathResource("com/activiti/rest/api/disco/oneTaskProcess.bpmn20.xml")
        .addClasspathResource("com/activiti/rest/api/disco/simpleProcess.bpmn20.xml")
        .deploy();
    
    for (int i = 0; i < 5; i++) {
      ActivitiUtil.getRuntimeService().startProcessInstanceByKey("simple");
    }
    
    for (int i = 0; i < 3; i++) {
      ActivitiUtil.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");
    }
    
    ClientResource client = getAuthenticatedClient(RestUrls.createRelativeResourceUrl(EnterpriseRestUrls.URL_DISCO_CATALOG));
    Representation response = client.get();
    assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
    
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(response.getStream());
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
    
    String simpleId = ActivitiUtil.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("simple").singleResult().getId();
    String oneTaskProcessId = ActivitiUtil.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult().getId();
    
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
    
    ActivitiUtil.getRepositoryService().deleteDeployment(deployment.getId(), true);
  }
  
  public void testGetCatalogUnauthenticated() throws Exception {
    
    ClientResource client = new ClientResource("http://localhost:8182/" + RestUrls.createRelativeResourceUrl(EnterpriseRestUrls.URL_DISCO_CATALOG));
    try {
      client.get();
      fail("Expected authentication exception");
    } catch (Exception e) {
      assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED.getCode(), client.getStatus().getCode());
    }
  }
}
