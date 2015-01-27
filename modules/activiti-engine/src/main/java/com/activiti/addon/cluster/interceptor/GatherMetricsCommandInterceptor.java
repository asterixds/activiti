package com.activiti.addon.cluster.interceptor;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.cmd.CompleteTaskCmd;
import org.activiti.engine.impl.cmd.ExecuteAsyncJobCmd;
import org.activiti.engine.impl.cmd.ExecuteJobsCmd;
import org.activiti.engine.impl.cmd.StartProcessInstanceByMessageCmd;
import org.activiti.engine.impl.cmd.StartProcessInstanceCmd;
import org.activiti.engine.impl.cmd.SubmitStartFormCmd;
import org.activiti.engine.impl.interceptor.AbstractCommandInterceptor;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.runtime.Clock;

import com.activiti.addon.cluster.metrics.EventCounter;
import com.activiti.addon.cluster.metrics.codahale.Meter;
import com.activiti.addon.cluster.util.MetricsUtil;

/**
 * @author jbarrez
 */
public class GatherMetricsCommandInterceptor extends AbstractCommandInterceptor {
	
	protected Meter startProcessInstanceMeter = new Meter();
	protected EventCounter startProcessInstanceCounter = new EventCounter();
	
	protected Meter taskCompletionMeter = new Meter();
	protected EventCounter taskCompletionCounter = new EventCounter();
	
	protected Meter jobsExecutionMeter = new Meter();
	protected EventCounter jobExecutionCounter = new EventCounter();
	
	private Clock clock;
	
	public GatherMetricsCommandInterceptor(Clock clock) {
		this.clock = clock;
	}
	
	public <T> T execute(CommandConfig config, Command<T> command) {
		
		if (command instanceof StartProcessInstanceCmd
				|| command instanceof StartProcessInstanceByMessageCmd
	      || command instanceof SubmitStartFormCmd) {
			handleStartProcessInstanceCmd();
		} else if (command instanceof CompleteTaskCmd
				|| command instanceof SubmitTaskFormCmd) {
			handleCompleteTaskCmd();
		} else if (command instanceof ExecuteAsyncJobCmd
				|| command instanceof ExecuteJobsCmd) {
			handleExecuteJobCmd();
		}
		
		return next.execute(config, command);
	}

	private void handleExecuteJobCmd() {
		jobsExecutionMeter.mark();
		jobExecutionCounter.eventHappened(getCurrentTime());
	}
	
	private void handleStartProcessInstanceCmd() {
		startProcessInstanceMeter.mark();
		startProcessInstanceCounter.eventHappened(getCurrentTime());
	}
	
	private void handleCompleteTaskCmd() {
		taskCompletionMeter.mark();
		taskCompletionCounter.eventHappened(getCurrentTime());
	}
	
	public Map<String, Object> gatherMetrics() {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("type", "runtime-metrics");

		if (jobsExecutionMeter != null) {
			metrics.put("job-execution", MetricsUtil.metricsToMap(jobsExecutionMeter, jobExecutionCounter));
		}
		
		if (startProcessInstanceMeter != null) {
			metrics.put("processinstance-start", MetricsUtil.metricsToMap(startProcessInstanceMeter, startProcessInstanceCounter));
		}
		
		if (taskCompletionMeter != null) {
			metrics.put("task-completion", MetricsUtil.metricsToMap(taskCompletionMeter, taskCompletionCounter));
		}
		
		return metrics;
	}
	
	private Calendar getCurrentTime() {
		return clock.getCurrentCalendar();
	}
	
}
