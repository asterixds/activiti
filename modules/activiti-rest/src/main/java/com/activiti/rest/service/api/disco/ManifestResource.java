package com.activiti.rest.service.api.disco;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.activiti.rest.service.api.ServerProperties;

public class ManifestResource extends ServerResource {

  private static final Logger log = Logger.getLogger(ManifestResource.class);
  
	@Get
  public DomRepresentation getManifest() {
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
	    
	    DomRepresentation result = new DomRepresentation(MediaType.APPLICATION_XML, doc);
	    return result;
	    
	  } catch (Exception e) {
	    log.error("Error building manifest", e);
	    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
	  }
  }
	
	protected void addFilter(String type, Element catalog, Document doc) {
	  Element filter = doc.createElement("filter");
    catalog.appendChild(filter);
    filter.setAttribute("type", type);
	}
}
