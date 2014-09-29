package com.activiti.license;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.runtime.Clock;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.verhas.licensor.License;

public final class LicenseHolder {

  private final Logger log = LoggerFactory.getLogger(LicenseHolder.class);
  
  private static final String LICENSE_FILE = "activiti.lic";
  
  private static final String PUBLIC_KEY_FILE = "pubring.gpg";

  public static final String HOLDER = "holder";
  public static final String SUBJECT = "subject";
  public static final String PRODUCT_KEY = "productKey";
  public static final String GOOD_AFTER_DATE = "goodAfterDate";
  public static final String GOOD_BEFORE_DATE = "goodBeforeDate";
  public static final String NUMBER_LICENSES = "numberOfLicenses";
  public static final String NUMBER_PROCESSES = "numberOfProcesses";
  public static final String NUMBER_EDITORS = "numberOfEditors";
  public static final String NUMBER_ADMINS = "numberOfAdmins";

  private static List<String> validVersionsList = Arrays.asList("1.0ev", "1.0ent", "1.0dep");
  
  private static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");
  
  private static final byte[] digest = new byte[] { (byte) 0x6D, (byte) 0x15, (byte) 0xE2, (byte) 0x0B, (byte) 0xA1, (byte) 0x68, (byte) 0x5D, (byte) 0x91, (byte) 0x55,
      (byte) 0x0C, (byte) 0x8E, (byte) 0xCA, (byte) 0x3A, (byte) 0x55, (byte) 0x2C, (byte) 0x44, (byte) 0x15, (byte) 0x72, (byte) 0x75, (byte) 0xED,
      (byte) 0xB6, (byte) 0x6D, (byte) 0xFF, (byte) 0xE7, (byte) 0x8F, (byte) 0xC0, (byte) 0xCF, (byte) 0xA0, (byte) 0x53, (byte) 0x37, (byte) 0xB5,
      (byte) 0xBF, (byte) 0x05, (byte) 0x3E, (byte) 0x16, (byte) 0x6E, (byte) 0xD0, (byte) 0x68, (byte) 0xA1, (byte) 0x81, (byte) 0x6D, (byte) 0xAA,
      (byte) 0x4E, (byte) 0x8E, (byte) 0x1F, (byte) 0x05, (byte) 0x9D, (byte) 0x99, (byte) 0x4F, (byte) 0x98, (byte) 0x21, (byte) 0x8B, (byte) 0xAD,
      (byte) 0xDB, (byte) 0x15, (byte) 0x07, (byte) 0x21, (byte) 0xF1, (byte) 0x23, (byte) 0x8F, (byte) 0x0E, (byte) 0x2B, (byte) 0xF4, (byte) 0x2E, };
  
  private Clock clock;
  
  public static final String SYSTEM_VAR_LICENSE_LOCATION = "activitiLicenseLocation";
  private String customLocationPath;
  private String customLocationClassPath;
  
  private static final long CACHE_TIME = 60 * 60 * 1000L; // 1 hour
  private License cachedLicense;
  private Date lastLicenseReadDate; // The license is cached for a certain time before it is re-read from the classpath


  public final boolean isLicenseValid() {
    try {
      License license = getLicense();
      validateLicense(license);
      return true;
    } catch (LicenseException e) {
      log.error("License file could not be validated:", e);
    } catch (Throwable t) {
      log.error("Unexpected exception while validating license", t);
    }
    return false;
  }
  

  public final void validateLicense(License license) {

    String holder = license.getFeature(HOLDER);
    if (StringUtils.isEmpty(holder)) {
      throw new LicenseException("Expected holder info is not present");
    }

    String subject = license.getFeature(SUBJECT);
    if (LicenseInfo.DEFAULT_SUBJECT.equals(subject) == false) {
      throw new LicenseException("Subject value is not expected");
    }

    String productKey = license.getFeature(PRODUCT_KEY);
    if (validVersionsList.contains(productKey) == false) {
      throw new LicenseException("Product key '" + productKey + "' is not valid");
    }

    String goodAfterDateString = license.getFeature(GOOD_AFTER_DATE);
    Date goodAfterDate = null;
    try {
      goodAfterDate = dateFormat.parse(goodAfterDateString);
    } catch (Exception e) {
      throw new LicenseException("Error parsing good after date '" + goodAfterDate + "'", e);
    }
    
    Date todayDate = clock.getCurrentTime();
    if (todayDate.before(goodAfterDate)) {
      throw new LicenseException("License is not valid yet, license is valid starting from " + goodAfterDateString);
    }

    String goodBeforeDateString = license.getFeature(GOOD_BEFORE_DATE);
    Date goodBeforeDate = null;
    try {
      goodBeforeDate = dateFormat.parse(goodBeforeDateString);
    } catch (Exception e) {
      throw new LicenseException("Error parsing good before date", e);
    }

    if (todayDate.after(goodBeforeDate)) {
      throw new LicenseException("License is not valid anymore, license was valid until " + goodBeforeDateString);
    }
    
  }
  
  public final FeatureInfo getLicenseFeatureInfo() {
    License license = getLicense();
    FeatureInfo featureInfo = new FeatureInfo();
    featureInfo.setNumberOfLicenses(Integer.valueOf(license.getFeature(NUMBER_LICENSES)));
    featureInfo.setNumberOfProcesses(Integer.valueOf(license.getFeature(NUMBER_PROCESSES)));
    featureInfo.setNumberOfEditors(Integer.valueOf(license.getFeature(NUMBER_EDITORS)));
    featureInfo.setNumberOfAdmins(Integer.valueOf(license.getFeature(NUMBER_ADMINS)));
    return featureInfo;
  }
  
  public final License getLicense() {
    Date now = getClock().getCurrentTime();
    if (cachedLicense == null || lastLicenseReadDate == null || (now.getTime() - lastLicenseReadDate.getTime() > CACHE_TIME) ) {
      
      // Check if there is a system property set
      String locationAsSystemVariable = System.getProperty(SYSTEM_VAR_LICENSE_LOCATION);
      if (locationAsSystemVariable != null) {
        log.info("Using system variable set for license location: " + SYSTEM_VAR_LICENSE_LOCATION);
        License licenseFromCustomPath = loadLicenseFromFileLocation(locationAsSystemVariable, LICENSE_FILE);
        if (licenseFromCustomPath != null) {
          setCachedLicense(now, licenseFromCustomPath);
          return licenseFromCustomPath;
        }
      }
      
      // Check if there is a custom location set (eg special unit tests)
      if (customLocationPath != null) {
        License licenseFromCustomPath = loadLicenseFromFileLocation(customLocationPath, LICENSE_FILE);
        if (licenseFromCustomPath != null) {
          setCachedLicense(now, licenseFromCustomPath);
          return licenseFromCustomPath;
        }
      }
      
      // Check if there is a custom classpath location set (eg special unit tests)
      if (customLocationClassPath != null) {
        License licenseFromCustomClassPath = loadLicenseFromClassPath(customLocationClassPath);
        if (licenseFromCustomClassPath != null) {
          setCachedLicense(now, licenseFromCustomClassPath);
          return licenseFromCustomClassPath;
        }
      }
      
      // Try local user home
      License userHomeLicense = loadLicenseFromUserHome(LICENSE_FILE);
      if (userHomeLicense != null) {
        setCachedLicense(now, userHomeLicense);
        return userHomeLicense;
      } else {
        // Otherwise look for the license on the classpath
        License licenseFromClasspath = loadLicenseFromClassPath(LICENSE_FILE);
        setCachedLicense(now, licenseFromClasspath);
        return licenseFromClasspath;
      }
      
    }
    return cachedLicense;
  }


  private final void setCachedLicense(Date now, License userHomeLicense) {
    cachedLicense = userHomeLicense;
    lastLicenseReadDate = now;
  }

  private final synchronized License loadLicenseFromClassPath(String fileName) {
    License license = new License();
    try {
      InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PUBLIC_KEY_FILE);
      license.loadKeyRing(inputStream, digest);
      IoUtil.closeSilently(inputStream);
      InputStream fileStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
      license.setLicenseEncoded(fileStream);
      IoUtil.closeSilently(fileStream);
    } catch (Exception e) {
      throw new LicenseException("Error loading license file " + fileName, e);
    }
    return license;
  }
  
  /**
   * NOTE: contrary to the {@link #loadLicenseFromClassPath(String)} this will return NULL if 
   * the files were not found!
   * 
   *  The reason for this is because the user home approach is only ment for 
   *  local development and users should use the classpath approach only.
   */
  private final License loadLicenseFromUserHome(String fileName) {
    String fileSeparator = System.getProperty("file.separator");
    return loadLicenseFromFileLocation(System.getProperty("user.home") +  fileSeparator + ".activiti" + fileSeparator + "enterprise-license" + fileSeparator, fileName);
  }
  
  /**
   * NOTE: contrary to the {@link #loadLicenseFromClassPath(String)} this will return NULL if 
   * the files were not found!
   * 
   *  The reason for this is because the user home approach is only ment for 
   *  local development and users should use the classpath approach only.
   */
  private final synchronized License loadLicenseFromFileLocation(String location, String licenseFileName) {
    License license = new License();
    try {
      
      InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PUBLIC_KEY_FILE);
      license.loadKeyRing(inputStream, digest);
      IoUtil.closeSilently(inputStream);
      
      File licenseFile = new File(location + licenseFileName);
      if (!licenseFile.exists()) {
        return null;
      }
      FileInputStream licenseFileInputStream = new FileInputStream(licenseFile);
      license.setLicenseEncoded(licenseFileInputStream);
      IoUtil.closeSilently(licenseFileInputStream);
    } catch (Exception e) {
      return null;
    }
    return license;
  }
  
  public final boolean isEnterprise() {
    String productKey = getLicense().getFeature(PRODUCT_KEY);
    return productKey.endsWith("ent");
  }
  
  public final boolean isDepartemental() {
    String productKey = getLicense().getFeature(PRODUCT_KEY);
    return productKey.endsWith("dep");
  }
  
  public final boolean isEvaluation() {
    String productKey = getLicense().getFeature(PRODUCT_KEY);
    return productKey.endsWith("ev");
  }
  
  public final String getLicenseInformation() {
    String separator = System.getProperty("line.separator");
    StringBuilder strb = new StringBuilder();
    License license = getLicense();
    strb.append(separator + separator + "------------------------------------------" + separator);
    strb.append("Holder:" + license.getFeature(HOLDER) + separator);
    strb.append("Product Key:" + license.getFeature(PRODUCT_KEY) + separator);
    strb.append("Good After date:" + license.getFeature(GOOD_AFTER_DATE) + separator);
    strb.append("Good Before date:" + license.getFeature(GOOD_BEFORE_DATE) + separator);
    
    if (isDepartemental()) {
      FeatureInfo featureInfo = getLicenseFeatureInfo();
      strb.append("Max number of process instances:" + featureInfo.getNumberOfProcesses() + separator);
    }
    strb.append("------------------------------------------" + separator);
    
    return strb.toString();
  }
  
  public final String getCustomLocationPath() {
    return customLocationPath;
  }
  
  public final void setCustomLocationPath(String customLocationPath) {
    this.customLocationPath = customLocationPath;
  }
  
  public final String getCustomLocationClassPath() {
    return customLocationClassPath;
  }
  
  public final void setCustomLocationClassPath(String customLocationClassPath) {
    this.customLocationClassPath = customLocationClassPath;
  }

  private final Clock getClock() {
    if (clock == null) {
      clock = Context.getProcessEngineConfiguration().getClock();
    }
    return clock;
  }

  public final void setClock(Clock clock) {
    this.clock = clock;
  }
  
}
