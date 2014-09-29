package com.activiti.license;

import java.util.Date;

public class LicenseInfo {
	
	public static final String DEFAULT_SUBJECT = "CN=Unknown, OU=Unknown, O=Alfresco, L=Maidenhead, ST=Berkshire, C=UK";

	protected String fileName;
	protected String holder; // customer name
	protected String subject = DEFAULT_SUBJECT; // Alfresco Activiti
	protected String productKey; // version
	protected Date goodAfterDate;
	protected Date goodBeforeDate;
	protected FeatureInfo featureInfo = new FeatureInfo();
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getHolder() {
		return holder;
	}
	public void setHolder(String holder) {
		this.holder = holder;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getProductKey() {
		return productKey;
	}
	public void setProductKey(String productKey) {
		this.productKey = productKey;
	}
	public Date getGoodAfterDate() {
		return goodAfterDate;
	}
	public void setGoodAfterDate(Date goodAfterDate) {
		this.goodAfterDate = goodAfterDate;
	}
	public Date getGoodBeforeDate() {
		return goodBeforeDate;
	}
	public void setGoodBeforeDate(Date goodBeforeDate) {
		this.goodBeforeDate = goodBeforeDate;
	}
	public FeatureInfo getFeatureInfo() {
		return featureInfo;
	}
	public void setFeatureInfo(FeatureInfo featureInfo) {
		this.featureInfo = featureInfo;
	}
}
