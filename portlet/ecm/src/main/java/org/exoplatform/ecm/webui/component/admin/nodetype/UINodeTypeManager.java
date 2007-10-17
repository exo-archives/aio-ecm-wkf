/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.nodetype;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 20, 2006
 * 2:20:55 PM 
 */
@ComponentConfig (
    lifecycle = UIContainerLifecycle.class
)
public class UINodeTypeManager extends UIContainer {

  final static public String IMPORT_POPUP = "NodeTypeImportPopup" ;
  final static public String EXPORT_POPUP = "NodeTypeExportPopup" ;

  public UINodeTypeManager() throws Exception {
    addChild(UINodeTypeList.class, null, "ListNodeType") ;
  }
  
  public void update() throws Exception {
    getChild(UINodeTypeList.class).refresh(null) ;
  }
  public void setExportPopup() throws Exception {
    removeChildById(EXPORT_POPUP) ;
    UIPopupWindow  uiPopup = addChild(UIPopupWindow.class, null, EXPORT_POPUP);
    uiPopup.setWindowSize(500, 400);    
    UINodeTypeExport uiExport = uiPopup.createUIComponent(UINodeTypeExport.class, null, null) ;
    uiExport.update() ;
    uiPopup.setUIComponent(uiExport) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void setImportPopup() throws Exception {
    removeChildById(IMPORT_POPUP) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, IMPORT_POPUP);
    uiPopup.setWindowSize(500, 400);    
    UINodeTypeImportPopup uiImportPopup =
      uiPopup.createUIComponent(UINodeTypeImportPopup.class, null, null) ;
    uiPopup.setUIComponent(uiImportPopup) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
  
  public void initPopup(boolean isView) throws Exception {
    String popupId = "NodeTypePopup" ;
    if(isView) popupId = "ViewNodeTypePopup" ;
    removeChildById("NodeTypePopup") ;
    removeChildById("ViewNodeTypePopup") ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, popupId) ;
    UINodeTypeForm uiForm = createUIComponent(UINodeTypeForm.class, null, null) ;
    uiForm.update(null, false) ;
    uiPopup.setWindowSize(660, 400) ;
    uiPopup.setUIComponent(uiForm) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
}
