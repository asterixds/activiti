package com.activiti.license;

public class FeatureInfo {

	protected int numberOfLicenses = 1;
	protected int numberOfProcesses = 100;
	protected int numberOfEditors = 5;
	protected int numberOfAdmins = 1;
	protected boolean multiTenant = false;
	protected String defaultTenant = "test";
	
	public int getNumberOfLicenses() {
		return numberOfLicenses;
	}
	public void setNumberOfLicenses(int numberOfLicenses) {
		this.numberOfLicenses = numberOfLicenses;
	}
	public int getNumberOfProcesses() {
		return numberOfProcesses;
	}
	public void setNumberOfProcesses(int numberOfProcesses) {
		this.numberOfProcesses = numberOfProcesses;
	}
	public int getNumberOfEditors() {
		return numberOfEditors;
	}
	public void setNumberOfEditors(int numberOfEditors) {
		this.numberOfEditors = numberOfEditors;
	}
	public int getNumberOfAdmins() {
		return numberOfAdmins;
	}
	public void setNumberOfAdmins(int numberOfAdmins) {
		this.numberOfAdmins = numberOfAdmins;
	}
  public boolean isMultiTenant() {
    return multiTenant;
  }
  public void setMultiTenant(boolean multiTenant) {
    this.multiTenant = multiTenant;
  }
  public String getDefaultTenant() {
    return defaultTenant;
  }
  public void setDefaultTenant(String defaultTenant) {
    this.defaultTenant = defaultTenant;
  }
}
