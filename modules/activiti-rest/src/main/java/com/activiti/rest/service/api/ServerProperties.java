package com.activiti.rest.service.api;

import java.util.Properties;

import org.apache.log4j.Logger;

public class ServerProperties {

  private static final Logger log = Logger.getLogger(ServerProperties.class);
  
  protected static Properties properties;

  public static String getServerUrl() {
    String url = null;
    try {
      Properties serverProperties = getProperties();
      if (serverProperties != null) {
        url = serverProperties.getProperty("server.url");
      }
    } catch (Exception e) {
      log.error("Error loading server url", e);
    }
    if (url == null) {
      url = "";
    }
    return url;
  }
  
  protected static Properties getProperties() {
    if (properties == null) {
      try {
        properties = new Properties();
        properties.load(ServerProperties.class.getClassLoader().getResourceAsStream("server.properties"));
      } catch (Exception e) {
        log.error("Error loading properties", e);
      }
    }
    return properties;
  }
}
