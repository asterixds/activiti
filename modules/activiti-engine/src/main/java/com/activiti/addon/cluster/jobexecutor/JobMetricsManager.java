package com.activiti.addon.cluster.jobexecutor;

import java.util.HashMap;
import java.util.Map;

import com.activiti.addon.cluster.util.MetricsUtil;

/**
 * @author jbarrez
 */
public class JobMetricsManager {
	
	protected GatherMetricsRejectedJobsHandler gatherMetricsRejectedJobsHandler;
	protected GatherMetricsFailedJobCommandFactory gatherMetricsFailedJobCommandFactory;
	
	public JobMetricsManager(GatherMetricsRejectedJobsHandler gatherMetricsRejectedJobsHandler,
			GatherMetricsFailedJobCommandFactory gatherMetricsFailedJobCommandFactory) {
		this.gatherMetricsRejectedJobsHandler = gatherMetricsRejectedJobsHandler;
		this.gatherMetricsFailedJobCommandFactory = gatherMetricsFailedJobCommandFactory;
	}
	
	public Map<String, Object> gatherMetrics() {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("type", "job-metrics");
		
		if (gatherMetricsRejectedJobsHandler != null 
				&& gatherMetricsRejectedJobsHandler.getRejectedJobsMeter() != null) {
			metrics.put("job-queue-full", MetricsUtil.meterToMap(gatherMetricsRejectedJobsHandler.getRejectedJobsMeter()));
		}
		
		if (gatherMetricsFailedJobCommandFactory != null
				&& gatherMetricsFailedJobCommandFactory.getFailedJobMeter() != null) {
			metrics.put("failed-jobs", MetricsUtil.meterToMap(gatherMetricsFailedJobCommandFactory.getFailedJobMeter()));
		}
		
		return metrics;
	}

}
