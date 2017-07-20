package com.activiti.license;

import java.io.InputStream;

/**
 * Implement this interface to load license data from a custom location such as a database.
 * When the process engine configuration is set, make sure to add the custom license loader to the license holder
 * using licenseHolder.setCustomLicenseLoader(licenseLoader);
 */
public interface CustomLicenseLoader {

    /**
     * Returns the license data from a custom location.
     *
     * @return null if no license was found or an InputStream containing the license data.
     */
    InputStream loadLicense();
}
