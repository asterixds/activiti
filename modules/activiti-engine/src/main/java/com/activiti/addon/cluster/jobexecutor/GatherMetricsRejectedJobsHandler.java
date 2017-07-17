package com.activiti.addon.cluster.jobexecutor;

/**
 * @author jbarrez
 */
public class GatherMetricsRejectedJobsHandler /*implements RejectedJobsHandler*/ {

	/*protected RejectedJobsHandler wrappedRejectedJobsHandler;

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
	}*/
}
