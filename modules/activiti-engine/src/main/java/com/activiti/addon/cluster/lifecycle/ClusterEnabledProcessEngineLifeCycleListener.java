package com.activiti.addon.cluster.lifecycle;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.activiti.addon.cluster.constants.EventTypes;
import com.activiti.addon.cluster.util.EventUtil;
import com.hazelcast.core.HazelcastInstance;

/**
 * @author jbarrez
 */
public class ClusterEnabledProcessEngineLifeCycleListener implements ProcessEngineLifecycleListener {

	protected Logger logger = LoggerFactory.getLogger(ClusterEnabledProcessEngineLifeCycleListener.class);
	
	protected HazelcastInstance hazelcastInstance;

	protected String uniqueNodeId;
	
	protected BlockingQueue<Map<String, Object>> eventQueue;

	protected ProcessEngineLifecycleListener wrappedListener;
	
	protected String currentState;

	public ClusterEnabledProcessEngineLifeCycleListener(HazelcastInstance hazelcastInstance, String uniqueNodeId, BlockingQueue<Map<String, Object>> eventQueue) {
		this.hazelcastInstance = hazelcastInstance;
		this.uniqueNodeId = uniqueNodeId;
		this.eventQueue = eventQueue;
		
		// Send 'booting' event to admin app
		changeLifeCycleState(EventTypes.BOOTING);
	}

	public ClusterEnabledProcessEngineLifeCycleListener(
			HazelcastInstance hazelcastInstance,
			String uniqueNodeId,
			BlockingQueue<Map<String, Object>> eventQueue,
			ProcessEngineLifecycleListener wrappedListener) {
		this(hazelcastInstance, uniqueNodeId, eventQueue);
		this.wrappedListener = wrappedListener;
	}

	public void onProcessEngineBuilt(ProcessEngine processEngine) {

		changeLifeCycleState(EventTypes.PROCESS_ENGINE_READY);

		if (wrappedListener != null) {
			wrappedListener.onProcessEngineBuilt(processEngine);
		}
	}

	public void onProcessEngineClosed(ProcessEngine processEngine) {

		try {
			changeLifeCycleState(EventTypes.PROCESS_ENGINE_CLOSED);
			
			if (hazelcastInstance != null) {
				logger.info("Shutting down hazelcast instance");
				hazelcastInstance.shutdown();
			} else {
				logger.info("No hazelcast instance found. Nothing to shut down.");
			}
		} catch (Exception e) {
			logger.warn("Could not shut down Hazelcast properly", e);
		}
		
		if (wrappedListener != null) {
			wrappedListener.onProcessEngineClosed(processEngine);
		}
	}
	
	public Map<String, Object> getCurrentlLifeCycleStateEvent() {
		return EventUtil.createProcessEngineLifecycleEvent(uniqueNodeId, currentState);
	}
	
	protected void changeLifeCycleState(String state) {
		currentState = state;
		sendCurrentLifeCycleStateEvent();
	}
	
	protected void sendCurrentLifeCycleStateEvent() {
		try {
			eventQueue.put(getCurrentlLifeCycleStateEvent());
		} catch (InterruptedException e) {
			logger.error("Couldn't send lifecycle state event to event queue", e);
		}
	}
	
}
