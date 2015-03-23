package org.activiti.rest.conf;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({
  @PropertySource(value = "classpath:/META-INF/activiti-rest/db.properties", ignoreResourceNotFound = true),
  @PropertySource(value = "classpath:db.properties", ignoreResourceNotFound = true),
  @PropertySource(value = "classpath:/META-INF/activiti-rest/engine.properties", ignoreResourceNotFound = true),
  @PropertySource(value = "classpath:engine.properties", ignoreResourceNotFound = true)
})
@ComponentScan(basePackages = {"org.activiti.rest.conf"})
@ImportResource({"classpath:/META-INF/activiti-rest/activiti-custom-context.xml"})
public class ApplicationConfiguration {
  
}
