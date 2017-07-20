package com.activiti.addon.cluster;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineLifecycleListener;
import org.activiti.engine.cfg.AbstractProcessEngineConfigurator;
import org.activiti.engine.cfg.ProcessEngineConfigurator;
import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.interceptor.CommandInterceptor;
import org.activiti.engine.impl.persistence.deploy.DeploymentCache;
import org.activiti.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.activiti.addon.cluster.cache.ProcessDefinitionCacheMetricsWrapper;
import com.activiti.addon.cluster.interceptor.GatherMetricsCommandInterceptor;
import com.activiti.addon.cluster.jobexecutor.GatherMetricsFailedJobCommandFactory;
import com.activiti.addon.cluster.json.MasterConfigurationRepresentation;
import com.activiti.addon.cluster.json.ProcessEngineMasterConfigurationRepresentation;
import com.activiti.addon.cluster.lifecycle.ClusterEnabledProcessEngineLifeCycleListener;
import com.activiti.addon.cluster.lifecycle.WrappedAsyncExecutor;
import com.activiti.addon.cluster.metrics.JvmMetricsManager;
import com.activiti.addon.cluster.state.MasterConfigurationState;
import com.activiti.license.LicenseException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jbarrez
 */
public class ActivitiClusterConfigurator implements ProcessEngineConfigurator {

	protected Logger logger = LoggerFactory.getLogger(ActivitiClusterConfigurator.class);

	// Should be run as first configurator, hence the low priority value
	public static int PRIORITY = AbstractProcessEngineConfigurator.DEFAULT_CONFIGURATOR_PRIORITY - 5000;

	/*
	 * Constants
	 */

	private static final String NAME = "Activiti Enterprise Cluster Addon";

	/*
	 * Instance members 
	 */
	
	protected AdminAppService adminAppService;
	
	protected ClusterConfigProperties clusterConfigProperties;
	
	protected String uniqueNodeId;
	
	protected String ipAddress;
	
	protected String localNodeHost;
	
	protected Integer localNodePort;
	
	protected MasterConfigurationState masterConfigurationState = new MasterConfigurationState();;
	
	protected ProcessEngineMasterConfigurationRepresentation masterConfiguration;
	
	protected WrappedAsyncExecutor wrappedAsyncExecutor;
	
	protected JvmMetricsManager jvmMetricsManager;
	
	protected ClusterEnabledProcessEngineLifeCycleListener clusterEnabledProcessEngineLifecycleListener;
	
	protected ProcessDefinitionCacheMetricsWrapper wrappedProcessDefinitionCache;
	
	protected GatherMetricsCommandInterceptor gatherMetricsCommandInterceptor;
	
	protected GatherMetricsFailedJobCommandFactory gatherMetricsFailedJobCommandFactory;
	
	public ActivitiClusterConfigurator(ClusterConfigProperties clusterConfigProperties) {
	  this.clusterConfigProperties = clusterConfigProperties;
	}
	
	/*
	 * The logic 
	 */

	public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        try {
            if (processEngineConfiguration.getLicenseHolder().isDepartemental()) {
                logger.info("Departemental license found: cluster configuration disabled");
                return;
            }
        } catch(LicenseException le) {
            logger.error("It was not possible to inspect the license before init to decide if the license is departemental or not. " +
                    "It will be treated as if it was and thus the cluster configuration will be disabled.", le);
            return;
        }

		
		// Let this be tweakable? Send ip info?
		uniqueNodeId = UUID.randomUUID().toString();
		logger.info(NAME + " : initializing engine node with id " + uniqueNodeId + "...");
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			if (inetAddress != null) {
				if (inetAddress.getHostName() != null) {
					ipAddress = inetAddress.getHostName().toString();
				} else if (inetAddress.getHostAddress() != null) {
					ipAddress = inetAddress.getHostAddress();
				}
			}
        } catch (Exception e) { }
		
		initAdminAppService();
		
		// Master config support
		if (processEngineConfiguration.getEnterpriseEnableMasterConfiguration()) {
			initMasterConfigurationPreInit(processEngineConfiguration);
		}
		
		// Add a process engine lifecycle listener to send the event when the engine is ready/closing down
		initProcessEngineLifeCycleListener(processEngineConfiguration);
		
		// Add custom command interceptor (keeps metrics for command execution)
		initCustomCommandInterceptor(processEngineConfiguration);
		
		// Enable async job executor
		initJobExecutor(processEngineConfiguration);
	}
	
	protected void initAdminAppService() {
		this.adminAppService = new AdminAppService(
				clusterConfigProperties.getAdminAppUrl(), 
				clusterConfigProperties.getClusterName(),
				clusterConfigProperties.getClusterUserName(),
				clusterConfigProperties.getClusterPassword());
	}
	
	public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
	  
        try {
            if (processEngineConfiguration.getLicenseHolder().isDepartemental()) {
                logger.info("Departemental license found: cluster configuration disabled");
                return;
            }
        } catch(LicenseException le) {
            logger.error("It was not possible to inspect the license during conguration to decide if the license is departemental or not. " +
                    "It will be treated as if it was and thus the cluster configuration will be disabled.", le);
            return;
        }

        // Process definitions cache
		initProcessDefinitionCache(processEngineConfiguration);
		
		// Job rejection and failed
		initFailedJobCommandFactory(processEngineConfiguration);
		
		// Master config support
		if (processEngineConfiguration.getEnterpriseEnableMasterConfiguration()) {
			initMasterConfigurationPostInit(processEngineConfiguration);
		}
		
		// Job executor state
		initWrappedAsyncExecutor(processEngineConfiguration);
		
		// Fire up the JVM metrics gathering
		initJvmMetricsManager();
		
		// HAS to be at the end here, has dependencies on all the other stuff above
		initSendEventsThread(processEngineConfiguration);
		
		logger.info(NAME + " : initialization completed.");
	}

	protected void initProcessDefinitionCache(ProcessEngineConfigurationImpl processEngineConfiguration) {
		DeploymentCache<ProcessDefinitionCacheEntry> processDefinitionCache = processEngineConfiguration.getProcessDefinitionCache();
		wrappedProcessDefinitionCache = new ProcessDefinitionCacheMetricsWrapper(processDefinitionCache);
		
		processEngineConfiguration.setProcessDefinitionCache(wrappedProcessDefinitionCache);
		processEngineConfiguration.getDeploymentManager().setProcessDefinitionCache(wrappedProcessDefinitionCache);
	}
	
	protected void initFailedJobCommandFactory(ProcessEngineConfigurationImpl processEngineConfiguration) {
		gatherMetricsFailedJobCommandFactory = new GatherMetricsFailedJobCommandFactory(processEngineConfiguration.getFailedJobCommandFactory());
		processEngineConfiguration.setFailedJobCommandFactory(gatherMetricsFailedJobCommandFactory);
	}
	
	protected void initProcessEngineLifeCycleListener(ProcessEngineConfigurationImpl processEngineConfiguration) {
		clusterEnabledProcessEngineLifecycleListener = new ClusterEnabledProcessEngineLifeCycleListener(
    processEngineConfiguration.getClock(),
		processEngineConfiguration.getProcessEngineLifecycleListener());
		processEngineConfiguration.setProcessEngineLifecycleListener(clusterEnabledProcessEngineLifecycleListener);
	}

	protected void initMasterConfigurationPreInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		fetchMasterConfigFromAdminApp();
		
		if (masterConfiguration != null) {
			
			logger.info("Master configuration enabled. Changing local configuration to master configuration.");
			
			masterConfigurationState.setUsingMasterConfiguration(true);
			masterConfigurationState.setConfigurationId(masterConfiguration.getConfigId());

			initMasterConfingurationForDatabase(processEngineConfiguration);
			initMasterConfigurationForHistory(processEngineConfiguration);
			initMasterConfigurationForMailServer(processEngineConfiguration);
			initMasterConfigurationForAdvancedSettings(processEngineConfiguration);
			
		} else {
			logger.info("Master configuration disabled.");
			masterConfigurationState.setUsingMasterConfiguration(false);
			
			Boolean requiresMasterConfig = clusterConfigProperties.getMasterConfigurationRequired();
			if (requiresMasterConfig != null && requiresMasterConfig) {
				throw new ActivitiException("This Activiti instance is configured to only boot up when a valid master configuration is available.");
			}
			
		}
	}
	
	protected void fetchMasterConfigFromAdminApp() {
	  CloseableHttpClient client = adminAppService.getHttpClient();
		try {

			HttpGet get = new HttpGet(clusterConfigProperties.getAdminAppUrl() + "/api/enterprise/" + clusterConfigProperties.getClusterName() + "/master-config");
			CloseableHttpResponse response = client.execute(get);

			try {
				
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != 200) {
						logger.warn("Could not retrieve master configuration from Admin app. Received status code: " + statusCode);
						checkIfCanContinue(clusterConfigProperties.getMasterConfigurationRequired(), null);
						return;
				}
				
				InputStream is = response.getEntity().getContent();
				ObjectMapper objectMapper = new ObjectMapper();
				MasterConfigurationRepresentation masterConfigurationRepresentation = objectMapper.readValue(is, MasterConfigurationRepresentation.class);
				if (!(masterConfigurationRepresentation instanceof ProcessEngineMasterConfigurationRepresentation)) {
					logger.info("Got back a master configuration that is not for an Activiti Engine. Ignoring.");
				} else {
					this.masterConfiguration = (ProcessEngineMasterConfigurationRepresentation) masterConfigurationRepresentation;
				}

			} finally {
				response.close();
			}

		} catch (Exception e) {
			logger.warn("Error getting master configuration from Activiti Admin Application: " + e.getMessage());
			checkIfCanContinue(clusterConfigProperties.getMasterConfigurationRequired(), e);
		} finally {
			try {
				client.close();
			} catch (Exception e) {
				logger.warn("Could not close http client", e);
			}
		}
	}

	protected void checkIfCanContinue(Boolean masterConfigRequired, Exception e) {
		if (masterConfigRequired != null && masterConfigRequired) {
			String text = "Master configuration is required, but could not get it from Activiti Admin Application";
			if (e != null) {
				throw new ActivitiException(text, e);
			} else {
				throw new RuntimeException(text);
			}
		}
	}

	protected void initMasterConfingurationForDatabase(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		String dataSourceJndiName = masterConfiguration.getDataSourceJndiName();
		if (dataSourceJndiName != null) {
			
			processEngineConfiguration.setDataSourceJndiName(dataSourceJndiName);
			logger.info("Datasource jndi is changed to " + dataSourceJndiName);
			
		} else {

			String jdbcUrl = masterConfiguration.getJdbcUrl();
			if (jdbcUrl != null) {
				processEngineConfiguration.setJdbcUrl(jdbcUrl);
				logger.info("JDBC url is changed to " + jdbcUrl);
			}
	
			String jdbcDriver = masterConfiguration.getJdbcDriver();
			if (jdbcDriver != null) {
				processEngineConfiguration.setJdbcDriver(jdbcDriver);
				logger.info("JDBC driver is changed to " + jdbcDriver);
			}
	
			String jdbcUsername = masterConfiguration.getJdbcUsername();
			if (jdbcUsername != null) {
				processEngineConfiguration.setJdbcUsername(jdbcUsername);
				logger.info("JDBC user name is changed to " + jdbcUsername);
			}
	
			String jdbcPassword = masterConfiguration.getJdbcPassword();
			if (jdbcPassword != null) {
				processEngineConfiguration.setJdbcPassword(jdbcPassword);
				logger.info("JDBC password is changed (not shown for security)");
			}
			
			String databaseSchemaUpdate = masterConfiguration.getDatabaseSchemaUpdate();
			if (databaseSchemaUpdate != null) {
				processEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);
				logger.info("DatabaseSchemaUpdate is changed to " + databaseSchemaUpdate);
			}
			
			Integer jdbcMaxActiveConnections = masterConfiguration.getJdbcMaxActiveConnections();
			if (jdbcMaxActiveConnections != null) {
				processEngineConfiguration.setJdbcMaxActiveConnections(jdbcMaxActiveConnections);
				logger.info("Jdbc max active connections is changed to " + jdbcMaxActiveConnections);
			}
			
			Integer jdbcMaxIdleConnections = masterConfiguration.getJdbcMaxIdleConnections();
			if (jdbcMaxIdleConnections != null) {
				processEngineConfiguration.setJdbcMaxIdleConnections(jdbcMaxIdleConnections);
				logger.info("Jdbc max idle connections is changed to " + jdbcMaxIdleConnections);
			}
			
			Integer jdbcMaxCheckoutTime = masterConfiguration.getJdbcMaxCheckoutTime();
			if (jdbcMaxCheckoutTime != null) {
				processEngineConfiguration.setJdbcMaxCheckoutTime(jdbcMaxCheckoutTime);
				logger.info("Jdbc max checkout time is changed to " + jdbcMaxCheckoutTime);
			}
			
			Integer jdbcMaxWaitTime = masterConfiguration.getJdbcMaxWaitTime();
			if (jdbcMaxWaitTime != null) {
				processEngineConfiguration.setJdbcMaxWaitTime(jdbcMaxWaitTime);
				logger.info("Jdbc max wait time is changed to " + jdbcMaxWaitTime);
			}
			
		}
	}
	
	protected void initMasterConfigurationForHistory(ProcessEngineConfigurationImpl processEngineConfiguration) {
		String history = masterConfiguration.getHistory();
		if (history != null) {
			processEngineConfiguration.setHistory(history);
			logger.info("History is changed to " + history);
		}
	}
	
	protected void initMasterConfigurationPostInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

		// Here is the stuff from the master config that can only be done *after*
		// certain components have been initialized
		
		if (masterConfiguration != null && masterConfiguration.getEnableJobExecutor() != null && masterConfiguration.getEnableJobExecutor()) {
			initMasterConfigurationForJobExecutor(processEngineConfiguration);
		}
		
	}
	
	protected void initMasterConfigurationForJobExecutor(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
		
		if (asyncExecutor == null) {
			logger.info("No async executor found, no further configuration applied");
			return;
		}
		
		Integer maxJobsPerAcquisition = masterConfiguration.getMaxJobsPerAcquisition();
		if (maxJobsPerAcquisition != null) {
			asyncExecutor.setMaxAsyncJobsDuePerAcquisition(maxJobsPerAcquisition);
			asyncExecutor.setMaxTimerJobsPerAcquisition(maxJobsPerAcquisition);
			logger.info("Max jobs per aquisition changed to " + maxJobsPerAcquisition);
		}
		
		Integer jobWaitTime = masterConfiguration.getJobWaitTime();
		if (jobWaitTime != null) {
			asyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(jobWaitTime);
			asyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(jobWaitTime);
			logger.info("Job executor wait time changed to " + jobWaitTime);
		}
		
		Integer jobLockTime = masterConfiguration.getJobLockTime();
		if (jobLockTime != null) {
			asyncExecutor.setTimerLockTimeInMillis(jobLockTime);
			asyncExecutor.setAsyncJobLockTimeInMillis(jobLockTime);
			logger.info("Job executor lock time changed to " + jobLockTime);
		}
		
		if (asyncExecutor instanceof DefaultAsyncJobExecutor) {
			
			DefaultAsyncJobExecutor defaultJobExecutor = (DefaultAsyncJobExecutor) asyncExecutor;
			
			Integer jobQueueSize = masterConfiguration.getJobQueueSize();
			if (jobQueueSize != null) {
				defaultJobExecutor.setQueueSize(jobQueueSize);
				logger.info("Job executor queue size changed to " + jobQueueSize);
			}
			
			Integer jobCorePoolSize = masterConfiguration.getJobCorePoolSize();
			if (jobCorePoolSize != null) {
				defaultJobExecutor.setCorePoolSize(jobCorePoolSize);
				logger.info("Job executor core pool size changed to " + jobCorePoolSize);
			}
			
			Integer jobMaxPoolSize = masterConfiguration.getJobMaxPoolSize();
			if (jobMaxPoolSize != null) {
				defaultJobExecutor.setMaxPoolSize(jobMaxPoolSize);
				logger.info("Job executor max pool size changed to " + jobMaxPoolSize);
			}
			
		}
		
		
	}
	
	protected void initMasterConfigurationForMailServer(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		String mailServerHost = masterConfiguration.getMailServerHost();
		if (mailServerHost != null) {
			processEngineConfiguration.setMailServerHost(mailServerHost);
			logger.info("Mail server host is changed to " + mailServerHost);
		}
		
		String mailServerUsername = masterConfiguration.getMailServerUsername();
		if (mailServerUsername != null) {
			processEngineConfiguration.setMailServerUsername(mailServerUsername);
			logger.info("Mail server user name is changed to " + mailServerHost);
		}
		
		String mailServerPassword = masterConfiguration.getMailServerPassword();
		if (mailServerPassword != null) {
			processEngineConfiguration.setMailServerPassword(mailServerPassword);
			logger.info("Mail server user password is changed (not shown for security)");
		}
		
		Integer mailServerPort = masterConfiguration.getMailServerPort();
		if (mailServerPort != null) {
			processEngineConfiguration.setMailServerPort(mailServerPort);
			logger.info("Mail server port is changed to " + mailServerPort);
		}
		
		Boolean mailServerUseSsl = masterConfiguration.getMailServerUseSsl();
		if (mailServerUseSsl != null) {
			processEngineConfiguration.setMailServerUseSSL(mailServerUseSsl);
			logger.info("Mail server useSsl is changed to " + mailServerUseSsl);
		}
		
		Boolean mailServerUseTls = Boolean.valueOf(masterConfiguration.getMailServerUseTls());
		if (mailServerUseTls != null) {
			processEngineConfiguration.setMailServerUseTLS(mailServerUseTls);
			logger.info("Mail server useTls is changed to " + mailServerUseTls);
		}
		
		String mailServerDefaultFrom = masterConfiguration.getMailServerDefaultFrom();
		if (mailServerDefaultFrom != null) {
			processEngineConfiguration.setMailServerDefaultFrom(mailServerDefaultFrom);
			logger.info("Mail server default from is changed to " + mailServerDefaultFrom);
		}
		
		String mailServerJndi = masterConfiguration.getMailServerJndi();
		if (mailServerJndi != null) {
			processEngineConfiguration.setMailSessionJndi(mailServerJndi);
			logger.info("Mail server session jndi is changed to " + mailServerJndi);
		}
		
	}
	
	protected void initMasterConfigurationForAdvancedSettings(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		Integer idBlockSize = masterConfiguration.getIdBlockSize();
		if (idBlockSize != null) {
			processEngineConfiguration.setIdBlockSize(idBlockSize);
			logger.info("Id block size is changed to " + idBlockSize);
		}
		
		Integer processDefinitionCacheLimit = masterConfiguration.getProcessDefinitionCacheLimit();
		if (processDefinitionCacheLimit != null) {
			processEngineConfiguration.setProcessDefinitionCacheLimit(processDefinitionCacheLimit);
			logger.info("Process definition cache limit is changed to " + processDefinitionCacheLimit);
		}
		
	}
	
	protected void initWrappedAsyncExecutor(ProcessEngineConfigurationImpl processEngineConfiguration) {
		AsyncExecutor originalAsyncExecutor = processEngineConfiguration.getAsyncExecutor();
		if (originalAsyncExecutor != null) {
			this.wrappedAsyncExecutor = new WrappedAsyncExecutor(originalAsyncExecutor);
			processEngineConfiguration.setAsyncExecutor(wrappedAsyncExecutor);
		}
	}
	
	protected void initCustomCommandInterceptor(ProcessEngineConfigurationImpl processEngineConfiguration) {
		List<CommandInterceptor> commandInterceptors = processEngineConfiguration.getCustomPreCommandInterceptors();
		if (commandInterceptors == null) {
			commandInterceptors = new ArrayList<CommandInterceptor>();
		}
		
		gatherMetricsCommandInterceptor = new GatherMetricsCommandInterceptor(processEngineConfiguration.getClock());
		commandInterceptors.add(0, gatherMetricsCommandInterceptor);
		processEngineConfiguration.setCustomPreCommandInterceptors(commandInterceptors);
	}
	
	protected void initJobExecutor(ProcessEngineConfigurationImpl processEngineConfiguration) {
		if (masterConfiguration != null && masterConfiguration.getEnableJobExecutor() != null && masterConfiguration.getEnableJobExecutor()) {
			processEngineConfiguration.setAsyncExecutorActivate(true);
		}
	}
	
	protected void initJvmMetricsManager() {
		jvmMetricsManager = new JvmMetricsManager();
	}
	
	protected void initSendEventsThread(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		final SendEventsRunnable sendEventsRunnable = new SendEventsRunnable(uniqueNodeId, ipAddress,
				processEngineConfiguration.getClock(), adminAppService);
		
		sendEventsRunnable.setJvmMetricsManager(jvmMetricsManager);
		sendEventsRunnable.setClusterEnabledProcessEngineLifeCycleListener(clusterEnabledProcessEngineLifecycleListener);
		sendEventsRunnable.setWrappedProcessDefinitionCache(wrappedProcessDefinitionCache);
		sendEventsRunnable.setGatherMetricsCommandInterceptor(gatherMetricsCommandInterceptor);
		sendEventsRunnable.setGatherMetricsFailedJobCommandFactory(gatherMetricsFailedJobCommandFactory);
		sendEventsRunnable.setMasterConfigurationState(masterConfigurationState);
		sendEventsRunnable.setWrappedAsyncExecutor(wrappedAsyncExecutor);
		
		Integer metricSendingInterval = clusterConfigProperties.getMetricSendingInterval();
		if (metricSendingInterval == null) {
		  metricSendingInterval = 30;
			logger.info("No metric sending interval configured, defaulting to " + metricSendingInterval + " seconds");
		}

		final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(sendEventsRunnable, 10, metricSendingInterval, TimeUnit.SECONDS);
		
		// Add a closing hook as process engine listener
		addCleanupExecutorHookOnShutdown(processEngineConfiguration,
        sendEventsRunnable, executorService);

	}

	private void addCleanupExecutorHookOnShutdown(
      ProcessEngineConfigurationImpl processEngineConfiguration,
      final SendEventsRunnable sendEventsRunnable,
      final ScheduledExecutorService executorService) {
		
	  final ProcessEngineLifecycleListener originalProcessEngineLifecycleListener =
				processEngineConfiguration.getProcessEngineLifecycleListener();
		processEngineConfiguration.setProcessEngineLifecycleListener(new ProcessEngineLifecycleListener() {
			
			@Override
			public void onProcessEngineBuilt(ProcessEngine processEngine) {
				if (originalProcessEngineLifecycleListener != null) {
					originalProcessEngineLifecycleListener.onProcessEngineBuilt(processEngine);
				}
			}
			
			@Override
			public void onProcessEngineClosed(ProcessEngine processEngine) {
				
				// First call the original lifecycle listeners. Sending the event with the latest state possible.
				if (originalProcessEngineLifecycleListener != null) {
					originalProcessEngineLifecycleListener.onProcessEngineClosed(processEngine);
				}
				
				sendEventsRunnable.run(); // One last time sending the events

				try {
					logger.info("Shutting down threadpool used to send metrics...");
					executorService.shutdown();
					try {
						executorService.awaitTermination(30, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						logger.warn("Waited for 30 seconds for threadpool (used for sending metrics) shutdown, but was interrupted", e);
					}
				} catch (Exception e) {
					logger.warn("Could not properly shut down executor service for SendRunnable thread", e);
				}
				
				
			}
			
		});
		
  }

	public int getPriority() {
		return PRIORITY;
	}
	
	/* Member getters and setters */

	public AdminAppService getAdminAppService() {
		return adminAppService;
	}

	public void setAdminAppService(AdminAppService adminAppService) {
		this.adminAppService = adminAppService;
	}

	public ClusterConfigProperties getClusterConfigProperties() {
		return clusterConfigProperties;
	}

	public void setClusterConfigProperties(
	    ClusterConfigProperties clusterConfigProperties) {
		this.clusterConfigProperties = clusterConfigProperties;
	}

	public String getUniqueNodeId() {
		return uniqueNodeId;
	}

	public void setUniqueNodeId(String uniqueNodeId) {
		this.uniqueNodeId = uniqueNodeId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getLocalNodeHost() {
		return localNodeHost;
	}

	public void setLocalNodeHost(String localNodeHost) {
		this.localNodeHost = localNodeHost;
	}

	public Integer getLocalNodePort() {
		return localNodePort;
	}

	public void setLocalNodePort(Integer localNodePort) {
		this.localNodePort = localNodePort;
	}

	public MasterConfigurationState getMasterConfigurationState() {
		return masterConfigurationState;
	}

	public void setMasterConfigurationState(
	    MasterConfigurationState masterConfigurationState) {
		this.masterConfigurationState = masterConfigurationState;
	}

	public WrappedAsyncExecutor getWrappedAsyncExecutor() {
		return wrappedAsyncExecutor;
	}

	public void setWrappedAsyncExecutor(WrappedAsyncExecutor wrappedAsyncExecutor) {
		this.wrappedAsyncExecutor = wrappedAsyncExecutor;
	}

	public JvmMetricsManager getJvmMetricsManager() {
		return jvmMetricsManager;
	}

	public void setJvmMetricsManager(JvmMetricsManager jvmMetricsManager) {
		this.jvmMetricsManager = jvmMetricsManager;
	}

	public ClusterEnabledProcessEngineLifeCycleListener getClusterEnabledProcessEngineLifecycleListener() {
		return clusterEnabledProcessEngineLifecycleListener;
	}

	public void setClusterEnabledProcessEngineLifecycleListener(
	    ClusterEnabledProcessEngineLifeCycleListener clusterEnabledProcessEngineLifecycleListener) {
		this.clusterEnabledProcessEngineLifecycleListener = clusterEnabledProcessEngineLifecycleListener;
	}

	public ProcessDefinitionCacheMetricsWrapper getWrappedProcessDefinitionCache() {
		return wrappedProcessDefinitionCache;
	}

	public void setWrappedProcessDefinitionCache(
	    ProcessDefinitionCacheMetricsWrapper wrappedProcessDefinitionCache) {
		this.wrappedProcessDefinitionCache = wrappedProcessDefinitionCache;
	}

	public GatherMetricsCommandInterceptor getGatherMetricsCommandInterceptor() {
		return gatherMetricsCommandInterceptor;
	}

	public void setGatherMetricsCommandInterceptor(
	    GatherMetricsCommandInterceptor gatherMetricsCommandInterceptor) {
		this.gatherMetricsCommandInterceptor = gatherMetricsCommandInterceptor;
	}

	public GatherMetricsFailedJobCommandFactory getGatherMetricsFailedJobCommandFactory() {
		return gatherMetricsFailedJobCommandFactory;
	}

	public void setGatherMetricsFailedJobCommandFactory(
	    GatherMetricsFailedJobCommandFactory gatherMetricsFailedJobCommandFactory) {
		this.gatherMetricsFailedJobCommandFactory = gatherMetricsFailedJobCommandFactory;
	}
	

}
