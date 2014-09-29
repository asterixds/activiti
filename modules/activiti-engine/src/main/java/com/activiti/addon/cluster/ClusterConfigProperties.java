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
  
  private static final String CLUSTER_NAME = "cluster.name";
  private static final String CLUSTER_PASSWORD = "cluster.password";
  private static final String NETWORK_STARTINGPORT = "network.startingport";
  
  private static final String MASTER_CFG_REQUIRED = "master.cfg.required";
  private static final String METRIC_SENDING_INTERVAL = "metric.sending.interval";
  
  private static final String NETWORK_MULTICAST_ENABLED = "network.multicast.enabled";
  private static final String NETWORK_MULTICAST_GROUP = "network.multicast.group";
  private static final String NETWORK_MULTICAST_PORT = "network.multicast.port";
  private static final String NETWORK_TCP_ENABLED = "network.tcpip.enabled";
  private static final String NETWORK_TCP_HOST = "network.tcpip.host";
  private static final String NETWORK_TCP_PORT = "network.tcpip.port";
  private static final String NETWORK_TCP_INTERFACES = "network.tcpip.interfaces";
    
  private static final String SECURITY_ENABLED = "security.enabled";
  private static final String SECURITY_PASSWORD = "security.password";
  private static final String SECURITY_SALT = "security.salt";
  private static final String SECURITY_ITERATION_COUNT = "security.iterationcount";
  
  protected Properties properties = new Properties();
  protected boolean exists;
  
  public ClusterConfigProperties() {
    try {
      properties.load(this.getClass().getResourceAsStream(PROPERTIES_FILE));
      exists = true; // Not catching exception = file exists
    } catch (Exception e) {
      logger.warn("Could not read 'activiti-cluster.properties' from classpath. Using defaults.");
    }
  }
  
  public boolean exists() {
    return properties != null && exists;
  }
  
  public String getClusterName() {
    return properties.getProperty(CLUSTER_NAME);
  }
  
  public String getClusterPassword() {
    return properties.getProperty(CLUSTER_PASSWORD);
  }
  
  public String getNetworkStartingPort() {
    return properties.getProperty(NETWORK_STARTINGPORT);
  }
  
  public String getMasterConfigurationRequired() {
    return properties.getProperty(MASTER_CFG_REQUIRED);
  }
  
  public String getMetricSendingInterval() {
    return properties.getProperty(METRIC_SENDING_INTERVAL);
  }
  
  public String getNetworkTcpHost() {
    return properties.getProperty(NETWORK_TCP_HOST);
  }
  
  public Integer getNetworkTcpPort() {
    return Integer.valueOf(properties.getProperty(NETWORK_TCP_PORT));
  }
  
  public Boolean isNetworkMulticastEnabled() {
    String multiCastEnabledString = properties.getProperty(NETWORK_MULTICAST_ENABLED);
    if (multiCastEnabledString != null) {
      return Boolean.valueOf(multiCastEnabledString);
    }
    return false;
  }
  
  public String getNetworkMulticastGroup() {
    return properties.getProperty(NETWORK_MULTICAST_GROUP);
  }
  
  public String getNetworkMulticastPort() {
    return properties.getProperty(NETWORK_MULTICAST_PORT);
  }
  
  public Boolean isNetworkTcpEnabled() {
    String tcpIpEnabledString = properties.getProperty(NETWORK_TCP_ENABLED);
    if (tcpIpEnabledString != null) {
      return  Boolean.valueOf(tcpIpEnabledString);
    }
    return false;
  }
  
  public String getNetworkTcpInterfaces() {
    return properties.getProperty(NETWORK_TCP_INTERFACES);
  }
  
  public Boolean isSecurityEnabled() {
    Boolean securityEnabled = false;
    String securityEnabledString = properties.getProperty(SECURITY_ENABLED);
    if (securityEnabledString != null) {
      securityEnabled = Boolean.valueOf(securityEnabledString);
    }
    return securityEnabled;
  }
  
  public String getSecurityPassword() {
    return properties.getProperty(SECURITY_PASSWORD);
  }
  
  public String getSecuritySalt() {
    return properties.getProperty(SECURITY_SALT);
  }
  
  public String getSecurityIterationCount() {
    return properties.getProperty(SECURITY_ITERATION_COUNT);
  }

}
