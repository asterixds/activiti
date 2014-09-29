package com.activiti.addon.cluster.util;

import java.util.HashMap;
import java.util.Map;

import com.activiti.addon.cluster.metrics.codahale.Meter;
import com.activiti.addon.cluster.metrics.codahale.Snapshot;
import com.activiti.addon.cluster.metrics.codahale.Timer;

/**
 * @author jbarrez
 */
public class MetricsUtil {
	
	public static Map<String, Object> meterToMap(Meter meter) {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("count", meter.getCount());
		metrics.put("mean-rate", meter.getMeanRate());
		metrics.put("one-minute-rate", meter.getOneMinuteRate());
		metrics.put("five-minute-rate", meter.getFiveMinuteRate());
		metrics.put("fifteen-minute-rate", meter.getFifteenMinuteRate());
		return metrics;
	}
	
	public static Map<String, Object> timerToMap(Timer timer) {
		Map<String, Object> metrics = new HashMap<String, Object>();
		metrics.put("count", timer.getCount());
		metrics.put("one-minute-rate", timer.getOneMinuteRate()); // jobs/second for the last minute 
		metrics.put("five-minute-rate", timer.getFiveMinuteRate()); // jobs/second for the last 5 minutes
		metrics.put("fifteen-minute-rate", timer.getFifteenMinuteRate()); // jobs/second for the last 15 minutes
		metrics.put("mean-rate", timer.getMeanRate()); // The mean (weighted average they call it) jobs / second
		
		Snapshot snapshot = timer.getSnapshot();
		metrics.put("min", convertNanoSecondsToMilliSeconds(snapshot.getMin()));
		metrics.put("max", convertNanoSecondsToMilliSeconds(snapshot.getMax()));
		metrics.put("median", convertNanoSecondsToMilliSeconds(snapshot.getMedian()));
		metrics.put("stddev", convertNanoSecondsToMilliSeconds(snapshot.getStdDev()));
		metrics.put("75th-percentile", convertNanoSecondsToMilliSeconds(snapshot.get75thPercentile())	);
		metrics.put("75th-percentile", convertNanoSecondsToMilliSeconds(snapshot.get75thPercentile()));
		metrics.put("95th-percentile", convertNanoSecondsToMilliSeconds(snapshot.get95thPercentile()));
		metrics.put("98th-percentile", convertNanoSecondsToMilliSeconds(snapshot.get98thPercentile()));
		metrics.put("99th-percentile", convertNanoSecondsToMilliSeconds(snapshot.get99thPercentile()));
		
		return metrics;
	}
	
	public static double convertNanoSecondsToMilliSeconds(long nanoSeconds) {
		try {
			return nanoSeconds / 1000000.0;
		} catch (Exception e) {
			return -1.0;
		}
	}
	
	public static double convertNanoSecondsToMilliSeconds(double nanoSeconds) {
		try {
			return nanoSeconds / 1000000.0;
		} catch (Exception e) {
			return -1.0;
		}
	}

}
