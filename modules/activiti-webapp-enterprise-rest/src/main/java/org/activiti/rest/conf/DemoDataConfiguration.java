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

package org.activiti.rest.conf;

import javax.annotation.PostConstruct;

import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;


/**
 * @author Joram Barrez
 */
@Configuration
public class DemoDataConfiguration {
  
  protected static final Logger LOGGER = LoggerFactory.getLogger(DemoDataConfiguration.class);

  @Autowired
  protected IdentityService identityService;
  
  @Autowired
  protected RepositoryService repositoryService;
  
  @Autowired
  protected TaskService taskService;
  
  @Autowired
  protected Environment environment;
  
  @PostConstruct
  public void init() {
    
    if (Boolean.valueOf(environment.getProperty("default.user.enable", "true"))) {
    	if (identityService.createUserQuery().count() == 0) {
	      LOGGER.info("Initializing default user");
	      initDefaultUser();
    	}
    }
    
  }

  protected void initDefaultUser() {
  	String userName = environment.getProperty("default.user.name", "admin");
  	String password = environment.getProperty("default.user.passsword", "admin");
  	createUser(userName, "Admin", "Admin", password, "admin@app.activiti.com");
  }
  
  protected void createUser(String userId, String firstName, String lastName, String password,  String email) {
    
    if (identityService.createUserQuery().userId(userId).count() == 0) {
      
      // Following data can already be set by demo setup script
      
      User user = identityService.newUser(userId);
      user.setFirstName(firstName);
      user.setLastName(lastName);
      user.setPassword(password);
      user.setEmail(email);
      identityService.saveUser(user);
      
    }
    
  }
  
}
