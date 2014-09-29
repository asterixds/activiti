package com.activiti.addon.cluster;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.activiti.engine.impl.jobexecutor.WrappedJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.activiti.addon.cluster.cache.ProcessDefinitionCacheMetricsWrapper;
import com.activiti.addon.cluster.interceptor.GatherMetricsCommandInterceptor;
import com.activiti.addon.cluster.jobexecutor.JobMetricsManager;
import com.activiti.addon.cluster.lifecycle.ClusterEnabledProcessEngineLifeCycleListener;
import com.activiti.addon.cluster.metrics.JvmMetricsManager;
import com.activiti.addon.cluster.state.AdminAppState;
import com.activiti.addon.cluster.state.AdminAppState.State;
import com.activiti.addon.cluster.state.MasterConfigurationState;

/**
 * @author jbarrez
 */
public class SendEventsRunnable implements Runnable {

	protected Logger logger = LoggerFactory.getLogger(SendEventsRunnable.class);

	protected String uniqueNodeId;
	protected BlockingQueue<Map<String, Object>> eventQueue;
	
	protected AdminAppState adminAppState;
	protected MasterConfigurationState masterConfigurationState; 
	protected ClusterEnabledProcessEngineLifeCycleListener clusterEnabledProcessEngineLifeCycleListener;
	protected ProcessDefinitionCacheMetricsWrapper wrappedProcessDefinitionCache;
	protected JvmMetricsManager jvmMetricsManager;
	protected GatherMetricsCommandInterceptor gatherMetricsCommandInterceptor;
	protected JobMetricsManager jobMetricsManager;
	protected WrappedJobExecutor wrappedJobExecutor;
	
	public SendEventsRunnable(String uniqueId, AdminAppState adminAppState, BlockingQueue<Map<String, Object>> eventQueue) {
		this.uniqueNodeId = uniqueId;
		this.adminAppState = adminAppState;
		this.eventQueue = eventQueue;
	}

	public void run() {
		
		if (logger.isDebugEnabled()) {
			logger.debug("About to send Activiti Node events...");
		}
		try {
			if (adminAppState.getState().equals(State.ALIVE)) {
				
				if (masterConfigurationState != null) {
					publishEvent(masterConfigurationState.getConfigurationState());
				}
				
				if (clusterEnabledProcessEngineLifeCycleListener != null) {
					publishEvent(clusterEnabledProcessEngineLifeCycleListener.getCurrentlLifeCycleStateEvent());
				}
				
				if (jvmMetricsManager != null) {
					publishEvent(jvmMetricsManager.gatherMetrics());
				}
				
				if (wrappedProcessDefinitionCache != null) {
					publishEvent(wrappedProcessDefinitionCache.gatherMetrics());
				}
				
				if (gatherMetricsCommandInterceptor != null) {
					publishEvent(gatherMetricsCommandInterceptor.gatherMetrics());
				}
				
				if (jobMetricsManager != null) {
					publishEvent(jobMetricsManager.gatherMetrics());
				}
				
				if (wrappedJobExecutor != null) {
					publishEvent(wrappedJobExecutor.getJobExecutorState());
				}
				
			} else {
				logger.warn("Admin app is presumed to be dead. Not sending any event to avoid overflowing the event queue");
				eventQueue.clear();
			}
		}
		catch (Exception e) {
			logger.error("Error while trying to send Activiti Node events", e);
		}
	}
	
	protected void publishEvent(Map<String, Object> event) {
		try {
			event.put("id", uniqueNodeId);
			eventQueue.put(event);
		} catch (InterruptedException e) {
			logger.error("Could not send metrics (type='" + event.get("type") + "') to events queue", e);
		}
	}

	public BlockingQueue<Map<String, Object>> getEventQueue() {
		return eventQueue;
	}

	public void setEventQueue(BlockingQueue<Map<String, Object>> eventQueue) {
		this.eventQueue = eventQueue;
	}

	public ProcessDefinitionCacheMetricsWrapper getWrappedProcessDefinitionCache() {
		return wrappedProcessDefinitionCache;
	}

	public void setWrappedProcessDefinitionCache(ProcessDefinitionCacheMetricsWrapper wrappedProcessDefinitionCache) {
		this.wrappedProcessDefinitionCache = wrappedProcessDefinitionCache;
	}

	public JvmMetricsManager getJvmMetricsManager() {
		return jvmMetricsManager;
	}

	public void setJvmMetricsManager(JvmMetricsManager jvmMetricsManager) {
		this.jvmMetricsManager = jvmMetricsManager;
	}

	public GatherMetricsCommandInterceptor getGatherMetricsCommandInterceptor() {
		return gatherMetricsCommandInterceptor;
	}

	public void setGatherMetricsCommandInterceptor(GatherMetricsCommandInterceptor gatherMetricsCommandInterceptor) {
		this.gatherMetricsCommandInterceptor = gatherMetricsCommandInterceptor;
	}

	public JobMetricsManager getJobMetricsManager() {
		return jobMetricsManager;
	}

	public void setJobMetricsManager(JobMetricsManager jobMetricsManager) {
		this.jobMetricsManager = jobMetricsManager;
	}
	
	public MasterConfigurationState getMasterConfigurationState() {
		return masterConfigurationState;
	}

	public void setMasterConfigurationState(
			MasterConfigurationState masterConfigurationState) {
		this.masterConfigurationState = masterConfigurationState;
	}

	public ClusterEnabledProcessEngineLifeCycleListener getClusterEnabledProcessEngineLifeCycleListener() {
		return clusterEnabledProcessEngineLifeCycleListener;
	}

	public void setClusterEnabledProcessEngineLifeCycleListener(ClusterEnabledProcessEngineLifeCycleListener clusterEnabledProcessEngineLifeCycleListener) {
		this.clusterEnabledProcessEngineLifeCycleListener = clusterEnabledProcessEngineLifeCycleListener;
	}

	public WrappedJobExecutor getWrappedJobExecutor() {
		return wrappedJobExecutor;
	}

	public void setWrappedJobExecutor(WrappedJobExecutor wrappedJobExecutor) {
		this.wrappedJobExecutor = wrappedJobExecutor;
	}
	
}
