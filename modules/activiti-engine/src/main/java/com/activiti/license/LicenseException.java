package com.activiti.license;


public class LicenseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LicenseException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public LicenseException(String message) {
		super(message);
	}
}
