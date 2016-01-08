package com.activiti.license;

/**
 * @author Erik Winlof
 */
public class LicenseNotFoundException extends LicenseException {

	private static final long serialVersionUID = 1L;

	public LicenseNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

    public LicenseNotFoundException(String message) {
        super(message);
    }
}
