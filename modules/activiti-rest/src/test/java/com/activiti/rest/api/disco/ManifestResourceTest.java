package com.activiti.rest.api.disco;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
 * Test for all REST-operations related to a Disco manifest resource.
 * 
 * @author Tijs Rademakers
 */
public class ManifestResourceTest extends BaseRestTestCase {
  
  /**
   * Test getting the manifest.
   * GET disco/manifest
   */
  public void testGetManifest() throws Exception {
    
    ClientResource client = new ClientResource("http://localhost:8182/" + RestUrls.createRelativeResourceUrl(EnterpriseRestUrls.URL_DISCO_MANIFEST));
    Representation response = client.get();
    assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
    
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(response.getStream());
    Element manifest = doc.getDocumentElement();
    assertEquals("manifest", manifest.getNodeName());
    NodeList vendorList = manifest.getElementsByTagName("vendor");
    Element vendor = (Element) vendorList.item(0);
    assertEquals("vendor", vendor.getNodeName());
    assertEquals("Alfresco Activiti", vendor.getAttribute("name"));
    assertEquals("http://www.alfresco.com/products/activiti", vendor.getAttribute("href"));
    
    NodeList catalogList = manifest.getElementsByTagName("catalog");
    Element catalog = (Element) catalogList.item(0);
    assertEquals("catalog", catalog.getNodeName());
    assertEquals("Activiti processes", catalog.getAttribute("name"));
    assertTrue(catalog.getAttribute("href").contains("/disco/catalog.xml"));
    assertEquals("basic", catalog.getAttribute("auth"));
    
    NodeList filterList = manifest.getElementsByTagName("filter");
    List<String> filters = new ArrayList<String>();
    for (int i = 0; i < filterList.getLength(); i++) {
      Element filter = (Element) filterList.item(i);
      filters.add(filter.getAttribute("type"));
    }
    assertTrue(filters.contains("timeframe"));
    assertTrue(filters.contains("completed"));
    assertTrue(filters.contains("pagination"));
  }
}
