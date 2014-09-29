package com.activiti.addon.cluster.state;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jbarrez
 */
public class MasterConfigurationState {
	
	protected boolean usingMasterConfiguration;
	
	protected String configurationId;

	public boolean isUsingMasterConfiguration() {
		return usingMasterConfiguration;
	}

	public void setUsingMasterConfiguration(boolean usingMasterConfiguration) {
		this.usingMasterConfiguration = usingMasterConfiguration;
	}

	public String getConfigurationId() {
		return configurationId;
	}

	public void setConfigurationId(String configurationId) {
		this.configurationId = configurationId;
	}
	
	public Map<String, Object> getConfigurationState() {
		Map<String, Object> state = new HashMap<String, Object>();
		state.put("type", "master-configuration");
		state.put("enabled", usingMasterConfiguration);
		
		if (configurationId != null) {
			state.put("configuration-id", configurationId);
		}
		
		return state;
	}
	
}
