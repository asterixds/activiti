/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.activiti.addon.cluster;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is a service that gives access to the operations the Activiti Admin App exposes
 * with regard to an Activiti Process Engine.
 * 
 * @author Joram Barrez
 */
public class AdminAppService {
	
	private static final Logger logger = LoggerFactory.getLogger(AdminAppService.class);

	protected ObjectMapper objectMapper = new ObjectMapper();
	
	protected String url;
	protected String clusterName;
	protected String userName;
	protected String password;
	
	public AdminAppService() {
		
	}
	
	public AdminAppService(String url, String clusterName, String userName, String password) {
		this.url = url;
		this.clusterName =  clusterName;
		this.userName = userName;
		this.password = password;
	}

	public boolean publishEvents(List<Map<String, Object>> events) {
		
		String eventsString = null;
    try {
	    eventsString = objectMapper.writeValueAsString(events);
    } catch (JsonProcessingException e) {
    	logger.error("Could not serialize events map to json string", e);
    	return false;
    }
		
		CloseableHttpClient client = getHttpClient();
		try {

			HttpPost post = new HttpPost(url + "/api/enterprise/" + clusterName + "/events");
			post.setHeader("Content-Type", "application/json");
			HttpEntity entity = new ByteArrayEntity(eventsString.getBytes("UTF-8"));
			post.setEntity(entity);
			CloseableHttpResponse response = client.execute(post);
			
			try {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != 200) {
					logger.warn("Could not post events to Activiti Admin Application. Received status code: " + statusCode);
					return false;
				}

			} finally {
				response.close();
			}

		} catch (Exception e) {
			logger.warn("Error posting events to Activiti Admin Application: " + e.getMessage());
			return false;

		} finally {
			try {
				client.close();
			} catch (Exception e) {
				logger.warn("Could not close http client", e);
				return false;
			}
		}
		return true;
	}
	
	public CloseableHttpClient getHttpClient() {
		

    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    
    // Basic Auth
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

    // Self signed certs for https
    try {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        httpClientBuilder.setSSLSocketFactory(sslsf);
    } catch (Exception e) {
    		logger.warn("Could not configure HTTP client to use SSL" , e);
    }
    
    // Connection timeout
    RequestConfig config = RequestConfig.custom()
        .setSocketTimeout(30000)
        .setConnectTimeout(30000)
        .build();
    httpClientBuilder.setDefaultRequestConfig(config);

    return httpClientBuilder.build();
	}
	
	public void setUrl(String url) {
		this.url = url;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}
