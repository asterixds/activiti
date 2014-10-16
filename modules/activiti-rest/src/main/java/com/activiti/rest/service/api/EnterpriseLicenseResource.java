package com.activiti.rest.service.api;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.rest.exception.ActivitiConflictException;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.activiti.license.LicenseException;
import com.activiti.license.LicenseHolder;
import com.verhas.licensor.License;

/**
 * Enterprise-only resource that exposes the license-information for the engine
 * that is exposed though the REST-API.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 */
@RestController
public class EnterpriseLicenseResource {

  private static final String KEY = "vN9kuqLLj5SDZ4UpBP6ekonWSVFJwZgg";
  private static final String ALGORITHM = "DESede";
  
  @Autowired
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  
  @RequestMapping(value="/management/engine/license", method = RequestMethod.GET, produces="application/json")
  public EnterpriseLicenseResponse getLicenseInfo() {
    
    LicenseHolder licenseHolder = processEngineConfiguration.getLicenseHolder();
    
    try {
      License license = licenseHolder.getLicense();
      EnterpriseLicenseResponse response = new EnterpriseLicenseResponse();
      response.setHolder(license.getFeature(LicenseHolder.HOLDER));
      response.setLicenseCheck(generateLicenseCheck(license.getFeature(LicenseHolder.HOLDER)));
      return response;
      
    } catch (LicenseException le) {
      // Throw 409, indicating enterprise version is running with invalid license
      throw new ActivitiConflictException("Engine is running with invalid license", le);
    }
  }

  private String generateLicenseCheck(String holder) {
    try {
      Base64 base64 = new Base64();
      SecretKeySpec spec = new SecretKeySpec(base64.decode(KEY), ALGORITHM);
      Cipher c = Cipher.getInstance(ALGORITHM);
      c.init(Cipher.ENCRYPT_MODE, spec);
      
      String toEncrypt = holder + "|" + Calendar.getInstance().getTimeInMillis();
      byte[] inputBytes = toEncrypt.getBytes();
      byte[] encryptionBytes = c.doFinal(inputBytes);
      
      return base64.encodeAsString(encryptionBytes);
      
    } catch (NoSuchAlgorithmException e) {
      throw new LicenseException("Error while generating license check", e);
    } catch (NoSuchPaddingException e) {
      throw new LicenseException("Error while generating license check", e);
    } catch (InvalidKeyException e) {
      throw new LicenseException("Error while generating license check", e);
    } catch (IllegalBlockSizeException e) {
      throw new LicenseException("Error while generating license check", e);
    } catch (BadPaddingException e) {
      throw new LicenseException("Error while generating license check", e);
    }
  }
}
