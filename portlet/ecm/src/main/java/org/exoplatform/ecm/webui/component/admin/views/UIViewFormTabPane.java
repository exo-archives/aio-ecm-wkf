/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.views;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.webui.component.UIFormTabPane;
import org.exoplatform.webui.component.UIPopupWindow;
import org.exoplatform.webui.component.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 19, 2006
 * 5:31:04 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/component/UIFormTabPane.gtmpl",
    events = {
        @EventConfig( listeners = UIViewFormTabPane.SaveActionListener.class),
        @EventConfig( listeners = UIViewFormTabPane.ResetActionListener.class),
        @EventConfig( listeners = UIViewFormTabPane.EditTabActionListener.class),
        @EventConfig( listeners = UIViewFormTabPane.DeleteTabActionListener.class),
        @EventConfig( listeners = UIViewFormTabPane.ChangeVersionActionListener.class),
        @EventConfig( listeners = UIViewFormTabPane.CancelActionListener.class, phase = Phase.DECODE),
        @EventConfig( listeners = UIViewForm.AddPermissionActionListener.class, phase = Phase.DECODE)
      }
)
public class UIViewFormTabPane extends UIFormTabPane {  

  final static public String POPUP_PERMISSION = "PopupViewPermission" ;
  
  private UIViewForm uiViewForm ;
  private UITabForm uiTabForm ;

  public UIViewFormTabPane() throws Exception {
    super("UIViewFormTabPane", false) ;

    uiViewForm = new UIViewForm("UIViewForm") ;
    addUIComponentInput(uiViewForm) ;
    
    uiTabForm = new UITabForm("UITabForm") ;
    uiTabForm.setRendered(false) ;
    addUIComponentInput(uiTabForm) ;
    
    setRenderTabId("UIViewForm") ;
    setActions(new String[]{}) ;
  }
  
  public String getLabel(ResourceBundle res, String id)  {
    try {
      return res.getString("UIViewForm.label." + id) ;
    } catch (MissingResourceException ex) {
      return id ;
    }
  }
  
  static  public class SaveActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      if(uiViewTabPane.getRenderTabId().equalsIgnoreCase("UIViewForm")) {
        uiViewTabPane.uiViewForm.save() ;
        uiViewContainer.removeChild(UIPopupWindow.class) ;
      } else {
        uiViewTabPane.uiTabForm.save() ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();      
      uiViewTabPane.uiTabForm.refresh(true) ;
      uiViewTabPane.uiViewForm.refresh(true) ;
      uiViewTabPane.removeChildById(POPUP_PERMISSION) ;
      UIViewContainer uiViewContainer = uiViewTabPane.getAncestorOfType(UIViewContainer.class) ;
      uiViewContainer.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }
  
  static  public class ResetActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      uiViewTabPane.uiTabForm.refresh(true) ;
      if(uiViewTabPane.getRenderTabId().equalsIgnoreCase("UIViewForm")) {
        uiViewTabPane.uiViewForm.revertVersion() ;
        uiViewTabPane.uiViewForm.refresh(true) ;
        uiViewTabPane.setRenderedChild(UIViewForm.class) ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewTabPane.getParent()) ;
    }
  }

  static  public class EditTabActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      String tabName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiViewTabPane.setRenderTabId("UITabForm") ;
      uiViewTabPane.uiViewForm.editTab(tabName) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewTabPane.getParent()) ;
    }
  }
  
  static  public class DeleteTabActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      String tabName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiViewTabPane.setRenderTabId("UIViewForm") ;
      uiViewTabPane.uiViewForm.deleteTab(tabName) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewTabPane.getParent()) ;
    }
  }

  static  public class ChangeVersionActionListener extends EventListener<UIViewFormTabPane> {
    public void execute(Event<UIViewFormTabPane> event) throws Exception {
      UIViewFormTabPane uiViewTabPane = event.getSource();
      uiViewTabPane.uiViewForm.changeVersion() ;
      ((UIViewList)uiViewTabPane.getParent()).updateViewListGrid() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewTabPane.getParent()) ;
    }
  }
}