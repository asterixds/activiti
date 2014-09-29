package com.activiti.addon.cluster.metrics;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspired by code from http://metrics.codahale.com/
 * 
 * @author jbarrez
 */
public class JvmMetricsManager {
	
	protected MemoryMXBean memoryMxBean;
	protected ClassLoadingMXBean classLoadingMXBean;
	protected RuntimeMXBean runtimeMXBean;
	protected List<GarbageCollectorMXBean> garbageCollectorMXBeans;
	
	public JvmMetricsManager() {
		this.memoryMxBean = ManagementFactory.getMemoryMXBean();
		this.classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
		this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		this.garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();;
	}
	
	public Map<String, Object> gatherMetrics() {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("type", "jvm-metrics");
		metrics.put("memory", gatherMemoryyMetrics());
	    metrics.put("classLoading", gatherClassLoadingMetrics());
	    metrics.put("gc", gatherGarbageCollectionMetrics());
	    metrics.put("general", gatherGeneralInfo());
	    return metrics;
	}

	protected Map<String, Object> gatherMemoryyMetrics() {
		
		Map<String, Object> metrics = new HashMap<String, Object>();
		
		// Init memory
		
		long heapInit = memoryMxBean.getHeapMemoryUsage().getInit();
		metrics.put("heap.init", heapInit);
		
		long nonHeapInit = memoryMxBean.getNonHeapMemoryUsage().getInit();
		metrics.put("nonheap.init", nonHeapInit);
		
		long totalInit = heapInit + nonHeapInit;
		metrics.put("total.init", totalInit);
		
		// Used memory
		
		long heapUsed = memoryMxBean.getHeapMemoryUsage().getUsed();
		metrics.put("heap.used", heapUsed);
		
		long nonHeapUsed = memoryMxBean.getNonHeapMemoryUsage().getUsed();
		metrics.put("nonheap.used", nonHeapUsed);
		
		long totalUserd = heapUsed + nonHeapUsed;
		metrics.put("total.used", totalUserd);
        
		// Max memory
		
		long heapMax = memoryMxBean.getHeapMemoryUsage().getMax();
		metrics.put("heap.max", heapMax);
		
		long nonHeapMax = memoryMxBean.getNonHeapMemoryUsage().getMax();
		metrics.put("nonheap.max", nonHeapMax);
		
		long totalMax = heapMax + nonHeapMax;
		metrics.put("total.max", totalMax);
		
		// Committed memory
		long heapCommitted = memoryMxBean.getHeapMemoryUsage().getCommitted();
		metrics.put("heap.comitted", heapCommitted);
		
		long nonHeapCommitted = memoryMxBean.getNonHeapMemoryUsage().getCommitted();
		metrics.put("nonheap.committed", nonHeapCommitted);
        
		long totalCommitted = heapCommitted + nonHeapCommitted;
		metrics.put("total.committed", totalCommitted);

		// Usage
		
		MemoryUsage usage = memoryMxBean.getHeapMemoryUsage();
		metrics.put("heap.usage", usage.getUsed() / usage.getMax());
		
		usage = memoryMxBean.getNonHeapMemoryUsage();
		metrics.put("nonheap.usage", usage.getUsed() / usage.getMax());
		
		return metrics;
	}
	
	protected Map<String, Object> gatherClassLoadingMetrics() {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("loaded.count", classLoadingMXBean.getLoadedClassCount());
		metrics.put("total.count", classLoadingMXBean.getTotalLoadedClassCount());
		metrics.put("unloaded.count", classLoadingMXBean.getUnloadedClassCount());
		return metrics;
	}
	

	protected List<Map<String, Object>> gatherGarbageCollectionMetrics() {
		List<Map<String, Object>> gcs = new ArrayList<Map<String,Object>>();
		
		for (final GarbageCollectorMXBean gc : garbageCollectorMXBeans) {
            Map<String, Object> gcMetric = new HashMap<String, Object>();
            gcMetric.put("name", gc.getName());
            gcMetric.put("collection.count", gc.getCollectionCount());
            gcMetric.put("collection.time", gc.getCollectionTime());
            gcs.add(gcMetric);
        }
		return gcs;
	}
	
	protected Map<String, Object> gatherGeneralInfo() {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("uptime", runtimeMXBean.getUptime());
		metrics.put("vm.name", runtimeMXBean.getVmName());
		metrics.put("vm.vendor", runtimeMXBean.getVmVendor());
		metrics.put("vm.version", runtimeMXBean.getVmVersion());
		return metrics;
	}


}
