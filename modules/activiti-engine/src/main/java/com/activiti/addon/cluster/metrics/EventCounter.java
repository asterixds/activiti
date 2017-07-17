package com.activiti.addon.cluster.metrics;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple class to take total counts for every hour of the day.
 * 
 * @author jbarrez
 */
public class EventCounter {
	
	public static final String HOUR_COUNTS = "hour-counts";
	public static final String WEEKDAY_COUNTS = "weekday-counts";
	
	protected long[] hours;
	protected long[] weekdays;
	
	public EventCounter() {
		
		hours = new long[24];
		for (int i=0; i<hours.length; i++) {
			hours[i] = 0;
		}
		
		weekdays = new long[7];
		for (int i=0; i<weekdays.length; i++) {
			weekdays[i] = 0;
		}
		
	}
	
	public void eventHappened(Calendar timeStamp) {
		
		int hour = timeStamp.get(Calendar.HOUR_OF_DAY);
		if (hour < hours.length) {
			hours[hour] = hours[hour] + 1;
		}
		
		int weekday = timeStamp.get(Calendar.DAY_OF_WEEK);
		if (weekday < weekdays.length) {
			weekdays[weekday] = weekdays[weekday] + 1;
		}
	}
	
	public Map<String, Map<Integer, Long>> generateMetrics() {
		Map<String, Map<Integer, Long>> map = new HashMap<String, Map<Integer, Long>>(2);
		
		Map<Integer, Long> hourCount = new HashMap<Integer, Long>(hours.length);
		for (int i=0; i<hours.length; i++) {
			hourCount.put(i, hours[i]);
		}
		map.put(HOUR_COUNTS, hourCount);
		
		Map<Integer, Long> weekdayCount = new HashMap<Integer, Long>();
		for (int j=0; j<weekdays.length; j++) {
			weekdayCount.put(j, weekdays[j]);
		}
		map.put(WEEKDAY_COUNTS, weekdayCount);
		
		return map;
	}

}
