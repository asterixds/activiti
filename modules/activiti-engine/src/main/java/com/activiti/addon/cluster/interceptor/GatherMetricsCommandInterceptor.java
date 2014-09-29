package com.activiti.addon.cluster.interceptor;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.cmd.CompleteTaskCmd;
import org.activiti.engine.impl.cmd.ExecuteJobsCmd;
import org.activiti.engine.impl.cmd.StartProcessInstanceByMessageCmd;
import org.activiti.engine.impl.cmd.StartProcessInstanceCmd;
import org.activiti.engine.impl.interceptor.AbstractCommandInterceptor;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandConfig;

import com.activiti.addon.cluster.metrics.codahale.Meter;
import com.activiti.addon.cluster.metrics.codahale.Timer;
import com.activiti.addon.cluster.metrics.codahale.Timer.Context;
import com.activiti.addon.cluster.util.MetricsUtil;

/**
 * @author jbarrez
 */
public class GatherMetricsCommandInterceptor extends AbstractCommandInterceptor {
	
	protected Meter startProcessInstanceMeter;
	protected Meter startProcessInstanceByMessageMeter;
	protected Meter taskCompletionMeter;
	protected Timer jobsExecutionTimer; 
	
	public <T> T execute(CommandConfig config, Command<T> command) {
		
		Context timerContext = null;
		if (command instanceof ExecuteJobsCmd) {
			timerContext = handleExecuteJobCmd();
		} else if (command instanceof StartProcessInstanceCmd) {
			handleStartProcessInstanceCmd();
		} else if (command instanceof StartProcessInstanceByMessageCmd) {
			handleStartProcessInstanceByMsgCmd();
		} else if (command instanceof CompleteTaskCmd) {
			handleCompleteTaskCmd();
		}
		
		T result = next.execute(config, command);
		
		if (timerContext != null) {
			timerContext.stop();
		}
		
		return result;
	}

	private Context handleExecuteJobCmd() {
		Context timerContext;
		if (jobsExecutionTimer == null) {
			jobsExecutionTimer = new Timer();
		}
		timerContext = jobsExecutionTimer.time();
		return timerContext;
	}
	
	private void handleStartProcessInstanceCmd() {
		if (startProcessInstanceMeter == null) {
			startProcessInstanceMeter = new Meter();
		}
		startProcessInstanceMeter.mark();
	}
	
	private void handleStartProcessInstanceByMsgCmd() {
		if (startProcessInstanceMeter == null) {
			startProcessInstanceMeter = new Meter();
		}
		startProcessInstanceMeter.mark();
		if (startProcessInstanceByMessageMeter == null) {
			startProcessInstanceByMessageMeter = new Meter();
		}
		startProcessInstanceByMessageMeter.mark();
	}
	
	private void handleCompleteTaskCmd() {
		if (taskCompletionMeter == null) {
			taskCompletionMeter = new Meter();
		}
		taskCompletionMeter.mark();
	}
	
	public Map<String, Object> gatherMetrics() {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("type", "runtime-metrics");

		if (jobsExecutionTimer != null) {
			metrics.put("job-execution", MetricsUtil.timerToMap(jobsExecutionTimer));
		}
		
		if (startProcessInstanceMeter != null) {
			metrics.put("processinstance-start", MetricsUtil.meterToMap(startProcessInstanceMeter));
		}
		
		if (startProcessInstanceByMessageMeter != null) {
			metrics.put("processinstance-start-by-msg", MetricsUtil.meterToMap(startProcessInstanceByMessageMeter));
		}
		
		if (taskCompletionMeter != null) {
			metrics.put("task-completion", MetricsUtil.meterToMap(taskCompletionMeter));
		}
		
		return metrics;
	}
	
}
