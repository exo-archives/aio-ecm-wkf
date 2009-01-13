/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.workflow.webui.component;

import org.exoplatform.webui.form.UIFormInput;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 9, 2009  
 */
public class InputInfo {
  private String label;
  private UIFormInput input;
  private String id;
  private String path;
  private boolean mandatory;

  public InputInfo(String id, String path, String label, UIFormInput input, boolean mandatory) {
    this.label = label;
    this.input = input;
    this.id = id;
    this.path = path;
    this.mandatory = mandatory;
  }

  public UIFormInput getInput() { return input; }
  public String getLabel() { return label; }
  public String getId() { return id; }
  public String getPath() { return path; }  
  public boolean isMandatory(){ return mandatory; }
}
