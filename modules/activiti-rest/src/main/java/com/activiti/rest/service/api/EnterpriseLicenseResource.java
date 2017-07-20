package com.activiti.rest.service.api;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.time.FastDateFormat;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.rest.exception.ActivitiConflictException;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.activiti.license.LicenseException;
import com.activiti.license.LicenseHolder;
import com.activiti.license.LicenseNotFoundException;
import com.verhas.licensor.License;

/**
 * Enterprise-only resource that exposes the license-information for the engine
 * that is exposed though the REST-API.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Erik Winlof
 */
@RestController
public class EnterpriseLicenseResource {

  private static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");

  private static final String KEY = "vN9kuqLLj5SDZ4UpBP6ekonWSVFJwZgg";
  private static final String ALGORITHM = "DESede";

  @Autowired
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  
  @RequestMapping(value="/management/engine/license", method = RequestMethod.GET, produces="application/json")
  public EnterpriseLicenseResponse getLicenseInfo() {
    
    LicenseHolder licenseHolder = processEngineConfiguration.getLicenseHolder();

    try {
        EnterpriseLicenseResponse response = new EnterpriseLicenseResponse();
        try {
          License license = licenseHolder.getLicense();
          response.setHolder(license.getFeature(LicenseHolder.HOLDER));
          response.setLicenseCheck(generateLicenseCheck(license.getFeature(LicenseHolder.HOLDER)));
          response.setStatus(EnterpriseLicenseResponse.STATUS_VALID);
          if (!licenseDatesAreValid(license)) {
            response.setStatus(EnterpriseLicenseResponse.STATUS_INVALID_DATE);
          }
        } catch (LicenseNotFoundException le) {
          // Indicate that license didn't exist
          // (can't use 404 since that is assumed to be a non existing endpoint by the client)
          response.setStatus(EnterpriseLicenseResponse.STATUS_NOT_FOUND);
        }
        return response;
    } catch (LicenseException le) {
      // Throw 409, indicating enterprise version is running with an invalid license
      throw new ActivitiConflictException("Engine is running with invalid license", le);
    }
  }

  private boolean licenseDatesAreValid(License license) {
    String goodAfterDateString = license.getFeature(LicenseHolder.GOOD_AFTER_DATE);
    Date goodAfterDate = null;
    try {
        goodAfterDate = dateFormat.parse(goodAfterDateString);
    } catch (Exception e) {
        throw new LicenseException("Error parsing good after date", e);
    }

    Date todayDate = new Date();
    if (todayDate.before(goodAfterDate)) {
        return false;
    }

    String goodBeforeDateString = license.getFeature(LicenseHolder.GOOD_BEFORE_DATE);
    Date goodBeforeDate = null;
    try {
        goodBeforeDate = dateFormat.parse(goodBeforeDateString);
    } catch (Exception e) {
        throw new LicenseException("Error parsing good before date", e);
    }

    if (todayDate.after(goodBeforeDate)) {
        return false;
    }

    return true;
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
