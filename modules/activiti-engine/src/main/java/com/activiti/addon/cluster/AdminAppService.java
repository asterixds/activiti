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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
	
	protected Cookie cookie;
	
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
		
		CloseableHttpClient client = getAuthenticatedClient();
		try {

			HttpPost post = new HttpPost(url + "/app/rest/" + clusterName + "/events");
			post.setHeader("Content-Type", "application/json");
			HttpEntity entity = new ByteArrayEntity(eventsString.getBytes("UTF-8"));
			post.setEntity(entity);
			CloseableHttpResponse response = client.execute(post);
			
			try {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != 200) {
					logger.warn("Unexpected http status code received when posting events : " + statusCode);
					return false;
				}

			} finally {
				response.close();
			}

		} catch (Exception e) {
			logger.warn("Error posting events to Activiti Admin Application", e);
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
	
	/**
	 * Creates an authenticated {@link HttpClient} to use when connecting to the Admin App. 
	 */
	protected CloseableHttpClient getAuthenticatedClient() {
		
		// Build session
		BasicCookieStore cookieStore = new BasicCookieStore();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();

		// If cookie wasn't provided in the cookie string, try to get a new cookie by logging in
		if (cookie == null) {
			try {
				HttpUriRequest login = RequestBuilder.post()
				    .setUri(new URI(url + "/app/authentication"))
				    .addParameter("j_username", userName)
				    .addParameter("j_password", password)
				    .addParameter("_spring_security_remember_me", "true").build();

				CloseableHttpResponse response = httpClient.execute(login);

				try {
					EntityUtils.consume(response.getEntity());
					List<Cookie> cookies = cookieStore.getCookies();
					if (cookies.isEmpty()) {
						// nothing to do
					} else {
						this.cookie = cookies.get(0);
					}

				} finally {
					response.close();
				}

			} catch (Exception e) {
				logger.error("Error authenticating " + userName, e);
			}

		} else {
			// setting cookie from cache
			cookieStore.addCookie(cookie);
		}

		return httpClient;
	}

	/**
	 * Little helper method that converts hex values from strings to byte array.
	 *
	 * @param hexString
	 *          string of hex-encoded values
	 * @return decoded byte array
	 */
	protected byte[] hexStringToByteArray(String hexString) {
		int len = hexString.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character
			    .digit(hexString.charAt(i + 1), 16));
		}
		return data;
	}
	
	/**
	 * Using some super basic byte array &lt;-&gt; hex conversions so we don't
	 * have to rely on any large Base64 libraries. 
	 */
	protected String byteArrayToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte element : bytes) {
			int v = element & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
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
