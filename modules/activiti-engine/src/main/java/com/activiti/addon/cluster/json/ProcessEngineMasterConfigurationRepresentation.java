/**
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package com.activiti.addon.cluster.json;

/**
 * Representation of the config json stored for an Activiti Process Engine
 * master config.
 *
 * @author jbarrez
 */
public class ProcessEngineMasterConfigurationRepresentation extends MasterConfigurationRepresentation {

	private static final long serialVersionUID = 1L;

	protected String jdbcUrl;
	protected String jdbcDriver;
	protected String jdbcUsername;
	protected String jdbcPassword;

	protected String dataSourceJndiName;

	protected String databaseSchemaUpdate;

	protected Integer jdbcMaxActiveConnections;
	protected Integer jdbcMaxIdleConnections;
	protected Integer jdbcMaxCheckoutTime;
	protected Integer jdbcMaxWaitTime;

	protected String history;

	protected String mailServerHost;
	protected String mailServerUsername;
	protected String mailServerPassword;
	protected Integer mailServerPort;
	protected Boolean mailServerUseSsl;
	protected String mailServerUseTls;
	protected String mailServerDefaultFrom;
	protected String mailServerJndi;

	protected Integer idBlockSize;

	protected Integer processDefinitionCacheLimit;

	protected Boolean enableJobExecutor;
	protected Integer jobMaxPoolSize;
	protected Integer jobCorePoolSize;
	protected Integer jobQueueSize;
	protected Integer jobLockTime;
	protected Integer jobWaitTime;
	protected Integer maxJobsPerAcquisition;

	public ProcessEngineMasterConfigurationRepresentation() {
		setType("engine");
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public String getJdbcUsername() {
		return jdbcUsername;
	}

	public void setJdbcUsername(String jdbcUsername) {
		this.jdbcUsername = jdbcUsername;
	}

	public String getJdbcPassword() {
		return jdbcPassword;
	}

	public void setJdbcPassword(String jdbcPassword) {
		this.jdbcPassword = jdbcPassword;
	}

	public String getDatabaseSchemaUpdate() {
		return databaseSchemaUpdate;
	}

	public void setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
		this.databaseSchemaUpdate = databaseSchemaUpdate;
	}

	public String getDataSourceJndiName() {
		return dataSourceJndiName;
	}

	public void setDataSourceJndiName(String dataSourceJndiName) {
		this.dataSourceJndiName = dataSourceJndiName;
	}

	public Integer getJdbcMaxActiveConnections() {
		return jdbcMaxActiveConnections;
	}

	public void setJdbcMaxActiveConnections(Integer jdbcMaxActiveConnections) {
		this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
	}

	public Integer getJdbcMaxIdleConnections() {
		return jdbcMaxIdleConnections;
	}

	public void setJdbcMaxIdleConnections(Integer jdbcMaxIdleConnections) {
		this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
	}

	public Integer getJdbcMaxCheckoutTime() {
		return jdbcMaxCheckoutTime;
	}

	public void setJdbcMaxCheckoutTime(Integer jdbcMaxCheckoutTime) {
		this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
	}

	public Integer getJdbcMaxWaitTime() {
		return jdbcMaxWaitTime;
	}

	public void setJdbcMaxWaitTime(Integer jdbcMaxWaitTime) {
		this.jdbcMaxWaitTime = jdbcMaxWaitTime;
	}

	public String getHistory() {
		return history;
	}

	public void setHistory(String history) {
		this.history = history;
	}

	public String getMailServerHost() {
		return mailServerHost;
	}

	public void setMailServerHost(String mailServerHost) {
		this.mailServerHost = mailServerHost;
	}

	public String getMailServerUsername() {
		return mailServerUsername;
	}

	public void setMailServerUsername(String mailServerUsername) {
		this.mailServerUsername = mailServerUsername;
	}

	public String getMailServerPassword() {
		return mailServerPassword;
	}

	public void setMailServerPassword(String mailServerPassword) {
		this.mailServerPassword = mailServerPassword;
	}

	public Integer getMailServerPort() {
		return mailServerPort;
	}

	public void setMailServerPort(Integer mailServerPort) {
		this.mailServerPort = mailServerPort;
	}

	public Boolean getMailServerUseSsl() {
		return mailServerUseSsl;
	}

	public void setMailServerUseSsl(Boolean mailServerUseSsl) {
		this.mailServerUseSsl = mailServerUseSsl;
	}

	public String getMailServerUseTls() {
		return mailServerUseTls;
	}

	public void setMailServerUseTls(String mailServerUseTls) {
		this.mailServerUseTls = mailServerUseTls;
	}

	public String getMailServerDefaultFrom() {
		return mailServerDefaultFrom;
	}

	public void setMailServerDefaultFrom(String mailServerDefaultFrom) {
		this.mailServerDefaultFrom = mailServerDefaultFrom;
	}

	public String getMailServerJndi() {
		return mailServerJndi;
	}

	public void setMailServerJndi(String mailServerJndi) {
		this.mailServerJndi = mailServerJndi;
	}

	public Integer getIdBlockSize() {
		return idBlockSize;
	}

	public void setIdBlockSize(Integer idBlockSize) {
		this.idBlockSize = idBlockSize;
	}

	public Integer getProcessDefinitionCacheLimit() {
		return processDefinitionCacheLimit;
	}

	public void setProcessDefinitionCacheLimit(Integer processDefinitionCacheLimit) {
		this.processDefinitionCacheLimit = processDefinitionCacheLimit;
	}

	public Boolean getEnableJobExecutor() {
		return enableJobExecutor;
	}

	public void setEnableJobExecutor(Boolean enableJobExecutor) {
		this.enableJobExecutor = enableJobExecutor;
	}

	public Integer getJobMaxPoolSize() {
		return jobMaxPoolSize;
	}

	public void setJobMaxPoolSize(Integer jobMaxPoolSize) {
		this.jobMaxPoolSize = jobMaxPoolSize;
	}

	public Integer getJobCorePoolSize() {
		return jobCorePoolSize;
	}

	public void setJobCorePoolSize(Integer jobCorePoolSize) {
		this.jobCorePoolSize = jobCorePoolSize;
	}

	public Integer getJobQueueSize() {
		return jobQueueSize;
	}

	public void setJobQueueSize(Integer jobQueueSize) {
		this.jobQueueSize = jobQueueSize;
	}

	public Integer getJobLockTime() {
		return jobLockTime;
	}

	public void setJobLockTime(Integer jobLockTime) {
		this.jobLockTime = jobLockTime;
	}

	public Integer getJobWaitTime() {
		return jobWaitTime;
	}

	public void setJobWaitTime(Integer jobWaitTime) {
		this.jobWaitTime = jobWaitTime;
	}

	public Integer getMaxJobsPerAcquisition() {
		return maxJobsPerAcquisition;
	}

	public void setMaxJobsPerAcquisition(Integer maxJobsPerAcquisition) {
		this.maxJobsPerAcquisition = maxJobsPerAcquisition;
	}

}
