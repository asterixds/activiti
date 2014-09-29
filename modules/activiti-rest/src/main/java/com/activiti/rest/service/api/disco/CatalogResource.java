package com.activiti.rest.service.api.disco;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.rest.common.api.ActivitiUtil;
import org.activiti.rest.common.api.SecuredResource;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.activiti.rest.service.api.ServerProperties;

public class CatalogResource extends SecuredResource {

  private static final Logger log = Logger.getLogger(CatalogResource.class);
  
	@Get
  public DomRepresentation getCatalog() {
	  if (authenticate() == false)
	    throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
	  
	  try {
	    
	    List<ProcessDefinition> definitions = ActivitiUtil.getRepositoryService()
	        .createProcessDefinitionQuery()
	        .latestVersion()
	        .list();
	    
	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
	    // root elements
	    Document doc = docBuilder.newDocument();
	    Element rootElement = doc.createElement("catalog");
	    doc.appendChild(rootElement);
	    rootElement.setAttribute("size", String.valueOf(definitions.size()));
	    rootElement.setAttribute("time", String.valueOf(new Date().getTime()));
	    rootElement.setAttribute("href", ServerProperties.getServerUrl() + "service/disco/manifest.xml");
	    
	    addLogs(definitions, rootElement, doc);
	    
	    DomRepresentation result = new DomRepresentation(MediaType.APPLICATION_XML, doc);
	    return result;
	    
	  } catch (Exception e) {
	    log.error("Error building catalog", e);
	    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
	  }
  }
	
	protected void addLogs(List<ProcessDefinition> definitions, Element catalog, Document doc) {
	  for (ProcessDefinition processDefinition : definitions) {
	    long instanceCount = ActivitiUtil.getHistoryService().createHistoricProcessInstanceQuery().processDefinitionId(processDefinition.getId()).count();
	    List<HistoricActivityInstance> activityList = ActivitiUtil.getHistoryService().createHistoricActivityInstanceQuery().processDefinitionId(processDefinition.getId()).orderByHistoricActivityInstanceStartTime().asc().list();
	    
      String name = getDefinitionName(processDefinition);
      String description = getDefinitionDescription(processDefinition);
      
      Element log = doc.createElement("log");
      catalog.appendChild(log);
      log.setAttribute("id", processDefinition.getId());
      log.setAttribute("name", name);
      log.setAttribute("href", ServerProperties.getServerUrl() + "service/disco/log/" + processDefinition.getId());
      
      Element descriptionElement = doc.createElement("description");
      log.appendChild(descriptionElement);
      descriptionElement.setTextContent(description);
      
      Element sizeElement = doc.createElement("size");
      log.appendChild(sizeElement);
      sizeElement.setAttribute("traces", String.valueOf(instanceCount));
      sizeElement.setAttribute("events", String.valueOf(activityList.size()));
      sizeElement.setAttribute("serial", String.valueOf(new Date().getTime()));
      
      Element timeElement = doc.createElement("time");
      log.appendChild(timeElement);
      if (activityList.size() > 0) {
        timeElement.setAttribute("start", String.valueOf(activityList.get(0).getStartTime().getTime()));
        Long end = null;
        HistoricActivityInstance activity = activityList.get(activityList.size() - 1);
        if (activity.getEndTime() != null) {
          end = activity.getEndTime().getTime();
        } else {
          end = activity.getStartTime().getTime();
        }
        timeElement.setAttribute("end", String.valueOf(end));
      }
      TimeZone tz = Calendar.getInstance().getTimeZone();
      timeElement.setAttribute("timezone", tz.getID());
    }
	}
	
	protected String getDefinitionName(ProcessDefinition definition) {
	  String name = null;
    if (StringUtils.isNotEmpty(definition.getName())) {
      name = definition.getName();
    
    } else {
      name = definition.getKey();
    }
    return name;
	}
	
	protected String getDefinitionDescription(ProcessDefinition definition) {
	  String description = null;
    if (StringUtils.isNotEmpty(definition.getDescription())) {
      description = definition.getDescription();
      
    } else if (StringUtils.isNotEmpty(definition.getName())) {
      description = definition.getName();
    
    } else {
      description = definition.getKey();
    }
    return description;
  }
}
