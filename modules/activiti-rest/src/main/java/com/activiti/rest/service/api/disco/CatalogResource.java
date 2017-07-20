package com.activiti.rest.service.api.disco;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.activiti.rest.service.api.ServerProperties;

@RestController
public class CatalogResource extends AbstractDiscoResource{

  private static final Logger log = Logger.getLogger(CatalogResource.class);
  
  @Autowired
  protected RepositoryService repositoryService;
  
  @Autowired
  protected HistoryService historyService;
  
  @RequestMapping(value="/disco/catalog.xml", method = RequestMethod.GET, produces="application/xml")
  public @ResponseBody String getCatalog() {
	  try {
	    
	    List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
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
	    
	    return transformDocumentToString(doc);
	    
	  } catch (ActivitiException e) {
	    log.error("Error building catalog", e);
	    throw e;
	    
	  } catch (Exception e) {
	    log.error("Error building catalog", e);
	    throw new ActivitiException("Error building catalog", e);
	  }
  }
	
	protected void addLogs(List<ProcessDefinition> definitions, Element catalog, Document doc) {
	  for (ProcessDefinition processDefinition : definitions) {
	    long instanceCount = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processDefinition.getId()).count();
	    List<HistoricActivityInstance> activityList = historyService.createHistoricActivityInstanceQuery().processDefinitionId(processDefinition.getId()).orderByHistoricActivityInstanceStartTime().asc().list();
	    
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
