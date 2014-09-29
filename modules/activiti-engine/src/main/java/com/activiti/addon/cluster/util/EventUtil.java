package com.activiti.addon.cluster.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jbarrez
 */
public class EventUtil {
	
	public static Map<String, Object> createProcessEngineLifecycleEvent(String uniqueNodeId, String eventType) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("type", "process-engine-lifecycle");
		map.put("id", uniqueNodeId);
		map.put("eventType", eventType);
		return map;
	}

}
