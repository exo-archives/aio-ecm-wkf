/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.cms.queries;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Quang Hung
 *          nguyenkequanghung@yahoo.com
 * mar 02, 2007 
 */
public class QueryData{

  private String name ;
  private String language ;
  private String statement ;
  private String permissions ;
  private boolean cachedResult ;
  
  public  QueryData(){}

  public String getName() { return this.name ; }
  public void setName(String name) { this.name = name ; }  

  public String getLanguage() { return this.language ; }
  public void setLanguage(String l) { this.language = l ; }
  
  public String getPermissions() { return this.permissions ; }
  public void setPermissions(String permission) { this.permissions = permission ; }

  public String getStatement() { return this.statement ; }
  public void setStatement(String s) { this.statement = s ; }
  
  public boolean getCacheResult() { return this.cachedResult ; }
  public void setCacheResult(boolean r) { this.cachedResult = r ; }
}
