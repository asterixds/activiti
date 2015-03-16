package com.activiti.rest.service.api.version;

import org.activiti.engine.ProcessEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppVersionResource {

  @Autowired
  protected ProcessEngine processEngine;
  
  @RequestMapping(value="/enterprise/app-version", method = RequestMethod.GET, produces="application/json")
  public AppVersionResponse getAppVersion() {
    AppVersionResponse appVersionResponse = new AppVersionResponse();
    appVersionResponse.setType("engine");
    appVersionResponse.setVersion(processEngine.VERSION);
    return appVersionResponse;
  }

}
