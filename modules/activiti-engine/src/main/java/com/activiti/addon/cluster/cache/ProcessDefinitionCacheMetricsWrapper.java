package com.activiti.addon.cluster.cache;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.persistence.deploy.DeploymentCache;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;

import com.activiti.addon.cluster.metrics.codahale.Meter;
import com.activiti.addon.cluster.util.MetricsUtil;

/**
 * @author jbarrez
 */
public class ProcessDefinitionCacheMetricsWrapper implements DeploymentCache<ProcessDefinitionEntity> {

	protected DeploymentCache<ProcessDefinitionEntity> wrappedCache;

	protected Meter cacheMissMeter = new Meter();
	protected Meter cacheHitMeter = new Meter();
	protected Meter addMeter = new Meter();
	protected Meter removeMeter = new Meter();

	public ProcessDefinitionCacheMetricsWrapper(DeploymentCache<ProcessDefinitionEntity> wrappedCache) {
		this.wrappedCache = wrappedCache;
	}

	public ProcessDefinitionEntity get(String id) {
		ProcessDefinitionEntity result = wrappedCache.get(id);
		if (result == null) {
			cacheMissMeter.mark();
		} else {
			cacheHitMeter.mark();
		}
		return result;
	}

	public void add(String id, ProcessDefinitionEntity processDefinitionEntity) {
		addMeter.mark();
		wrappedCache.add(id, processDefinitionEntity);
	}

	public void remove(String id) {
		removeMeter.mark();
		wrappedCache.remove(id);
	}

	public void clear() {
		wrappedCache.clear();
	}

	public DeploymentCache<ProcessDefinitionEntity> getWrappedCache() {
		return wrappedCache;
	}

	public void setWrappedCache(
			DeploymentCache<ProcessDefinitionEntity> wrappedCache) {
		this.wrappedCache = wrappedCache;
	}

	public Meter getCacheMissMeter() {
		return cacheMissMeter;
	}

	public void setCacheMissMeter(Meter cacheMissMeter) {
		this.cacheMissMeter = cacheMissMeter;
	}

	public Meter getCacheHitMeter() {
		return cacheHitMeter;
	}

	public void setCacheHitMeter(Meter cacheHitMeter) {
		this.cacheHitMeter = cacheHitMeter;
	}

	public Meter getAddMeter() {
		return addMeter;
	}

	public void setAddMeter(Meter addMeter) {
		this.addMeter = addMeter;
	}

	public Meter getRemoveMeter() {
		return removeMeter;
	}

	public void setRemoveMeter(Meter removeMeter) {
		this.removeMeter = removeMeter;
	}

	public Map<String, Object> gatherMetrics() {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("type", "process-definition-cache-metrics");
		metrics.put("cache-miss", MetricsUtil.meterToMap(cacheMissMeter));
		metrics.put("cache-hit", MetricsUtil.meterToMap(cacheHitMeter));
		metrics.put("additions", MetricsUtil.meterToMap(addMeter));
		metrics.put("removals", MetricsUtil.meterToMap(removeMeter));
		return metrics;
	}

}
