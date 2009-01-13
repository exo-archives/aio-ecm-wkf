/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.workflow.webui.component;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 12, 2009  
 */
public interface UISelectable {
  
  /**
   * Do select.
   * 
   * @param selectField the select field
   * @param value the value
   * @throws Exception the exception
   */
  public void doSelect(String selectField, Object value) throws Exception;
}
