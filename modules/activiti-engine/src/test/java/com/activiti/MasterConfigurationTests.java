package com.activiti;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;

import com.activiti.addon.cluster.json.ProcessEngineMasterConfigurationRepresentation;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jbarrez
 */
public class MasterConfigurationTests {
	
	private ClientAndServer proxy;
	private ClientAndServer mockServer;
	
	@Before
	public void startProxy() {
	    mockServer = startClientAndServer(8976);
	    proxy = startClientAndServer(8986);
	}	
	
	@After
	public void stopProxy() {
	    proxy.stop();
	    mockServer.stop();
	}
	
	@Test
	public void testClusterEnabledNoMasterConfig() {
		
		// Admin app mock returns no master config
		
		new MockServerClient("127.0.0.1", 8976)
	    .when(
	            request()
	                    .withMethod("GET")
	                    .withPath("/api/enterprise/test/master-config")
	            ,exactly(1)
	    )
	    .respond(
	            response()
	                    .withStatusCode(404)
	                    .withHeaders(
	                            new Header("Content-Type", "application/json; charset=utf-8"),
	                            new Header("Cache-Control", "public, max-age=86400")
	                    )
	    );
		
		ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
		processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1");
		processEngineConfiguration.setEnableClusterConfig(true);
		processEngineConfiguration.setEnterpriseAdminAppUrl("http://127.0.0.1:8976");
		processEngineConfiguration.setEnterpriseClusterName("test");
		processEngineConfiguration.setEnterpriseClusterUserName("testing");
		processEngineConfiguration.setEnterpriseClusterPassword("123");
		
		ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
		
		// No master config -> should not be overridden
		Assert.assertEquals("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", processEngine.getProcessEngineConfiguration().getJdbcUrl());
                 
	}
	
	@Test
	public void testClusterEnabledWithMasterConfig() throws Exception {
		
		// Admin app mock returns master config
		
		ProcessEngineMasterConfigurationRepresentation processEngineMasterConfigurationRepresentation 
			= new ProcessEngineMasterConfigurationRepresentation();
		processEngineMasterConfigurationRepresentation.setJdbcUrl("jdbc:h2:mem:overridden;DB_CLOSE_DELAY=-1");
		
		new MockServerClient("127.0.0.1", 8976)
	    .when(
	            request()
	                    .withMethod("GET")
	                    .withPath("/api/enterprise/test/master-config")
	            ,exactly(1)
	    )
	    .respond(
	            response()
	                    .withStatusCode(200)
	                    .withHeaders(
	                            new Header("Content-Type", "application/json; charset=utf-8"),
	                            new Header("Cache-Control", "public, max-age=86400")
	                    )
	                    .withBody(new ObjectMapper().writeValueAsString(processEngineMasterConfigurationRepresentation))
	                    
	    );
		
		ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
		processEngineConfiguration.setEnableClusterConfig(true);
		processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1");
		processEngineConfiguration.setEnterpriseAdminAppUrl("http://127.0.0.1:8976");
		processEngineConfiguration.setEnterpriseClusterName("test");
		processEngineConfiguration.setEnterpriseClusterUserName("testing");
		processEngineConfiguration.setEnterpriseClusterPassword("123");
		
		ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
		
		// No master config -> should not be overridden
		Assert.assertEquals("jdbc:h2:mem:overridden;DB_CLOSE_DELAY=-1", processEngine.getProcessEngineConfiguration().getJdbcUrl());
                 
	}
	
	@Test
	public void testClusterDisabledMasterConfigEnabled() {
		
		ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
		processEngineConfiguration.setEnableClusterConfig(false);
		processEngineConfiguration.setEnterpriseMasterConfigurationRequired(true);
		
		ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
		Assert.assertNotNull(processEngine);
                 
	}
	
	@Test
	public void testClusterEnabledMasterConfigEnabledButNoAdminApp() {
		
		ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
		processEngineConfiguration.setEnableClusterConfig(true);
		processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1");
		processEngineConfiguration.setEnterpriseAdminAppUrl("http://127.0.0.1:8976");
		processEngineConfiguration.setEnterpriseClusterName("test");
		processEngineConfiguration.setEnterpriseClusterUserName("testing");
		processEngineConfiguration.setEnterpriseClusterPassword("123");
		processEngineConfiguration.setEnterpriseMasterConfigurationRequired(true);
		
		try {
			processEngineConfiguration.buildProcessEngine();
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("Master configuration is required, but could not get it from Activiti Admin Application", e.getMessage());
		}
                 
	}
	
	@Test
	public void testClusterEnabledMasterConfigDisabled() {
		
		ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
		processEngineConfiguration.setEnableClusterConfig(true);
		processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1");
		processEngineConfiguration.setEnterpriseAdminAppUrl("http://127.0.0.1:8976");
		processEngineConfiguration.setEnterpriseClusterName("test");
		processEngineConfiguration.setEnterpriseClusterUserName("testing");
		processEngineConfiguration.setEnterpriseClusterPassword("123");
		processEngineConfiguration.setEnterpriseMasterConfigurationRequired(false);
		
		ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
		Assert.assertNotNull(processEngine);
                 
	}

}
