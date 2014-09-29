package com.activiti.addon.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.activiti.engine.ProcessEngineLifecycleListener;
import org.activiti.engine.cfg.AbstractProcessEngineConfigurator;
import org.activiti.engine.cfg.ProcessEngineConfigurator;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.interceptor.CommandInterceptor;
import org.activiti.engine.impl.jobexecutor.DefaultJobExecutor;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.jobexecutor.WrappedJobExecutor;
import org.activiti.engine.impl.persistence.deploy.DeploymentCache;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.activiti.addon.cluster.cache.ProcessDefinitionCacheMetricsWrapper;
import com.activiti.addon.cluster.interceptor.GatherMetricsCommandInterceptor;
import com.activiti.addon.cluster.jobexecutor.GatherMetricsFailedJobCommandFactory;
import com.activiti.addon.cluster.jobexecutor.GatherMetricsRejectedJobsHandler;
import com.activiti.addon.cluster.jobexecutor.JobMetricsManager;
import com.activiti.addon.cluster.lifecycle.ClusterEnabledProcessEngineLifeCycleListener;
import com.activiti.addon.cluster.metrics.JvmMetricsManager;
import com.activiti.addon.cluster.state.AdminAppState;
import com.activiti.addon.cluster.state.MasterConfigurationState;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SymmetricEncryptionConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

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

	private static final String ADMIN_APP_EVENT_TOPIC = "admin-app-event-topic";

	private static final String EVENT_QUEUE = "event-queue";
	
	private static final String MASTER_CFG_DISTRIBUTED_MAP = "masterCfg";
	
	private static final String MASTER_CFG_ID = "masterConfigId";

	private static final String MASTER_CFG_ENABLED = "masterConfigEnabled";

	private static final String MASTER_CFG_JDBC_URL = "masterConfigJdbcUrl";

	private static final String MASTER_CFG_JDBC_DRIVER = "masterConfigJdbcDriver";

	private static final String MASTER_CFG_JDBC_USERNAME = "masterConfigJdbcUsername";

	private static final String MASTER_CFG_JDBC_PASSWORD = "masterConfigJdbcPassword";
	
	private static final String MASTER_CFG_JDBC_MAX_ACTIVE_CONNECTIONS = "masterConfigMaxActiveConnections";
	
	private static final String MASTER_CFG_JDBC_MAX_IDLE_CONNECTIONS = "masterConfigMaxIdleConnections";
	
	private static final String MASTER_CFG_JDBC_MAX_CHECKOUT_TIME = "masterConfigMaxCheckoutTime";
	
	private static final String MASTER_CFG_JDBC_MAX_WAIT_TIME = "masterConfigMaxWaitTime";
	
	private static final String MASTER_CFG_DATASOURCE_JNDI = "masterConfigDatasourceJndi";

	private static final String MASTER_CFG_HISTORY = "masterConfigHistory";

	private static final String MASTER_CFG_DB_SCHEMA_UPDATE = "masterConfigDatabaseSchemaUpdate";
	
	private static final String MASTER_CFG_MAIL_SERVER_HOST = "masterConfigMailServerHost";
	
	private static final String MASTER_CFG_MAIL_SERVER_USER_NAME = "masterConfigMailServerUsername";
	
	private static final String MASTER_CFG_MAIL_SERVER_PASSWORD = "masterConfigMailServerPassword";
	
	private static final String MASTER_CFG_MAIL_SERVER_PORT = "masterConfigMailServerPort";
	
	private static final String MASTER_CFG_MAIL_SERVER_USE_SSL = "masterConfigMailServerUseSsl";
	
	private static final String MASTER_CFG_MAIL_SERVER_USE_TLS = "masterConfigMailServerUseTls";
	
	private static final String MASTER_CFG_MAIL_SERVER_DEFAULT_FROM = "masterConfigMailServerDefaultFrom";
	
	private static final String MASTER_CFG_MAIL_SERVER_JNDI = "masterConfigMailServerJndi";
	
	private static final String MASTER_CFG_ID_BLOCK_SIZE = "masterConfigIdBlockSize";
	
	private static final String MASTER_CFG_PROC_DEF_CACHE_LIMIT = "masterConfigProcessDefinitionCacheLimit";
	
	private static final String MASTER_CFG_MAX_JOBS_PER_ACQUISITION = "masterConfigMaxJobsPerAcquisition";
	
	private static final String MASTER_CFG_JOB_WAIT_TIME = "masterConfigJobWaitTime";
	
	private static final String MASTER_CFG_JOB_LOCK_TIME = "masterConfigJobLockTime";
	
	private static final String MASTER_CFG_JOB_MAX_POOL_SIZE = "masterConfigJobMaxPoolSize";
	
	private static final String MASTER_CFG_JOB_CORE_POOL_SIZE = "masterConfigJobCorePoolSize";
	
	private static final String MASTER_CFG_JOB_QUEUE_SIZE = "masterConfigJobQueueSize";
	
	
	/*
	 * Instance members 
	 */
	
	protected ClusterConfigProperties clusterConfigProperties;
	
	protected String uniqueNodeId;
	
	protected String localNodeHost;
	
	protected Integer localNodePort;
	
	protected AdminAppState adminAppState;
	
	protected MasterConfigurationState masterConfigurationState;
	
	protected WrappedJobExecutor wrappedJobExecutor;
	
	protected JvmMetricsManager jvmMetricsManager;
	
	protected ProcessDefinitionCacheMetricsWrapper wrappedProcessDefinitionCache;
	
	protected GatherMetricsCommandInterceptor gatherMetricsCommandInterceptor;
	
	protected GatherMetricsRejectedJobsHandler gatherMetricsRejectedJobsHandler;
	
	protected GatherMetricsFailedJobCommandFactory gatherMetricsFailedJobCommandFactory;
	
	protected JobMetricsManager jobMetricsManager;

	/*
	 * Distributed Hazelcast instances
	 */

	protected HazelcastInstance hazelcastInstance;

	/** A distributed topic where events will be published on */
	protected ITopic<Map<String, Object>> eventTopic;

	/** A distributed queue for sending information to the admin app */
	protected BlockingQueue<Map<String, Object>> eventQueue;
	
	/** A distributed map for the shared 'master' configuration */
	protected IMap<String, Object> masterConfigurationMap;
	
	public ActivitiClusterConfigurator() {
	}
	
	public ActivitiClusterConfigurator(ClusterConfigProperties clusterConfigProperties) {
	  this.clusterConfigProperties = clusterConfigProperties;
	}

	
	/*
	 * The logic 
	 */
	
	public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
	  
	  if (processEngineConfiguration.getLicenseHolder().isDepartemental()) {
	    logger.info("Departemental license found: cluster configuration disabled");
	    return;
	  }

		logger.info(NAME + " : initializing ...");
		
		// Read properties from classpath
		initProperties();
		
		// Fire up Hazelcast with the default config
		initHazelcast();
		uniqueNodeId = hazelcastInstance.getCluster().getLocalMember().getInetSocketAddress().toString();
		localNodeHost = hazelcastInstance.getCluster().getLocalMember().getInetSocketAddress().getHostName();
		localNodePort = hazelcastInstance.getCluster().getLocalMember().getInetSocketAddress().getPort();
		
		// Initialize admin app state object
		adminAppState = new AdminAppState();
		
		// Master config support
		initMasterConfigurationPreInit(processEngineConfiguration);

		// Support for publishing events
		initEventQueue(processEngineConfiguration);
		initAdminAppEventTopic();
		
		// Add a process engine lifecycle listener to send the event when the engine is ready/closing down
		initProcessEngineLifeCycleListener(processEngineConfiguration);
		
		// Add custom command interceptor (keeps metrics for command execution)
		initCustomCommandInterceptor(processEngineConfiguration);
	}
	
	protected void initProperties() {
	  if (clusterConfigProperties == null) {
	    clusterConfigProperties = new ClusterConfigProperties();
	  }
	}
	
	protected void initHazelcast() {
		Config config = new Config();
		
		// Group config
		String clusterName = clusterConfigProperties.getClusterName();
		if (clusterName == null) {
			logger.info("No cluster name defined. Default to 'dev'");
			clusterName = "dev-" + System.getProperty("user.name");
		}
		
		String clusterPassword = clusterConfigProperties.getClusterPassword();
		if (clusterPassword == null) {
			logger.info("No cluster password defined. Default to default one");
			clusterPassword = "dev-pass";
		}
		
		config.setGroupConfig(new GroupConfig(clusterName, clusterPassword));
		
		// Network config
		NetworkConfig networkConfig = new NetworkConfig();
		config.setNetworkConfig(networkConfig);
		
		JoinConfig joinConfig = new JoinConfig();
		networkConfig.setJoin(joinConfig);
		
		Boolean multiCastEnabled = clusterConfigProperties.isNetworkMulticastEnabled();
		Boolean tcpIpEnabled = clusterConfigProperties.isNetworkTcpEnabled();
		
		if (!multiCastEnabled && !tcpIpEnabled) {
			logger.info("No correct configuration found for network multicast/tcpIp (both are 'false') : defaulting to multicast");
			multiCastEnabled = true;
		}
		if (multiCastEnabled && tcpIpEnabled) {
			logger.info("No correct configuration found for network multicast/tcpIp (both are 'true'): defaulting to multicast");
			multiCastEnabled = true;
			tcpIpEnabled = false;
		}
		
		MulticastConfig multicastConfig = new MulticastConfig();
		multicastConfig.setEnabled(false);
		joinConfig.setMulticastConfig(multicastConfig);
		
		TcpIpConfig tcpIpConfig = new TcpIpConfig();
		joinConfig.setTcpIpConfig(tcpIpConfig);
		
		if (multiCastEnabled) {
		  
		  String startingPortString = clusterConfigProperties.getNetworkStartingPort();
	    Integer startingPort = 5701;
	    if (startingPortString != null) {
	      startingPort = Integer.valueOf(startingPortString);
	    } else {
	      logger.info("No starting port set, using default one (5701");
	    }
	    networkConfig.setPort(startingPort);
	    networkConfig.setPortAutoIncrement(true);
			
			multicastConfig.setEnabled(true);
			multicastConfig.setMulticastTimeoutSeconds(30);
			
			String multiCastGroup = clusterConfigProperties.getNetworkMulticastGroup();
			if (multiCastGroup == null) {
			  multiCastGroup = "224.2.2.3";
				logger.info("Multicast is enabled, but no group set. Defaulting to " + multiCastGroup);
			}
			multicastConfig.setMulticastGroup(multiCastGroup);
			
			String multiCastPortString = clusterConfigProperties.getNetworkMulticastPort();
			Integer multiCastPort = 54327;
			if (multiCastPortString != null) {
				multiCastPort = Integer.valueOf(multiCastPortString);
			} else {
				logger.info("Multicast is enabled, but no group set. Defaulting to " + multiCastPort);
			}
			multicastConfig.setMulticastPort(multiCastPort);
			
		} else if  (tcpIpEnabled) {
		  
		  networkConfig.setPort(clusterConfigProperties.getNetworkTcpPort());
      networkConfig.setPortAutoIncrement(false);
		  
			tcpIpConfig.setEnabled(true);
			tcpIpConfig.setConnectionTimeoutSeconds(30);
			String tcpInterfaces = clusterConfigProperties.getNetworkTcpInterfaces();
			if (tcpInterfaces != null) {
				tcpIpConfig.setMembers(Arrays.asList(tcpInterfaces));
			} else {
				logger.warn("Tcp ip is enabled, but no interfaces provided!");
			}
			
		}
		
		// Security
		if (clusterConfigProperties.isSecurityEnabled()) {
			SymmetricEncryptionConfig symmetricEncryptionConfig = new SymmetricEncryptionConfig();
			symmetricEncryptionConfig.setEnabled(true);
			symmetricEncryptionConfig.setAlgorithm("PBEWithMD5AndDES");
			
			String securityPassword = clusterConfigProperties.getSecurityPassword();
			if (securityPassword != null) {
				symmetricEncryptionConfig.setPassword(securityPassword);
			} else {
				logger.warn("Security is enabled, but not password set!");
			}
			String securitySalt = clusterConfigProperties.getSecuritySalt();
			if (securitySalt != null) {
				symmetricEncryptionConfig.setSalt(securitySalt);
			} else {
				logger.warn("Security is enabled, but no salt set!");
			}
			Integer securityIterationCount = 19;
			String securityIterationCountString = clusterConfigProperties.getSecurityIterationCount();
			if (securityIterationCountString != null) {
				securityIterationCount = Integer.valueOf(securityIterationCountString);
			}
			symmetricEncryptionConfig.setIterationCount(securityIterationCount);
			
			networkConfig.setSymmetricEncryptionConfig(symmetricEncryptionConfig);
		}
		
		logger.info("Hazelcast config: " + config);
		this.hazelcastInstance = Hazelcast.newHazelcastInstance(config);
	}
	
	public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
	  
	  if (processEngineConfiguration.getLicenseHolder().isDepartemental()) {
      logger.info("Departemental license found: cluster configuration disabled");
      return;
    }
		
		// Process definitions cache
		initProcessDefinitionCache(processEngineConfiguration);
		
		// Job rejection and failed
		initJobRejectionHandler(processEngineConfiguration);
		initFailedJobCommandFactory(processEngineConfiguration);
		
		// Job Metrics manager
		initJobMetricsManager();
		
		// Master config support
		initMasterConfigurationPostInit(processEngineConfiguration);
		
		// Job executor state
		initWrappedJobExecutor(processEngineConfiguration);
		
		// Fire up the JVM metrics gathering
		// HAS to be at the end here, has dependencies on all the other stuff
		initSendEventsThread();
		
		logger.info(NAME + " : initialization completed.");
	}

	protected void initProcessDefinitionCache(ProcessEngineConfigurationImpl processEngineConfiguration) {
		DeploymentCache<ProcessDefinitionEntity> processDefinitionCache = processEngineConfiguration.getProcessDefinitionCache();
		wrappedProcessDefinitionCache = new ProcessDefinitionCacheMetricsWrapper(processDefinitionCache);
		
		processEngineConfiguration.setProcessDefinitionCache(wrappedProcessDefinitionCache);
		processEngineConfiguration.getDeploymentManager().setProcessDefinitionCache(wrappedProcessDefinitionCache);
	}
	
	protected void initJobRejectionHandler(ProcessEngineConfigurationImpl processEngineConfiguration) {
		gatherMetricsRejectedJobsHandler = new GatherMetricsRejectedJobsHandler(
				processEngineConfiguration.getJobExecutor().getRejectedJobsHandler());
		processEngineConfiguration.getJobExecutor().setRejectedJobsHandler(gatherMetricsRejectedJobsHandler);
	}
	
	protected void initFailedJobCommandFactory(ProcessEngineConfigurationImpl processEngineConfiguration) {
		gatherMetricsFailedJobCommandFactory = new GatherMetricsFailedJobCommandFactory(processEngineConfiguration.getFailedJobCommandFactory());
		processEngineConfiguration.setFailedJobCommandFactory(gatherMetricsFailedJobCommandFactory);
	}
	
	protected void initJobMetricsManager() {
		jobMetricsManager = new JobMetricsManager(gatherMetricsRejectedJobsHandler, gatherMetricsFailedJobCommandFactory);
	}

	protected void initEventQueue(ProcessEngineConfigurationImpl processEngineConfiguration) {
		eventQueue = hazelcastInstance.getQueue(EVENT_QUEUE);
	}

	private void initProcessEngineLifeCycleListener(ProcessEngineConfigurationImpl processEngineConfiguration) {
		ProcessEngineLifecycleListener listener = new ClusterEnabledProcessEngineLifeCycleListener(
				uniqueNodeId, eventQueue, processEngineConfiguration.getProcessEngineLifecycleListener());
		processEngineConfiguration.setProcessEngineLifecycleListener(listener);
	}

	protected void initAdminAppEventTopic() {
		eventTopic = hazelcastInstance.getTopic(ADMIN_APP_EVENT_TOPIC);
		
		eventTopic.addMessageListener(new MessageListener<Map<String, Object>>() {
			
			public void onMessage(Message<Map<String, Object>> message) {
				Map<String, Object> event = message.getMessageObject();
				String type = (String) event.get("type");
				if (type != null) {
					if (type.equals("i-am-alive")) {
						adminAppState.iAmAliveEventReceived();
					} else if (type.equals("start-job-executor")) {
						String host = (String) event.get("host");
						Integer port = (Integer) event.get("port");
						
						if (host != null && port != null
								&& host.equals(localNodeHost) && port.equals(localNodePort)) {
							wrappedJobExecutor.start();
						}
						
					} else {
						logger.warn("Unknown event of type '" + type + "' received on event topic");
					}
				} else {
					logger.warn("Received event without a type on event topic");
				}
			}
			
		});
	}
	
	protected void initMasterConfigurationPreInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		masterConfigurationMap = hazelcastInstance.getMap(MASTER_CFG_DISTRIBUTED_MAP);
		masterConfigurationState = new MasterConfigurationState();
		
		Boolean configEnabled = (Boolean) masterConfigurationMap.get(MASTER_CFG_ENABLED);
		if (configEnabled != null && configEnabled) {
			
			logger.info("Master configuration enabled. Changing local configuration to master configuration.");
			
			masterConfigurationState.setUsingMasterConfiguration(true);
			masterConfigurationState.setConfigurationId((String) masterConfigurationMap.get(MASTER_CFG_ID));

			initMasterConfingurationForDatabase(processEngineConfiguration);
			initMasterConfigurationForHistory(processEngineConfiguration);
			initMasterConfigurationForMailServer(processEngineConfiguration);
			initMasterConfigurationForAdvancedSettings(processEngineConfiguration);
			
		} else {
			logger.info("Master configuration disabled.");
			masterConfigurationState.setUsingMasterConfiguration(false);
			
			String requiresMasterConfigString = clusterConfigProperties.getMasterConfigurationRequired();
			if (requiresMasterConfigString != null) {
				Boolean requiresMasterConfig = Boolean.valueOf(requiresMasterConfigString);
				if (requiresMasterConfig) {
					throw new RuntimeException("This Activiti instance is configured to only boot up when a valid master configuration is available.");
				}
			}
			
		}
	}

	protected void initMasterConfingurationForDatabase(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		String dataSourceJndiName = (String) masterConfigurationMap.get(MASTER_CFG_DATASOURCE_JNDI);
		if (dataSourceJndiName != null) {
			
			processEngineConfiguration.setDataSourceJndiName(dataSourceJndiName);
			logger.info("Datasource jndi is changed to " + dataSourceJndiName);
			
		} else {

			String jdbcUrl = (String) masterConfigurationMap.get(MASTER_CFG_JDBC_URL);
			if (jdbcUrl != null) {
				processEngineConfiguration.setJdbcUrl(jdbcUrl);
				logger.info("JDBC url is changed to " + jdbcUrl);
			}
	
			String jdbcDriver = (String) masterConfigurationMap.get(MASTER_CFG_JDBC_DRIVER);
			if (jdbcDriver != null) {
				processEngineConfiguration.setJdbcDriver(jdbcDriver);
				logger.info("JDBC driver is changed to " + jdbcDriver);
			}
	
			String jdbcUsername = (String) masterConfigurationMap.get(MASTER_CFG_JDBC_USERNAME);
			if (jdbcUsername != null) {
				processEngineConfiguration.setJdbcUsername(jdbcUsername);
				logger.info("JDBC user name is changed to " + jdbcUsername);
			}
	
			String jdbcPassword = (String) masterConfigurationMap.get(MASTER_CFG_JDBC_PASSWORD);
			if (jdbcPassword != null) {
				processEngineConfiguration.setJdbcPassword(jdbcPassword);
				logger.info("JDBC password is changed (not shown for security)");
			}
			
			String databaseSchemaUpdate = (String) masterConfigurationMap.get(MASTER_CFG_DB_SCHEMA_UPDATE);
			if (databaseSchemaUpdate != null) {
				processEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);
				logger.info("DatabaseSchemaUpdate is changed to " + databaseSchemaUpdate);
			}
			
			Integer jdbcMaxActiveConnections = (Integer) masterConfigurationMap.get(MASTER_CFG_JDBC_MAX_ACTIVE_CONNECTIONS);
			if (jdbcMaxActiveConnections != null) {
				processEngineConfiguration.setJdbcMaxActiveConnections(jdbcMaxActiveConnections);
				logger.info("Jdbc max active connections is changed to " + jdbcMaxActiveConnections);
			}
			
			Integer jdbcMaxIdleConnections = (Integer) masterConfigurationMap.get(MASTER_CFG_JDBC_MAX_IDLE_CONNECTIONS);
			if (jdbcMaxIdleConnections != null) {
				processEngineConfiguration.setJdbcMaxIdleConnections(jdbcMaxIdleConnections);
				logger.info("Jdbc max idle connections is changed to " + jdbcMaxIdleConnections);
			}
			
			Integer jdbcMaxCheckoutTime = (Integer) masterConfigurationMap.get(MASTER_CFG_JDBC_MAX_CHECKOUT_TIME);
			if (jdbcMaxCheckoutTime != null) {
				processEngineConfiguration.setJdbcMaxCheckoutTime(jdbcMaxCheckoutTime);
				logger.info("Jdbc max checkout time is changed to " + jdbcMaxCheckoutTime);
			}
			
			Integer jdbcMaxWaitTime = (Integer) masterConfigurationMap.get(MASTER_CFG_JDBC_MAX_WAIT_TIME);
			if (jdbcMaxWaitTime != null) {
				processEngineConfiguration.setJdbcMaxWaitTime(jdbcMaxWaitTime);
				logger.info("Jdbc max wait time is changed to " + jdbcMaxWaitTime);
			}
			
		}
	}
	
	protected void initWrappedJobExecutor(ProcessEngineConfigurationImpl processEngineConfiguration) {
		JobExecutor originalJobExecutor = processEngineConfiguration.getJobExecutor();
		if (originalJobExecutor != null) {
			this.wrappedJobExecutor = new WrappedJobExecutor(originalJobExecutor);
			processEngineConfiguration.setJobExecutor(wrappedJobExecutor);
			processEngineConfiguration.setJobExecutorActivate(false); // Don't start automatically, the admin app will tell when to start one
		}
	}
	
	protected void initMasterConfigurationForHistory(ProcessEngineConfigurationImpl processEngineConfiguration) {
		String history = (String) masterConfigurationMap.get(MASTER_CFG_HISTORY);
		if (history != null) {
			processEngineConfiguration.setHistory(history);
			logger.info("History is changed to " + history);
		}
	}
	
	protected void initMasterConfigurationPostInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

		// Here is the stuff from the master config that can only be done *after*
		// certain components have been initialized
		
		Boolean configEnabled = (Boolean) masterConfigurationMap.get(MASTER_CFG_ENABLED);
		if (configEnabled != null && configEnabled) {
			initMasterConfigurationForJobExecutor(processEngineConfiguration);
		}
		
		initMasterConfigurationChangeListener();
	}
	
	protected void initMasterConfigurationForJobExecutor(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
		if (jobExecutor == null) {
			logger.info("No job executor found, no further configuration applied");
			return;
		}
		
		Integer maxJobsPerAcquisition = (Integer) masterConfigurationMap.get(MASTER_CFG_MAX_JOBS_PER_ACQUISITION);
		if (maxJobsPerAcquisition != null) {
			jobExecutor.setMaxJobsPerAcquisition(maxJobsPerAcquisition);
			logger.info("Max jobs per aquisition changed to " + maxJobsPerAcquisition);
		}
		
		Integer jobWaitTime = (Integer) masterConfigurationMap.get(MASTER_CFG_JOB_WAIT_TIME);
		if (jobWaitTime != null) {
			jobExecutor.setWaitTimeInMillis(jobWaitTime);
			logger.info("Job executor wait time changed to " + jobWaitTime);
		}
		
		Integer jobLockTime = (Integer) masterConfigurationMap.get(MASTER_CFG_JOB_LOCK_TIME);
		if (jobLockTime != null) {
			jobExecutor.setLockTimeInMillis(jobLockTime);
			logger.info("Job executor lock time changed to " + jobLockTime);
		}
		
		if (jobExecutor instanceof DefaultJobExecutor) {
			
			DefaultJobExecutor defaultJobExecutor = (DefaultJobExecutor) jobExecutor;
		
			Integer jobQueueSize = (Integer) masterConfigurationMap.get(MASTER_CFG_JOB_QUEUE_SIZE);
			if (jobQueueSize != null) {
				defaultJobExecutor.setQueueSize(jobQueueSize);
				logger.info("Job executor queue size changed to " + jobQueueSize);
			}
			
			Integer jobCorePoolSize = (Integer) masterConfigurationMap.get(MASTER_CFG_JOB_CORE_POOL_SIZE);
			if (jobCorePoolSize != null) {
				defaultJobExecutor.setCorePoolSize(jobCorePoolSize);
				logger.info("Job executor core pool size changed to " + jobCorePoolSize);
			}
			
			Integer jobMaxPoolSize = (Integer) masterConfigurationMap.get(MASTER_CFG_JOB_MAX_POOL_SIZE);
			if (jobMaxPoolSize != null) {
				defaultJobExecutor.setMaxPoolSize(jobMaxPoolSize);
				logger.info("Job executor max pool size changed to " + jobMaxPoolSize);
			}
			
		}
		
		
	}
	
	protected void initMasterConfigurationForMailServer(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		String mailServerHost = (String) masterConfigurationMap.get(MASTER_CFG_MAIL_SERVER_HOST);
		if (mailServerHost != null) {
			processEngineConfiguration.setMailServerHost(mailServerHost);
			logger.info("Mail server host is changed to " + mailServerHost);
		}
		
		String mailServerUsername = (String) masterConfigurationMap.get(MASTER_CFG_MAIL_SERVER_USER_NAME);
		if (mailServerUsername != null) {
			processEngineConfiguration.setMailServerUsername(mailServerUsername);
			logger.info("Mail server user name is changed to " + mailServerHost);
		}
		
		String mailServerPassword = (String) masterConfigurationMap.get(MASTER_CFG_MAIL_SERVER_PASSWORD);
		if (mailServerPassword != null) {
			processEngineConfiguration.setMailServerPassword(mailServerPassword);
			logger.info("Mail server user password is changed (not shown for security)");
		}
		
		Integer mailServerPort = (Integer) masterConfigurationMap.get(MASTER_CFG_MAIL_SERVER_PORT);
		if (mailServerPort != null) {
			processEngineConfiguration.setMailServerPort(mailServerPort);
			logger.info("Mail server port is changed to " + mailServerPort);
		}
		
		Boolean mailServerUseSsl = (Boolean) masterConfigurationMap.get(MASTER_CFG_MAIL_SERVER_USE_SSL);
		if (mailServerUseSsl != null) {
			processEngineConfiguration.setMailServerUseSSL(mailServerUseSsl);
			logger.info("Mail server useSsl is changed to " + mailServerUseSsl);
		}
		
		Boolean mailServerUseTls = (Boolean) masterConfigurationMap.get(MASTER_CFG_MAIL_SERVER_USE_TLS);
		if (mailServerUseTls != null) {
			processEngineConfiguration.setMailServerUseTLS(mailServerUseTls);
			logger.info("Mail server useTls is changed to " + mailServerUseTls);
		}
		
		String mailServerDefaultFrom = (String) masterConfigurationMap.get(MASTER_CFG_MAIL_SERVER_DEFAULT_FROM);
		if (mailServerDefaultFrom != null) {
			processEngineConfiguration.setMailServerDefaultFrom(mailServerDefaultFrom);
			logger.info("Mail server default from is changed to " + mailServerDefaultFrom);
		}
		
		String mailServerJndi = (String) masterConfigurationMap.get(MASTER_CFG_MAIL_SERVER_JNDI);
		if (mailServerJndi != null) {
			processEngineConfiguration.setMailSessionJndi(mailServerJndi);
			logger.info("Mail server session jndi is changed to " + mailServerJndi);
		}
		
	}
	
	protected void initMasterConfigurationForAdvancedSettings(ProcessEngineConfigurationImpl processEngineConfiguration) {
		
		Integer idBlockSize = (Integer) masterConfigurationMap.get(MASTER_CFG_ID_BLOCK_SIZE);
		if (idBlockSize != null) {
			processEngineConfiguration.setIdBlockSize(idBlockSize);
			logger.info("Id block size is changed to " + idBlockSize);
		}
		
		Integer processDefinitionCacheLimit = (Integer) masterConfigurationMap.get(MASTER_CFG_PROC_DEF_CACHE_LIMIT);
		if (processDefinitionCacheLimit != null) {
			processEngineConfiguration.setProcessDefinitionCacheLimit(processDefinitionCacheLimit);
			logger.info("Process definition cache limit is changed to " + processDefinitionCacheLimit);
		}
		
	}
	
	protected void initMasterConfigurationChangeListener() {
	  if (masterConfigurationMap != null) {
	    masterConfigurationMap.addEntryListener(new EntryListener<String, Object>() {
        
        @Override
        public void entryUpdated(EntryEvent<String, Object> event) {
          logMasterConfigChangeWarning(event.getKey());
        }
        
        @Override
        public void entryRemoved(EntryEvent<String, Object> event) {
          logMasterConfigChangeWarning(event.getKey());
        }
        
        @Override
        public void entryEvicted(EntryEvent<String, Object> event) {
          logMasterConfigChangeWarning(event.getKey());
        }
        
        @Override
        public void entryAdded(EntryEvent<String, Object> event) {
          logMasterConfigChangeWarning(event.getKey());
        }
      }, false);
	  }
	}
	
	protected void logMasterConfigChangeWarning(String changedElement) {
	  logger.warn("Master configuration has been updated. This could mean a reboot of this node is needed to use the updated values!");
	}
	
	protected void initCustomCommandInterceptor(ProcessEngineConfigurationImpl processEngineConfiguration) {
		List<CommandInterceptor> commandInterceptors = processEngineConfiguration.getCustomPreCommandInterceptors();
		if (commandInterceptors == null) {
			commandInterceptors = new ArrayList<CommandInterceptor>();
		}
		
		gatherMetricsCommandInterceptor = new GatherMetricsCommandInterceptor();
		commandInterceptors.add(0, gatherMetricsCommandInterceptor);
		processEngineConfiguration.setCustomPreCommandInterceptors(commandInterceptors);
	}
	
	protected void initSendEventsThread() {
		jvmMetricsManager = new JvmMetricsManager();
		
		SendEventsRunnable sendEventsRunnable = new SendEventsRunnable(uniqueNodeId, adminAppState, eventQueue);
		sendEventsRunnable.setJvmMetricsManager(jvmMetricsManager);
		sendEventsRunnable.setWrappedProcessDefinitionCache(wrappedProcessDefinitionCache);
		sendEventsRunnable.setGatherMetricsCommandInterceptor(gatherMetricsCommandInterceptor);
		sendEventsRunnable.setJobMetricsManager(jobMetricsManager);
		sendEventsRunnable.setMasterConfigurationState(masterConfigurationState);
		sendEventsRunnable.setWrappedJobExecutor(wrappedJobExecutor);
		
		String metricSendingIntervalString = clusterConfigProperties.getMetricSendingInterval();
		Integer metricSendingInterval = 60;
		if (metricSendingIntervalString != null) {
			metricSendingInterval = Integer.valueOf(metricSendingIntervalString);
		} else {
			logger.info("No metric sending interval configured, defaulting to " + metricSendingInterval + " seconds");
		}

		final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(sendEventsRunnable, 5, metricSendingInterval, TimeUnit.SECONDS);

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				logger.info("Shutting down threadpool used to send metrics...");
				executorService.shutdown();
				try {
					executorService.awaitTermination(60, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					logger.warn("Waited for 1 minute for threadpool (used for sending metrics) shutdown, but was interrupted", e);
				}
				logger.info("Shutdown of threadpool used to send metrics completed.");
			}
		});
	}

	public int getPriority() {
		return PRIORITY;
	}

}
