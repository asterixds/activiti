package com.activiti.rest.service.api;


/**
 * @author Tijs Rademakers
 */
public final class EnterpriseRestUrls {

  // Enterprise
  public static final String SEGMENT_DISCO_RESOURCES = "disco";
  public static final String SEGMENT_MANIFEST = "manifest.xml";
  public static final String SEGMENT_CATALOG = "catalog.xml";
  public static final String SEGMENT_LOG = "log";

  /**
   * URL template for the Disco manifest: <i>disco/manifest</i>
   */
  public static final String[] URL_DISCO_MANIFEST = {SEGMENT_DISCO_RESOURCES, SEGMENT_MANIFEST};
  
  /**
   * URL template for the Disco catalog: <i>disco/catalog</i>
   */
  public static final String[] URL_DISCO_CATALOG = {SEGMENT_DISCO_RESOURCES, SEGMENT_CATALOG};
  
  /**
   * URL template for the Disco catalog: <i>disco/log/{0:processDefinitionId}.dxml</i>
   */
  public static final String[] URL_DISCO_LOG = {SEGMENT_DISCO_RESOURCES, SEGMENT_LOG, "{0}.dxml"};
}
