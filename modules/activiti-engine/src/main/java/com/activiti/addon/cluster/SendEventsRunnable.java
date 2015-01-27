package com.activiti.addon.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.jobexecutor.WrappedAsyncExecutor;
import org.activiti.engine.runtime.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.activiti.addon.cluster.cache.ProcessDefinitionCacheMetricsWrapper;
import com.activiti.addon.cluster.interceptor.GatherMetricsCommandInterceptor;
import com.activiti.addon.cluster.jobexecutor.GatherMetricsFailedJobCommandFactory;
import com.activiti.addon.cluster.lifecycle.ClusterEnabledProcessEngineLifeCycleListener;
import com.activiti.addon.cluster.metrics.JvmMetricsManager;
import com.activiti.addon.cluster.state.MasterConfigurationState;

/**
 * @author jbarrez
 */
public class SendEventsRunnable implements Runnable {

	protected Logger logger = LoggerFactory.getLogger(SendEventsRunnable.class);

	protected String uniqueNodeId;
	protected String ipAddress;
	protected Clock clock;
	protected AdminAppService adminAppService;
	
	protected MasterConfigurationState masterConfigurationState; 
	protected ClusterEnabledProcessEngineLifeCycleListener clusterEnabledProcessEngineLifeCycleListener;
	protected ProcessDefinitionCacheMetricsWrapper processDefinitionCache;
	protected JvmMetricsManager jvmMetricsManager;
	protected GatherMetricsCommandInterceptor gatherMetricsCommandInterceptor;
	protected GatherMetricsFailedJobCommandFactory gatherMetricsFailedJobCommandFactory;
	protected WrappedAsyncExecutor wrappedAsyncExecutor;
	
	public SendEventsRunnable(String uniqueId, String ipAddress, Clock clock, AdminAppService adminAppService) {
		this.uniqueNodeId = uniqueId;
		this.ipAddress = ipAddress;
		this.clock = clock;
		this.adminAppService = adminAppService;
	}

	public void run() {
		
		if (logger.isDebugEnabled()) {
			logger.debug("About to send Activiti Node events...");
		}
		try {
			
			List<Map<String, Object>> events = new ArrayList<Map<String,Object>>();
				
	//		if (masterConfigurationState != null) {
	//			publishEvent(masterConfigurationState.getConfigurationState());
	//		}
			
			if (clusterEnabledProcessEngineLifeCycleListener != null) {
				events.add(clusterEnabledProcessEngineLifeCycleListener.getCurrentlLifeCycleStateEvent());
			}
			
			if (jvmMetricsManager != null) {
				events.add(jvmMetricsManager.gatherMetrics());
			}
			
			if (processDefinitionCache != null) {
				events.add(processDefinitionCache.gatherMetrics());
			}
			
			if (gatherMetricsCommandInterceptor != null) {
				events.add(gatherMetricsCommandInterceptor.gatherMetrics());
			}
			
			if (gatherMetricsFailedJobCommandFactory != null) {
				events.add(gatherMetricsFailedJobCommandFactory.gatherMetrics());
			}
			
			if (wrappedAsyncExecutor != null) {
				events.add(wrappedAsyncExecutor.getJobExecutorState());
			}
			
			publishEvents(events);
			
			}
		catch (Exception e) {
			logger.error("Error while trying to send Activiti Node events", e);
		}
	}
	
	protected void publishEvents(List<Map<String, Object>> events) {
		
		for (Map<String, Object> event : events ) {
			
			// Always add the node id and ip addres for this engine
			event.put("nodeId", uniqueNodeId);
			if (ipAddress != null) {
				event.put("ipAddress", ipAddress);
			}
		
			// Always add the local date
			event.put("timestamp", clock.getCurrentTime());
			
		}
		
		adminAppService.publishEvents(events);
	}

	public ProcessDefinitionCacheMetricsWrapper getWrappedProcessDefinitionCache() {
		return processDefinitionCache;
	}

	public void setWrappedProcessDefinitionCache(ProcessDefinitionCacheMetricsWrapper wrappedProcessDefinitionCache) {
		this.processDefinitionCache = wrappedProcessDefinitionCache;
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

	public WrappedAsyncExecutor getWrappedAsyncExecutor() {
		return wrappedAsyncExecutor;
	}

	public void setWrappedAsyncExecutor(WrappedAsyncExecutor wrappedAsyncExecutor) {
		this.wrappedAsyncExecutor = wrappedAsyncExecutor;
	}

	public GatherMetricsFailedJobCommandFactory getGatherMetricsFailedJobCommandFactory() {
		return gatherMetricsFailedJobCommandFactory;
	}

	public void setGatherMetricsFailedJobCommandFactory(
	    GatherMetricsFailedJobCommandFactory gatherMetricsFailedJobCommandFactory) {
		this.gatherMetricsFailedJobCommandFactory = gatherMetricsFailedJobCommandFactory;
	}
	
}
