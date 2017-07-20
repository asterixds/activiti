package com.activiti.addon.cluster.lifecycle;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jbarrez
 */
public class WrappedAsyncExecutor extends DefaultAsyncJobExecutor {
	
	protected Logger logger = LoggerFactory.getLogger(WrappedAsyncExecutor.class);
	
	public enum State { STARTED, STOPPING, STOPPED };
	
	protected AsyncExecutor originalAsyncExecutor;
	
	protected State state = State.STARTED;
	
	public WrappedAsyncExecutor(AsyncExecutor originalAsyncExecutor) {
		this.originalAsyncExecutor = originalAsyncExecutor;
	}
	
	@Override
	public void start() {
		logger.info("Starting async executor");
		originalAsyncExecutor.start();
		this.state = State.STARTED;
		logger.info("Async executor is started");
	}
	
	@Override
	public synchronized void shutdown() {
		logger.info("Stopping async executor");
		this.state = State.STOPPING;
		originalAsyncExecutor.shutdown();
		this.state = State.STOPPED;
		logger.info("Async excutor is stopped");
	}

  @Override
  public boolean executeAsyncJob(Job job) {
	  return originalAsyncExecutor.executeAsyncJob(job);
  }

  @Override
  public boolean isAutoActivate() {
    return originalAsyncExecutor.isAutoActivate();
  }

  @Override
  public void setAutoActivate(boolean isAutoActivate) {
    originalAsyncExecutor.setAutoActivate(isAutoActivate);
  }

  @Override
  public boolean isActive() {
    return originalAsyncExecutor.isActive();
  }

  @Override
  public String getLockOwner() {
    return originalAsyncExecutor.getLockOwner();
  }

  @Override
  public int getTimerLockTimeInMillis() {
    return originalAsyncExecutor.getTimerLockTimeInMillis();
  }

  @Override
  public int getAsyncJobLockTimeInMillis() {
    return originalAsyncExecutor.getAsyncJobLockTimeInMillis();
  }

  @Override
  public void setAsyncJobLockTimeInMillis(int asyncJobLockTimeInMillis) {
    originalAsyncExecutor.setAsyncJobLockTimeInMillis(asyncJobLockTimeInMillis);
  }

  @Override
  public int getMaxTimerJobsPerAcquisition() {
    return originalAsyncExecutor.getMaxTimerJobsPerAcquisition();
  }

  @Override
  public int getMaxAsyncJobsDuePerAcquisition() {
    return originalAsyncExecutor.getMaxAsyncJobsDuePerAcquisition();
  }

  @Override
  public int getDefaultTimerJobAcquireWaitTimeInMillis() {
    return originalAsyncExecutor.getDefaultTimerJobAcquireWaitTimeInMillis();
  }

  @Override
  public int getDefaultAsyncJobAcquireWaitTimeInMillis() {
    return originalAsyncExecutor.getDefaultAsyncJobAcquireWaitTimeInMillis();
  }

  public State getState() {
		return state;
	}
	
	public Map<String, Object> getJobExecutorState() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("type", "async-executor");
		map.put("state", getState().toString().toLowerCase());
		return map;
	}
	
}