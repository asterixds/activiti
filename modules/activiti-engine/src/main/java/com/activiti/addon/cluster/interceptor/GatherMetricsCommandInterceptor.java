package com.activiti.addon.cluster.interceptor;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.cmd.CompleteTaskCmd;
import org.activiti.engine.impl.cmd.ExecuteAsyncJobCmd;
import org.activiti.engine.impl.cmd.StartProcessInstanceByMessageCmd;
import org.activiti.engine.impl.cmd.StartProcessInstanceCmd;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.AbstractCommandInterceptor;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.runtime.Clock;

import com.activiti.addon.cluster.metrics.HourCounter;
import com.activiti.addon.cluster.metrics.codahale.Meter;
import com.activiti.addon.cluster.util.MetricsUtil;

/**
 * @author jbarrez
 */
public class GatherMetricsCommandInterceptor extends AbstractCommandInterceptor {
	
	protected Meter startProcessInstanceMeter = new Meter();
	protected HourCounter startProcessInstanceHourCounter = new HourCounter();
	
	protected Meter taskCompletionMeter = new Meter();
	protected HourCounter taskCompletionHourCounter = new HourCounter();
	
	protected Meter jobsExecutionMeter = new Meter();
	protected HourCounter jobExecutionHourCounter = new HourCounter();
	
	private Clock clock;
	
	public GatherMetricsCommandInterceptor(Clock clock) {
		this.clock = clock;
	}
	
	public <T> T execute(CommandConfig config, Command<T> command) {
		
		if (command instanceof ExecuteAsyncJobCmd) {
			handleExecuteJobCmd();
		} else if (command instanceof StartProcessInstanceCmd) {
			handleStartProcessInstanceCmd();
		} else if (command instanceof StartProcessInstanceByMessageCmd) {
			handleStartProcessInstanceCmd();
		} else if (command instanceof CompleteTaskCmd) {
			handleCompleteTaskCmd();
		}
		
		return next.execute(config, command);
	}

	private void handleExecuteJobCmd() {
		jobsExecutionMeter.mark();
		jobExecutionHourCounter.increment(getHour());
	}
	
	private void handleStartProcessInstanceCmd() {
		startProcessInstanceMeter.mark();
		startProcessInstanceHourCounter.increment(getHour());
	}
	
	private void handleCompleteTaskCmd() {
		taskCompletionMeter.mark();
		taskCompletionHourCounter.increment(getHour());
	}
	
	public Map<String, Object> gatherMetrics() {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("type", "runtime-metrics");

		if (jobsExecutionMeter != null) {
			metrics.put("job-execution", MetricsUtil.metricsToMap(jobsExecutionMeter, jobExecutionHourCounter));
		}
		
		if (startProcessInstanceMeter != null) {
			metrics.put("processinstance-start", MetricsUtil.metricsToMap(startProcessInstanceMeter, startProcessInstanceHourCounter));
		}
		
		if (taskCompletionMeter != null) {
			metrics.put("task-completion", MetricsUtil.metricsToMap(taskCompletionMeter, taskCompletionHourCounter));
		}
		
		return metrics;
	}
	
	private int getHour() {
		Calendar currentTime = clock.getCurrentCalendar();
		return currentTime.get(Calendar.HOUR_OF_DAY);
	}
	
}
