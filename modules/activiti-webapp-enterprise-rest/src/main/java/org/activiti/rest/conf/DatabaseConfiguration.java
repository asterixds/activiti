package org.activiti.rest.conf;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
public class DatabaseConfiguration {

  private final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);
  
  @Autowired
  protected Environment environment;
  
	@Bean
	public DataSource dataSource() {
		String dataSourceDriver = environment.getProperty("datasource.driver");
		String dataSourceUrl = environment.getProperty("datasource.url");

		String dataSourceUsername = environment.getProperty("datasource.username");
		String dataSourcePassword = environment.getProperty("datasource.password");

		Integer minPoolSize = environment.getProperty("datasource.min-pool-size", Integer.class);
		if (minPoolSize == null) {
			minPoolSize = 10;
		}

		Integer maxPoolSize = environment.getProperty("datasource.max-pool-size", Integer.class);
		if (maxPoolSize == null) {
			maxPoolSize = 100;
		}

		Integer acquireIncrement = environment.getProperty("datasource.acquire-increment", Integer.class);
		if (acquireIncrement == null) {
			acquireIncrement = 5;
		}

		String preferredTestQuery = environment.getProperty("datasource.preferred-test-query");

		Boolean testConnectionOnCheckin = environment.getProperty("datasource.test-connection-on-checkin", Boolean.class);
		if (testConnectionOnCheckin == null) {
			testConnectionOnCheckin = true;
		}

		Boolean testConnectionOnCheckOut = environment.getProperty("datasource.test-connection-on-checkout", Boolean.class);
		if (testConnectionOnCheckOut == null) {
			testConnectionOnCheckOut = true;
		}

		Integer maxIdleTime = environment.getProperty("datasource.max-idle-time", Integer.class);
		if (maxIdleTime == null) {
			maxIdleTime = 1800;
		}

		Integer maxIdleTimeExcessConnections = environment.getProperty("datasource.max-idle-time-excess-connections", Integer.class);
		if (maxIdleTimeExcessConnections == null) {
			maxIdleTimeExcessConnections = 1800;
		}

		if (log.isInfoEnabled()) {
			log.info("Configuring Datasource with following properties (omitted password for security)");
			log.info("datasource driver: " + dataSourceDriver);
			log.info("datasource url : " + dataSourceUrl);
			log.info("datasource user name : " + dataSourceUsername);
			log.info("Min pool size | Max pool size | acquire increment : " + minPoolSize + " | " + maxPoolSize + " | " + acquireIncrement);
		}

		ComboPooledDataSource ds = new ComboPooledDataSource();
		try {
			ds.setDriverClass(dataSourceDriver);
		} catch (PropertyVetoException e) {
			log.error("Could not set Jdbc Driver class", e);
			return null;
		}

		// Connection settings
		ds.setJdbcUrl(dataSourceUrl);
		ds.setUser(dataSourceUsername);
		ds.setPassword(dataSourcePassword);

		// Pool config: see http://www.mchange.com/projects/c3p0/#configuration
		ds.setMinPoolSize(minPoolSize);
		ds.setMaxPoolSize(maxPoolSize);
		ds.setAcquireIncrement(acquireIncrement);
		if (preferredTestQuery != null) {
			ds.setPreferredTestQuery(preferredTestQuery);
		}
		ds.setTestConnectionOnCheckin(testConnectionOnCheckin);
		ds.setTestConnectionOnCheckout(testConnectionOnCheckOut);
		ds.setMaxIdleTimeExcessConnections(maxIdleTimeExcessConnections);
		ds.setMaxIdleTime(maxIdleTime);

		return ds;
	}

  @Bean(name = "transactionManager")
  public PlatformTransactionManager annotationDrivenTransactionManager() {
    DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
    transactionManager.setDataSource(dataSource());
    return transactionManager;
  }
  
}
