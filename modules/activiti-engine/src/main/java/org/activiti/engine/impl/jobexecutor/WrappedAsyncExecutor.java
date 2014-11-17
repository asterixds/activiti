package org.activiti.engine.impl.jobexecutor;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For enterprise usage: gather metrics about the job executor
 * 
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
		super.start();
		this.state = State.STARTED;
		logger.info("Async executor is started");
	}
	
	@Override
	public synchronized void shutdown() {
		logger.info("Stopping async executor");
		this.state = State.STOPPING;
		super.shutdown();
		this.state = State.STOPPED;
		logger.info("Async excutor is stopped");
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
