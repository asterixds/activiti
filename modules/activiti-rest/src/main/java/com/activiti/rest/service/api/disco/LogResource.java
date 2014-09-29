package com.activiti.rest.service.api.disco;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableInstance;
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

public class LogResource extends SecuredResource {

  private static final Logger log = Logger.getLogger(LogResource.class);
  
  private static final String SCOPE_SUMMARY = "summary";
  private static final String SCOPE_TRACES = "traces";
  private static final String SCOPE_ALL = "all";
  
	@Get
  public DomRepresentation getLog() {
	  if (authenticate() == false)
	    throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
	  
    ProcessDefinition processDefinition = getProcessDefinition();
    String scope = getQuery().getFirstValue("scope", SCOPE_ALL);
    
    if (scope.equals(SCOPE_SUMMARY) == false && scope.equals(SCOPE_TRACES) == false && scope.equals(SCOPE_ALL) == false) {
      throw new ActivitiIllegalArgumentException("scope parameter value '" + scope + "' is not valid, only all, traces and summary are accepted");
    }
    
    List<HistoricProcessInstance> instanceList = ActivitiUtil.getHistoryService()
        .createHistoricProcessInstanceQuery()
        .processDefinitionId(processDefinition.getId())
        .orderByProcessInstanceStartTime()
        .asc()
        .list();
    
    List<HistoricActivityInstance> activityList = ActivitiUtil.getHistoryService()
        .createHistoricActivityInstanceQuery()
        .processDefinitionId(processDefinition.getId())
        .orderByHistoricActivityInstanceStartTime()
        .asc()
        .list();
    
	  try {
	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
	    // root elements
	    Document doc = docBuilder.newDocument();
	    Element rootElement = doc.createElement("log");
	    doc.appendChild(rootElement);
	    rootElement.setAttribute("name", getDefinitionName(processDefinition));
	    rootElement.setAttribute("href", ServerProperties.getServerUrl() + "service/disco/log/" + processDefinition.getId());
	    rootElement.setAttribute("scope", scope);
	    rootElement.setAttribute("traces", String.valueOf(instanceList.size()));
	    rootElement.setAttribute("events", String.valueOf(activityList.size()));
	    if (activityList.size() > 0) {
	      rootElement.setAttribute("start", String.valueOf(activityList.get(0).getStartTime().getTime()));
        Long end = null;
        HistoricActivityInstance activity = activityList.get(activityList.size() - 1);
        if (activity.getEndTime() != null) {
          end = activity.getEndTime().getTime();
        } else {
          end = activity.getStartTime().getTime();
        }
        rootElement.setAttribute("end", String.valueOf(end));
      }
      TimeZone tz = Calendar.getInstance().getTimeZone();
      rootElement.setAttribute("timezone", tz.getID());
      rootElement.setAttribute("serial", String.valueOf(new Date().getTime()));
      rootElement.setAttribute("catalog", ServerProperties.getServerUrl() + "service/disco/catalog.xml");
      rootElement.setAttribute("manifest", ServerProperties.getServerUrl() + "service/disco/manifest.xml");
	    
      Element descriptionElement = doc.createElement("description");
      rootElement.appendChild(descriptionElement);
      descriptionElement.setTextContent(getDefinitionDescription(processDefinition));
      
      Element globalsElement = doc.createElement("globals");
      rootElement.appendChild(globalsElement);
      
      if (instanceList.size() > 0) {
        Map<String, HistoricVariableInstance> variableMap = new HashMap<String, HistoricVariableInstance>();
        List<String> instanceIds = findCompletedInstances(instanceList);
        if (instanceIds.size() == 1) {
          addVariables(instanceIds.get(0), variableMap);
        } else {
          addVariables(instanceIds.get(0), variableMap);
          addVariables(instanceIds.get(instanceIds.size() - 1), variableMap);
        }
        
        Iterator<String> itVariable = variableMap.keySet().iterator();
        while (itVariable.hasNext()) {
          HistoricVariableInstance variable = variableMap.get(itVariable.next());
          createTraceAttributeElement(variable.getVariableName(), variable.getVariableTypeName(), globalsElement, doc);
        }
        
        createEventAttributeElement("activityId", "string", null, globalsElement, doc);
        createEventAttributeElement("activityName", "string", "activity", globalsElement, doc);
        createEventAttributeElement("assignee", "string", "resource", globalsElement, doc);
        createEventAttributeElement("activityType", "string", null, globalsElement, doc);
        
        if (scope.equals(SCOPE_TRACES) || scope.equals(SCOPE_ALL)) {
          Map<String, List<HistoricActivityInstance>> activityMap = sortActivityInstances(activityList);
          Map<String, List<HistoricVariableInstance>> instanceVariableMap = null;
          
          if (scope.equals(SCOPE_ALL)) {
            List<HistoricVariableInstance> variableList = ActivitiUtil.getHistoryService()
                .createHistoricVariableInstanceQuery()
                .list();
            instanceVariableMap = sortVariableInstances(variableList);
          }
          
          for (HistoricProcessInstance historicProcessInstance : instanceList) {
            Element traceElement = doc.createElement("trace");
            rootElement.appendChild(traceElement);
            traceElement.setAttribute("id", historicProcessInstance.getId());
            int size = 0;
            if (activityMap.containsKey(historicProcessInstance.getId())) {
              size = activityMap.get(historicProcessInstance.getId()).size();
            }
            traceElement.setAttribute("size", String.valueOf(size));
            long start = historicProcessInstance.getStartTime().getTime();
            long end;
            boolean completed = false;
            if (historicProcessInstance.getEndTime() != null) {
              completed = true;
              end = historicProcessInstance.getEndTime().getTime();
            } else {
              end = getActivityListLatestTime(historicProcessInstance, activityMap.get(historicProcessInstance.getId()));
            }
            traceElement.setAttribute("start", String.valueOf(start));
            traceElement.setAttribute("end", String.valueOf(end));
            traceElement.setAttribute("completed", String.valueOf(completed));
            
            if (scope.equals(SCOPE_ALL)) {
              List<HistoricVariableInstance> instanceVariableList = instanceVariableMap.get(historicProcessInstance.getId());
              itVariable = variableMap.keySet().iterator();
              while (itVariable.hasNext()) {
                String varName = itVariable.next();
                HistoricVariableInstance instanceVariable = getVariableByName(varName, instanceVariableList);
                String value = "";
                if (instanceVariable != null && instanceVariable.getValue() != null) {
                  value = instanceVariable.getValue().toString();
                }
                createAttributeElement(varName, value, traceElement, doc);
              }
              
              List<HistoricActivityInstance> instanceActivityList = activityMap.get(historicProcessInstance.getId());
              if (instanceActivityList != null) {
                for (HistoricActivityInstance historicActivityInstance : instanceActivityList) {
                  Element eventElement = doc.createElement("event");
                  traceElement.appendChild(eventElement);
                  eventElement.setAttribute("start", String.valueOf(historicActivityInstance.getStartTime().getTime()));
                  if (historicActivityInstance.getEndTime() != null) {
                    eventElement.setAttribute("end", String.valueOf(historicActivityInstance.getEndTime().getTime()));
                  }
                  createAttributeElement("activityId", historicActivityInstance.getActivityId(), eventElement, doc);
                  String name = "";
                  if (historicActivityInstance.getActivityName() != null) {
                    name = historicActivityInstance.getActivityName();
                  }
                  createAttributeElement("activityName", name, eventElement, doc);
                  String assignee = "";
                  if (historicActivityInstance.getAssignee() != null) {
                    assignee = historicActivityInstance.getAssignee();
                  }
                  createAttributeElement("assignee", assignee, eventElement, doc);
                  createAttributeElement("activityType", historicActivityInstance.getActivityType(), eventElement, doc);
                }
              }
            }
          }
        }
      }
	    
	    DomRepresentation result = new DomRepresentation(MediaType.APPLICATION_XML, doc);
	    return result;
	    
	  } catch (Exception e) {
	    log.error("Error building log", e);
	    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
	  }
  }
	
	protected void createTraceAttributeElement(String key, String type, Element globalsElement, Document doc) {
	  Element attributeElement = doc.createElement("traceAttribute");
	  globalsElement.appendChild(attributeElement);
	  attributeElement.setAttribute("key", key);
	  attributeElement.setAttribute("type", type);
	}
	
	protected void createEventAttributeElement(String key, String type, String classifier, Element globalsElement, Document doc) {
    Element attributeElement = doc.createElement("eventAttribute");
    globalsElement.appendChild(attributeElement);
    attributeElement.setAttribute("key", key);
    attributeElement.setAttribute("type", type);
    if (classifier != null) {
      attributeElement.setAttribute("classifier", classifier);
    }
	}
	
	protected void createAttributeElement(String key, String value, Element traceElement, Document doc) {
    Element attributeElement = doc.createElement("attribute");
    traceElement.appendChild(attributeElement);
    attributeElement.setAttribute("key", key);
    attributeElement.setAttribute("value", value);
  }
	
	protected List<String> findCompletedInstances(List<HistoricProcessInstance> instanceList) {
	  List<String> completedInstanceIds = new ArrayList<String>();
	  List<String> runningInstanceIds = new ArrayList<String>();
	  for (HistoricProcessInstance historicProcessInstance : instanceList) {
      if (historicProcessInstance.getEndTime() != null) {
        completedInstanceIds.add(historicProcessInstance.getId());
      } else {
        runningInstanceIds.add(historicProcessInstance.getId());
      }
    }
	  
	  if (completedInstanceIds.size() > 0) {
	    return completedInstanceIds;
	  } else {
	    return runningInstanceIds;
	  }
	}
	
	protected void addVariables(String instanceId, Map<String, HistoricVariableInstance> variableMap) {
	  List<HistoricVariableInstance> variables = ActivitiUtil.getHistoryService().createHistoricVariableInstanceQuery()
	      .processInstanceId(instanceId)
	      .list();
	  
	  for (HistoricVariableInstance historicVariableInstance : variables) {
      if (variableMap.containsKey(historicVariableInstance.getVariableName()) == false) {
        variableMap.put(historicVariableInstance.getVariableName(), historicVariableInstance);
      }
    }
	}
	
	protected Map<String, List<HistoricActivityInstance>> sortActivityInstances(List<HistoricActivityInstance> fullActivityList) {
	  Map<String, List<HistoricActivityInstance>> activityMap = new HashMap<String, List<HistoricActivityInstance>>();
	  for (HistoricActivityInstance historicActivityInstance : fullActivityList) {
      if (activityMap.containsKey(historicActivityInstance.getProcessInstanceId())) {
        activityMap.get(historicActivityInstance.getProcessInstanceId()).add(historicActivityInstance);
      } else {
        List<HistoricActivityInstance> activityList = new ArrayList<HistoricActivityInstance>();
        activityList.add(historicActivityInstance);
        activityMap.put(historicActivityInstance.getProcessInstanceId(), activityList);
      }
    }
	  return activityMap;
	}
	
	protected Map<String, List<HistoricVariableInstance>> sortVariableInstances(List<HistoricVariableInstance> fullVariableList) {
    Map<String, List<HistoricVariableInstance>> variableMap = new HashMap<String, List<HistoricVariableInstance>>();
    for (HistoricVariableInstance historicVariableInstance : fullVariableList) {
      if (variableMap.containsKey(historicVariableInstance.getProcessInstanceId())) {
        variableMap.get(historicVariableInstance.getProcessInstanceId()).add(historicVariableInstance);
      } else {
        List<HistoricVariableInstance> variableList = new ArrayList<HistoricVariableInstance>();
        variableList.add(historicVariableInstance);
        variableMap.put(historicVariableInstance.getProcessInstanceId(), variableList);
      }
    }
    return variableMap;
  }
	
	protected HistoricVariableInstance getVariableByName(String variableName, List<HistoricVariableInstance> variableList) {
	  HistoricVariableInstance result = null;
	  if (variableList != null) {
	    for (HistoricVariableInstance historicVariableInstance : variableList) {
        if (variableName.equals(historicVariableInstance.getVariableName())) {
          result = historicVariableInstance;
          break;
        }
      }
	  }
	  return result;
	}
	
	protected long getActivityListLatestTime(HistoricProcessInstance processInstance, List<HistoricActivityInstance> activityList) {
	  if (activityList != null && activityList.size() > 0) {
	    Date latestTime = null;
	    for (HistoricActivityInstance activity : activityList) {
	      Date activityTime = getActivityLatestTime(activity);
	      if (latestTime == null || latestTime.before(activityTime)) {
	        latestTime = activityTime;
	      }
	    }
	    return latestTime.getTime();
	  } else {
	    return processInstance.getStartTime().getTime();
	  }
	}
	
	protected Date getActivityLatestTime(HistoricActivityInstance activity) {
	  if (activity.getEndTime() != null) {
	    return activity.getEndTime();
	  } else {
	    return activity.getStartTime();
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
	
	protected ProcessDefinition getProcessDefinition() {
	  String processDefinitionId = getAttribute("processDefinitionId");
    if (processDefinitionId == null) {
      throw new ActivitiIllegalArgumentException("The processDefinitionId cannot be null");
    }
    
    ProcessDefinition processDefinition = ActivitiUtil.getRepositoryService().createProcessDefinitionQuery()
        .processDefinitionId(processDefinitionId).singleResult();

    if (processDefinition == null) {
      throw new ActivitiObjectNotFoundException("Could not find a process definition with id '" + processDefinitionId + "'.", ProcessDefinition.class);
    }
    
    return processDefinition;
	}
}
