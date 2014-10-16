package com.activiti.rest.service.api.disco;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.activiti.engine.ActivitiException;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.activiti.rest.service.api.ServerProperties;

@RestController
public class ManifestResource extends AbstractDiscoResource {

  private static final Logger log = Logger.getLogger(ManifestResource.class);
  
  @RequestMapping(value="/disco/manifest.xml", method = RequestMethod.GET, produces="application/xml")
  public @ResponseBody String getManifest() {
	  try {
	    
	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
	    // root elements
	    Document doc = docBuilder.newDocument();
	    Element rootElement = doc.createElement("manifest");
	    doc.appendChild(rootElement);
	    
	    // vendor element
	    Element vendor = doc.createElement("vendor");
	    rootElement.appendChild(vendor);
	    vendor.setAttribute("name", "Alfresco Activiti");
	    vendor.setAttribute("href", "http://www.alfresco.com/products/activiti");
	    vendor.setAttribute("icon", "http://www.alfresco.com/sites/www/themes/alfrescodotcom/img/logo.png");
	    
	    // catalog element
      Element catalog = doc.createElement("catalog");
      rootElement.appendChild(catalog);
      catalog.setAttribute("name", "Activiti processes");
      catalog.setAttribute("href", ServerProperties.getServerUrl() + "service/disco/catalog.xml");
      catalog.setAttribute("auth", "basic");
      
      Element catalogDescription = doc.createElement("description");
      catalog.appendChild(catalogDescription);
      catalogDescription.setTextContent("Event log of Alfresco Activiti");
      
      Element format = doc.createElement("format");
      catalog.appendChild(format);
      format.setAttribute("type", "DXML");
      
      addFilter("timeframe", catalog, doc);
      addFilter("completed", catalog, doc);
      addFilter("pagination", catalog, doc);
	    
	    return transformDocumentToString(doc);
	    
	  } catch (ActivitiException e) {
      log.error("Error building manifest", e);
      throw e;
      
    } catch (Exception e) {
      log.error("Error building manifest", e);
      throw new ActivitiException("Error building manifest", e);
    }
  }
	
	protected void addFilter(String type, Element catalog, Document doc) {
	  Element filter = doc.createElement("filter");
    catalog.appendChild(filter);
    filter.setAttribute("type", type);
	}
}
