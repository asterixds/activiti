package com.activiti.addon.cluster.jobexecutor;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.jobexecutor.FailedJobCommandFactory;

import com.activiti.addon.cluster.metrics.codahale.Meter;

/**
 * @author jbarrez
 */
public class GatherMetricsFailedJobCommandFactory implements FailedJobCommandFactory {
	
	protected Meter failedJobMeter;
	
	protected FailedJobCommandFactory wrappedFailedJobCommandFactory;
	
	public GatherMetricsFailedJobCommandFactory(FailedJobCommandFactory wrappedFailedJobCommandFactory) {
		this.wrappedFailedJobCommandFactory = wrappedFailedJobCommandFactory;
	}
	
	// The idea here is that this factory is called *only* a job has failed
	public Command<Object> getCommand(String jobId, Throwable exception) {
		
		if (failedJobMeter == null) {
			failedJobMeter = new Meter();
		}
		failedJobMeter.mark();
		
		return wrappedFailedJobCommandFactory.getCommand(jobId, exception);
	}

	public Meter getFailedJobMeter() {
		return failedJobMeter;
	}

	public void setFailedJobMeter(Meter failedJobMeter) {
		this.failedJobMeter = failedJobMeter;
	}
	
}
