package com.activiti.addon.cluster.lifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.activiti.addon.cluster.constants.EventTypes;

/**
 * @author jbarrez
 */
public class ClusterEnabledProcessEngineLifeCycleListener implements ProcessEngineLifecycleListener {

	protected Logger logger = LoggerFactory.getLogger(ClusterEnabledProcessEngineLifeCycleListener.class);
	
	protected String uniqueNodeId;
	
	protected ProcessEngineLifecycleListener wrappedListener;
	
	protected String currentState;

	public ClusterEnabledProcessEngineLifeCycleListener(String uniqueNodeId) {
		this.uniqueNodeId = uniqueNodeId;
		changeLifeCycleState(EventTypes.BOOTING);
	}

	public ClusterEnabledProcessEngineLifeCycleListener(
			String uniqueNodeId,
			BlockingQueue<Map<String, Object>> eventQueue,
			ProcessEngineLifecycleListener wrappedListener) {
		this(uniqueNodeId);
		this.wrappedListener = wrappedListener;
	}

	public void onProcessEngineBuilt(ProcessEngine processEngine) {

		changeLifeCycleState(EventTypes.PROCESS_ENGINE_READY);

		if (wrappedListener != null) {
			wrappedListener.onProcessEngineBuilt(processEngine);
		}
	}

	public void onProcessEngineClosed(ProcessEngine processEngine) {

		changeLifeCycleState(EventTypes.PROCESS_ENGINE_CLOSED);
		
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
		map.put("id", uniqueNodeId);
		map.put("state", currentState);
		return map;
	}
	
}
