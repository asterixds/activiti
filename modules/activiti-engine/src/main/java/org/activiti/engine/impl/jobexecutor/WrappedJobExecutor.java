package org.activiti.engine.impl.jobexecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For enterprise usage: gather metrics about the job executor
 * 
 * Hack-alert: in the same package as the {@link JobExecutor}, since we
 * need to wrap an abstract class with protected methods.
 * 
 * @author jbarrez
 */
public class WrappedJobExecutor extends JobExecutor {
	
	protected Logger logger = LoggerFactory.getLogger(WrappedJobExecutor.class);
	
	public enum State { STARTED, STOPPING, STOPPED };
	
	protected JobExecutor originalJobExecutor;
	
	protected State state = State.STOPPED;
	
	public WrappedJobExecutor(JobExecutor originalJobExecutor) {
		this.originalJobExecutor = originalJobExecutor;
	}
	
	@Override
	public void start() {
		logger.info("Starting job executor");
		super.start();
		this.state = State.STARTED;
		logger.info("Job executor is started");
	}
	
	@Override
	public synchronized void shutdown() {
		logger.info("Stopping job executor");
		this.state = State.STOPPING;
		super.shutdown();
		this.state = State.STOPPED;
		logger.info("Job excutor is stopped");
	}

	@Override
	protected void startExecutingJobs() {
		originalJobExecutor.startExecutingJobs();
	}

	@Override
	protected void stopExecutingJobs() {
		originalJobExecutor.stopExecutingJobs();
	}

	@Override
	public void executeJobs(List<String> jobs) {
		originalJobExecutor.executeJobs(jobs);
	}

	public State getState() {
		return state;
	}
	
	public Map<String, Object> getJobExecutorState() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("type", "job-executor");
		map.put("state", getState().toString().toLowerCase());
		return map;
	}
	
}
