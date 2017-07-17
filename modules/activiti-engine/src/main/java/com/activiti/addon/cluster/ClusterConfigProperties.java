package com.activiti.addon.cluster;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for the properties file on the classpath used to enable/configure the cluster config functionality.
 * 
 * @author Joram Barrez
 */
public class ClusterConfigProperties {

  private static final Logger logger = LoggerFactory.getLogger(ClusterConfigProperties.class);
  
  private static final String PROPERTIES_FILE = "/activiti-cluster.properties";
  
  private static final String ADMIN_APP_URL ="admin.app.url";
  
  private static final String CLUSTER_NAME = "cluster.name";
  private static final String CLUSTER_USER_NAME = "cluster.username";
  private static final String CLUSTER_PASSWORD = "cluster.password";
  
  private static final String MASTER_CFG_REQUIRED = "master.cfg.required";
  private static final String METRIC_SENDING_INTERVAL = "metric.sending.interval";
  
  protected String adminAppUrl;
  
  protected String clusterName;
  protected String clusterUserName;
  protected String clusterPassword;
  
  protected Boolean masterConfigurationRequired;
  protected Integer metricSendingInterval;
  
  protected boolean propertyFileExists;
  
  public ClusterConfigProperties() {
    try {
      Properties properties = new Properties();
      properties.load(this.getClass().getResourceAsStream(PROPERTIES_FILE));
      propertyFileExists = true; // Not catching exception ==> file exists
      
      adminAppUrl = properties.getProperty(ADMIN_APP_URL);
      
      clusterName = properties.getProperty(CLUSTER_NAME);
      clusterUserName = properties.getProperty(CLUSTER_USER_NAME);
      clusterPassword = properties.getProperty(CLUSTER_PASSWORD);
      
      masterConfigurationRequired = readBooleanProperty(MASTER_CFG_REQUIRED, properties);
      metricSendingInterval = readIntegerProperty(METRIC_SENDING_INTERVAL, properties);
      
    } catch (Exception e) {
      logger.debug("Could not find or read 'activiti-cluster.properties' from classpath. Using defaults or process engine config.");
    }
  }
  
  public String getAdminAppUrl() {
		return adminAppUrl;
	}

	public void setAdminAppUrl(String adminAppUrl) {
		this.adminAppUrl = adminAppUrl;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getClusterUserName() {
		return clusterUserName;
	}

	public void setClusterUserName(String clusterUserName) {
		this.clusterUserName = clusterUserName;
	}

	public String getClusterPassword() {
		return clusterPassword;
	}

	public void setClusterPassword(String clusterPassword) {
		this.clusterPassword = clusterPassword;
	}

	public Boolean getMasterConfigurationRequired() {
		return masterConfigurationRequired;
	}

	public void setMasterConfigurationRequired(Boolean masterConfigurationRequired) {
		this.masterConfigurationRequired = masterConfigurationRequired;
	}

	public Integer getMetricSendingInterval() {
		return metricSendingInterval;
	}

	public void setMetricSendingInterval(Integer metricSendingInterval) {
		this.metricSendingInterval = metricSendingInterval;
	}

	public boolean isPropertyFileExists() {
    return propertyFileExists;
  }

  public void setPropertyFileExists(boolean propertyFileExists) {
    this.propertyFileExists = propertyFileExists;
  }

  protected Integer readIntegerProperty(String name, Properties properties) {
    Integer resultValue = null;
    String propValue = properties.getProperty(name);
    if (propValue != null) {
      resultValue = Integer.valueOf(propValue);
    }
    return resultValue;
  }
  
  protected Boolean readBooleanProperty(String name, Properties properties) {
    Boolean resultValue = false;
    String propValue = properties.getProperty(name);
    if (propValue != null) {
      resultValue = Boolean.valueOf(propValue);
    }
    return resultValue;
  }

}
