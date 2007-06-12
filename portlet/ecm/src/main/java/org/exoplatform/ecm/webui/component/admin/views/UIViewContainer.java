/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.views;

import org.exoplatform.ecm.webui.component.UIECMPermissionBrowser;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 23, 2006
 * 2:09:18 PM 
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIViewContainer extends UIContainer {

  public UIViewContainer() throws Exception {
    addChild(UIViewList.class, null, null) ;
  }

  public void initPopup(String popupId) throws Exception {
    removeChildById(popupId) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, popupId) ;
    uiPopup.setWindowSize(600,400) ;
    UIViewFormTabPane uiViewForm = createUIComponent(UIViewFormTabPane.class, null, null) ;
    uiPopup.setUIComponent(uiViewForm) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
  
  public void update() throws Exception {
    getChild(UIViewList.class).updateViewListGrid() ;
  }
  
  public void initPopupPermission(String membership) throws Exception {
    removeChildById(UIViewFormTabPane.POPUP_PERMISSION) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, UIViewFormTabPane.POPUP_PERMISSION);
    uiPopup.setWindowSize(600, 300);
    UIECMPermissionBrowser uiECMPermission = 
      createUIComponent(UIECMPermissionBrowser.class, null, null) ;
    uiPopup.setUIComponent(uiECMPermission);
    UIViewForm uiViewForm = findFirstComponentOfType(UIViewForm.class) ;
    if(membership != null && membership.indexOf(":/") > -1) {
      String[] arrMember = membership.split(":/") ;
      uiECMPermission.setCurrentPermission("/" + arrMember[1]) ;
    }
    uiECMPermission.setComponent(uiViewForm, null) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
}
