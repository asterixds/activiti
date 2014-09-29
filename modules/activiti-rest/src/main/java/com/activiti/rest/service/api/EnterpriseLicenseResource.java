package com.activiti.rest.service.api;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.activiti.engine.impl.ProcessEngineImpl;
import org.activiti.rest.common.api.ActivitiUtil;
import org.activiti.rest.common.api.SecuredResource;
import org.restlet.data.Status;
import org.restlet.engine.util.Base64;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

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
public class EnterpriseLicenseResource extends SecuredResource {

  private static final String KEY = "vN9kuqLLj5SDZ4UpBP6ekonWSVFJwZgg";
  private static final String ALGORITHM = "DESede";
  
  @Get
  public EnterpriseLisenceResponse getLisenceInfo() {
    if (authenticate() == false)
      return null;

    LicenseHolder licenseHolder = ((ProcessEngineImpl) ActivitiUtil.getProcessEngine())
            .getProcessEngineConfiguration().getLicenseHolder();
    
    try {
      License license = licenseHolder.getLicense();
      EnterpriseLisenceResponse response = new EnterpriseLisenceResponse();
      response.setHolder(license.getFeature(LicenseHolder.HOLDER));
      response.setLicenseCheck(generateLicenseCheck(license.getFeature(LicenseHolder.HOLDER)));
      return response;
      
    } catch(LicenseException le) {
      // Throw 409, indicating enterprise version is running with invalid license
      throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, le);
    }
  }

  private String generateLicenseCheck(String holder) {
    try {
      SecretKeySpec spec = new SecretKeySpec(Base64.decode(KEY), ALGORITHM);
      Cipher c = Cipher.getInstance(ALGORITHM);
      c.init(Cipher.ENCRYPT_MODE, spec);
      
      String toEncrypt = holder + "|" + Calendar.getInstance().getTimeInMillis();
      byte[] inputBytes = toEncrypt.getBytes();
      byte[] encryptionBytes = c.doFinal(inputBytes);
      
      return Base64.encode(encryptionBytes, false);
      
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
