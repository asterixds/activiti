package com.activiti.addon.cluster.lifecycle;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineLifecycleListener;
import org.activiti.engine.runtime.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.activiti.addon.cluster.constants.EventTypes;

/**
 * @author jbarrez
 */
public class ClusterEnabledProcessEngineLifeCycleListener implements ProcessEngineLifecycleListener {

	protected Logger logger = LoggerFactory.getLogger(ClusterEnabledProcessEngineLifeCycleListener.class);
	
	protected Clock clock;
	protected ProcessEngineLifecycleListener wrappedListener;
	
	protected String currentState;
	protected Date engineCreateDate;
	protected Date engineReadyDate;
	protected Date engineClosedDate;

	public ClusterEnabledProcessEngineLifeCycleListener(Clock clock,
			ProcessEngineLifecycleListener wrappedListener) {
		this.clock = clock;
		this.wrappedListener = wrappedListener;
		this.currentState = EventTypes.BOOTING;
		this.engineCreateDate = clock.getCurrentTime();
	}

	public void onProcessEngineBuilt(ProcessEngine processEngine) {

		changeLifeCycleState(EventTypes.PROCESS_ENGINE_READY);
		engineReadyDate = clock.getCurrentTime();

		if (wrappedListener != null) {
			wrappedListener.onProcessEngineBuilt(processEngine);
		}
	}

	public void onProcessEngineClosed(ProcessEngine processEngine) {

		changeLifeCycleState(EventTypes.PROCESS_ENGINE_CLOSED);
		engineClosedDate = clock.getCurrentTime();
		
		if (wrappedListener != null) {
			wrappedListener.onProcessEngineClosed(processEngine);
		}
	}
	
	protected void changeLifeCycleState(String state) {
		currentState = state;
	}
	
	public Map<String, Object> getCurrentlLifeCycleStateEvent() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("type", "process-engine-lifecycle");
		map.put("state", currentState);
		
		if (engineCreateDate != null) {
			map.put("engineCreateDate", engineCreateDate);
		}
		
		if (engineReadyDate != null) {
			map.put("engineReadyDate", engineReadyDate);
		}
		
		if (engineClosedDate != null) {
			map.put("engineClosedDate", engineClosedDate);
		}
		
		return map;
	}
	
}
