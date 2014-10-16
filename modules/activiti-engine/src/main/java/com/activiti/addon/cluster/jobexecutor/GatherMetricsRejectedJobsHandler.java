package com.activiti.addon.cluster.jobexecutor;

import java.util.List;

import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.jobexecutor.RejectedJobsHandler;

import com.activiti.addon.cluster.metrics.codahale.Meter;

/**
 * @author jbarrez
 */
public class GatherMetricsRejectedJobsHandler implements RejectedJobsHandler {

	protected RejectedJobsHandler wrappedRejectedJobsHandler;

	protected Meter rejectedJobsMeter;

	public GatherMetricsRejectedJobsHandler(RejectedJobsHandler rejectedJobsHandler) {
		this.wrappedRejectedJobsHandler = rejectedJobsHandler;
	}

	public void jobsRejected(JobExecutor jobExecutor, List<String> jobs) {

		// Delegate
		if (wrappedRejectedJobsHandler != null) {
			wrappedRejectedJobsHandler.jobsRejected(jobExecutor, jobs);
		}

		// Measure
		if (rejectedJobsMeter == null) {
			rejectedJobsMeter = new Meter();
		}
		rejectedJobsMeter.mark();
		
	}

	public Meter getRejectedJobsMeter() {
		return rejectedJobsMeter;
	}

	public void setRejectedJobsMeter(Meter rejectedJobsMeter) {
		this.rejectedJobsMeter = rejectedJobsMeter;
	}
}
