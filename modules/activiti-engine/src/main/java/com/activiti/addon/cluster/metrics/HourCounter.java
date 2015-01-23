package com.activiti.addon.cluster.metrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple class to take total counts for every hour of the day.
 * 
 * @author jbarrez
 */
public class HourCounter {
	
	protected long[] hours;
	
	public HourCounter() {
		hours = new long[24];
		for (int i=0; i<hours.length; i++) {
			hours[i] = 0L;
		}
	}
	
	public void increment(int hour) {
		if (hour < hours.length) {
			hours[hour] = hours[hour] + 1;
		}
	}
	
	public Map<Integer, Long> getCounts() {
		Map<Integer, Long> counts = new HashMap<Integer, Long>();
		for (int i=0; i<hours.length; i++) {
			counts.put(i, hours[i]);
		}
		return counts;
	}

}
