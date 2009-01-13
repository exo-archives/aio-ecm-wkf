/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.workflow.webui.component;

import java.util.Map;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 9, 2009  
 */
public class VariableMaps {  
  private Map workflowVariables;
  private Map jcrVariables;
  
  public VariableMaps(Map workflowVariables, Map jcrVariables) {
    this.workflowVariables = workflowVariables;
    this.jcrVariables = jcrVariables;
  }
  
  public Map getJcrVariables() {
    return jcrVariables;
  }
  public Map getWorkflowVariables() {
    return workflowVariables;
  }
}
