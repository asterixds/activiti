package com.activiti.addon.cluster;

import java.util.ArrayList;
import java.util.List;
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
  
  protected String clusterName;
  protected String clusterPassword;
  protected Integer networkStartingPort;
  
  protected Boolean masterConfigurationRequired;
  protected Integer metricSendingInterval;
  
  protected Boolean networkMulticastEnabled;
  protected String networkMulticastGroup;
  protected Integer networkMulticastPort;
  
  protected Boolean networkTcpEnabled;
  protected String networkTcpHost;
  protected Integer networkTcpPort;
  protected List<String> networkTcpInterfaces = new ArrayList<String>();
  
  protected Boolean securityEnabled;
  protected String securityPassword;
  protected String securitySalt;
  protected Integer securityIterationCount;
  
  protected boolean propertyFileExists;
  
  public ClusterConfigProperties() {
    try {
      Properties properties = new Properties();
      properties.load(this.getClass().getResourceAsStream(PROPERTIES_FILE));
      propertyFileExists = true; // Not catching exception = file exists
      
      clusterName = properties.getProperty(CLUSTER_NAME);
      clusterPassword = properties.getProperty(CLUSTER_PASSWORD);
      networkStartingPort = readIntegerProperty(NETWORK_STARTINGPORT, properties);
      
      masterConfigurationRequired = readBooleanProperty(MASTER_CFG_REQUIRED, properties);
      metricSendingInterval = readIntegerProperty(METRIC_SENDING_INTERVAL, properties);
      
      networkMulticastEnabled = readBooleanProperty(NETWORK_MULTICAST_ENABLED, properties);
      networkMulticastGroup = properties.getProperty(NETWORK_MULTICAST_GROUP);
      networkMulticastPort = readIntegerProperty(NETWORK_MULTICAST_PORT, properties);
      
      networkTcpEnabled = readBooleanProperty(NETWORK_TCP_ENABLED, properties);
      networkTcpHost = properties.getProperty(NETWORK_TCP_HOST);
      networkTcpPort = readIntegerProperty(NETWORK_TCP_PORT, properties);
      
      String commaSeparatedInterfaces = properties.getProperty(NETWORK_TCP_INTERFACES);
      if (commaSeparatedInterfaces != null) {
        String[] interfacesArray = commaSeparatedInterfaces.split(",");
        for (String interfaceObject : interfacesArray) {
          networkTcpInterfaces.add(interfaceObject.trim());
        }
      }
      
      securityEnabled = readBooleanProperty(SECURITY_ENABLED, properties);
      securityPassword = properties.getProperty(SECURITY_PASSWORD);
      securitySalt = properties.getProperty(SECURITY_SALT);
      securityIterationCount = readIntegerProperty(SECURITY_ITERATION_COUNT, properties);
      
    } catch (Exception e) {
      logger.warn("Could not read 'activiti-cluster.properties' from classpath. Using defaults or process engine config.");
    }
  }
  
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getClusterPassword() {
    return clusterPassword;
  }

  public void setClusterPassword(String clusterPassword) {
    this.clusterPassword = clusterPassword;
  }

  public Integer getNetworkStartingPort() {
    return networkStartingPort;
  }

  public void setNetworkStartingPort(Integer networkStartingPort) {
    this.networkStartingPort = networkStartingPort;
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

  public Boolean getNetworkMulticastEnabled() {
    return networkMulticastEnabled;
  }

  public void setNetworkMulticastEnabled(Boolean networkMulticastEnabled) {
    this.networkMulticastEnabled = networkMulticastEnabled;
  }

  public String getNetworkMulticastGroup() {
    return networkMulticastGroup;
  }

  public void setNetworkMulticastGroup(String networkMulticastGroup) {
    this.networkMulticastGroup = networkMulticastGroup;
  }

  public Integer getNetworkMulticastPort() {
    return networkMulticastPort;
  }

  public void setNetworkMulticastPort(Integer networkMulticastPort) {
    this.networkMulticastPort = networkMulticastPort;
  }

  public Boolean getNetworkTcpEnabled() {
    return networkTcpEnabled;
  }

  public void setNetworkTcpEnabled(Boolean networkTcpEnabled) {
    this.networkTcpEnabled = networkTcpEnabled;
  }

  public String getNetworkTcpHost() {
    return networkTcpHost;
  }

  public void setNetworkTcpHost(String networkTcpHost) {
    this.networkTcpHost = networkTcpHost;
  }

  public Integer getNetworkTcpPort() {
    return networkTcpPort;
  }

  public void setNetworkTcpPort(Integer networkTcpPort) {
    this.networkTcpPort = networkTcpPort;
  }

  public List<String> getNetworkTcpInterfaces() {
    return networkTcpInterfaces;
  }

  public void setNetworkTcpInterfaces(List<String> networkTcpInterfaces) {
    this.networkTcpInterfaces = networkTcpInterfaces;
  }

  public void addNetworkTcpInterface(String tcpInterface) {
    this.networkTcpInterfaces.add(tcpInterface);
  }

  public Boolean getSecurityEnabled() {
    return securityEnabled;
  }

  public void setSecurityEnabled(Boolean securityEnabled) {
    this.securityEnabled = securityEnabled;
  }

  public String getSecurityPassword() {
    return securityPassword;
  }

  public void setSecurityPassword(String securityPassword) {
    this.securityPassword = securityPassword;
  }

  public String getSecuritySalt() {
    return securitySalt;
  }

  public void setSecuritySalt(String securitySalt) {
    this.securitySalt = securitySalt;
  }

  public Integer getSecurityIterationCount() {
    return securityIterationCount;
  }

  public void setSecurityIterationCount(Integer securityIterationCount) {
    this.securityIterationCount = securityIterationCount;
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
