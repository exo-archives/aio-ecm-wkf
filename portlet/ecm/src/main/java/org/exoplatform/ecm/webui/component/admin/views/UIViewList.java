/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.utils.SessionsUtils;
import org.exoplatform.ecm.utils.Utils;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 19, 2006
 * 11:45:11 AM 
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/UIGridWithButton.gtmpl",
    events = {
        @EventConfig(listeners = UIViewList.DeleteActionListener.class, confirm = "UIViewList.msg.confirm-delete"),
        @EventConfig(listeners = UIViewList.EditInfoActionListener.class),
        @EventConfig(listeners = UIViewList.ViewActionListener.class),
        @EventConfig(listeners = UIViewList.AddViewActionListener.class)
    }
)

public class UIViewList extends UIGrid {
  
  final static public String[] ACTIONS = {"AddView"} ; 
  final static public String ST_VIEW = "ViewPopup" ;
  final static public String ST_EDIT = "EditPopup" ;
  final static public String ST_ADD = "AddPopup" ;
  private static String[] VIEW_BEAN_FIELD = {"name", "permissions", "tabList", "baseVersion"} ;
  private static String[] VIEW_ACTION = {"View","EditInfo","Delete"} ;
  
  public UIViewList() throws Exception {
    getUIPageIterator().setId("UIViewsGrid") ;
    configure("name", VIEW_BEAN_FIELD, VIEW_ACTION) ;
  }
  
  private String getBaseVersion(String name) throws Exception {
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    Node node = getApplicationComponent(ManageViewService.class).getViewByName(name, repository,SessionsUtils.getSessionProvider());
    if(!node.isNodeType(Utils.MIX_VERSIONABLE) || node.isNodeType(Utils.NT_FROZEN)) return "";
    return node.getBaseVersion().getName();    
  }
  
  public String[] getActions() { return ACTIONS ; }
  
  @SuppressWarnings("unchecked")
  public void updateViewListGrid() throws Exception {
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    List<ViewConfig> views = getApplicationComponent(ManageViewService.class).getAllViews(repository) ;
    List<ViewBean> viewBeans = new ArrayList<ViewBean>() ;
    for(ViewConfig view:views) {      
      List<String>tabsName = new ArrayList<String>() ;
      for(ViewConfig.Tab tab:view.getTabList()) {
        tabsName.add(tab.getTabName()) ;
      }
      ViewBean bean = new ViewBean(view.getName(),view.getPermissions(),tabsName) ;
      bean.setBaseVersion(getBaseVersion(view.getName())) ;
      viewBeans.add(bean) ;
    }      
    Collections.sort(viewBeans, new ViewComparator()) ;
    getUIPageIterator().setPageList(new ObjectPageList(viewBeans, 10)) ;    
  }
  
  static public class ViewComparator implements Comparator {
    public int compare(Object o1, Object o2) throws ClassCastException {
      String name1 = ((ViewBean) o1).getName() ;
      String name2 = ((ViewBean) o2).getName() ;
      return name1.compareToIgnoreCase(name2) ;
    }
  }
  
  public boolean canDelete(List drivers, String viewName) {
    for(Object driver : drivers){
      String views = ((DriveData)driver).getViews() ;
      for(String view: views.split(",")){
        if(viewName.equals(view.trim())) return false ;
      }
    }
    return true ;
  }
  
  static  public class AddViewActionListener extends EventListener<UIViewList> {
    public void execute(Event<UIViewList> event) throws Exception {
      UIViewList uiViewList = event.getSource() ;
      UIViewContainer uiViewContainer = uiViewList.getParent() ;
      uiViewContainer.removeChildById(UIViewList.ST_VIEW) ;
      uiViewContainer.removeChildById(UIViewList.ST_EDIT) ;      
      uiViewContainer.initPopup(UIViewList.ST_ADD) ;
      UIViewFormTabPane uiViewTabPane = 
        uiViewContainer.findFirstComponentOfType(UIViewFormTabPane.class) ;
      uiViewTabPane.reset() ;
      UIViewManager uiManager = uiViewList.getAncestorOfType(UIViewManager.class) ;
      uiManager.setRenderedChild(UIViewContainer.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }
  
  static  public class DeleteActionListener extends EventListener<UIViewList> {
    public void execute(Event<UIViewList> event) throws Exception {
      UIViewList viewList = event.getSource() ;
      String repository = viewList.getAncestorOfType(UIECMAdminPortlet.class)
                                           .getPreferenceRepository() ;
      viewList.setRenderSibbling(UIViewList.class) ;
      String viewName = event.getRequestContext().getRequestParameter(OBJECTID)  ;
      ManageDriveService manageDrive = viewList.getApplicationComponent(ManageDriveService.class) ;
      if(!viewList.canDelete(manageDrive.getAllDrives(repository), viewName)) {
        UIApplication app = viewList.getAncestorOfType(UIApplication.class) ;
        Object[] args = {viewName} ;
        app.addMessage(new ApplicationMessage("UIViewList.msg.template-in-use", args)) ; 
        return ;
      }
      viewList.getApplicationComponent(ManageViewService.class).removeView(viewName, repository) ;
      viewList.updateViewListGrid() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(viewList.getParent()) ;
    }
  }

  static  public class EditInfoActionListener extends EventListener<UIViewList> {
    public void execute(Event<UIViewList> event) throws Exception {
      UIViewList uiViewList = event.getSource() ;
      String repository = uiViewList.getAncestorOfType(UIECMAdminPortlet.class)
      .getPreferenceRepository() ;
      uiViewList.setRenderSibbling(UIViewList.class) ;
      String viewName = event.getRequestContext().getRequestParameter(OBJECTID) ;      
      Node viewNode = uiViewList.getApplicationComponent(ManageViewService.class)
                                .getViewByName(viewName, repository,SessionsUtils.getSessionProvider()) ;
      UIViewContainer uiViewContainer = uiViewList.getParent() ;
      uiViewContainer.removeChildById(UIViewList.ST_VIEW) ;
      uiViewContainer.removeChildById(UIViewList.ST_ADD) ;      
      uiViewContainer.initPopup(UIViewList.ST_EDIT) ;
      UIViewFormTabPane viewTabPane = uiViewContainer.findFirstComponentOfType(UIViewFormTabPane.class) ;
      UIViewForm viewForm = viewTabPane.getChild(UIViewForm.class) ;
      viewForm.refresh(true) ;
      viewForm.update(viewNode, false, null) ;
      if(viewForm.getUIFormCheckBoxInput(UIViewForm.FIELD_ENABLEVERSION).isChecked()) {
        viewForm.setActions(new String[]{"Save", "Restore", "Cancel"}, null) ;
      } else {
        viewForm.setActions(new String[]{"Save", "Cancel"}, null) ;
      }
      viewForm.setActionInfo(UIViewForm.FIELD_PERMISSION, new String[] {"AddPermission"}) ;
      viewTabPane.getChild(UITabForm.class).setActions(new String[]{"Save"}, null) ;
      viewTabPane.setRenderedChild(UIViewForm.class) ;
      UIViewManager uiManager = uiViewList.getAncestorOfType(UIViewManager.class) ;
      uiManager.setRenderedChild(UIViewContainer.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }
  
  static  public class ViewActionListener extends EventListener<UIViewList> {
    public void execute(Event<UIViewList> event) throws Exception {
      UIViewList uiViewList = event.getSource() ;
      String repository = uiViewList.getAncestorOfType(UIECMAdminPortlet.class)
      .getPreferenceRepository() ;
      uiViewList.setRenderSibbling(UIViewList.class) ;
      String viewName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node viewNode = uiViewList.getApplicationComponent(ManageViewService.class)
                                .getViewByName(viewName, repository,SessionsUtils.getSessionProvider()) ;
      UIViewContainer uiViewContainer = uiViewList.getParent() ;
      uiViewContainer.removeChildById(UIViewList.ST_EDIT) ;
      uiViewContainer.removeChildById(UIViewList.ST_ADD) ;      
      uiViewContainer.initPopup(UIViewList.ST_VIEW) ;
      UIViewFormTabPane uiViewTabPane = 
        uiViewContainer.findFirstComponentOfType(UIViewFormTabPane.class);
      UIViewForm uiViewForm = uiViewTabPane.getChild(UIViewForm.class) ;
      uiViewForm.refresh(false) ;
      uiViewForm.update(viewNode, true, null) ;
      uiViewForm.setActionInfo(UIViewForm.FIELD_PERMISSION, null) ;
      uiViewForm.setActions(new String[]{"Close"}, null) ;
      uiViewTabPane.getChild(UITabForm.class).setActions(null, null) ;
      uiViewTabPane.setRenderedChild(UIViewForm.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiViewContainer) ;
    }
  }
  
  public class ViewBean {    
    private String name ;
    private String permissions ;
    private String tabList ;
    private String baseVersion =  "";

    public ViewBean(String n, String per, List tabs) {
      name = n ;
      permissions = per ;
      StringBuilder str = new StringBuilder() ;
      for(int i = 0; i < tabs.size(); i++) {
        str.append(" [ ").append(tabs.get(i)).append(" ]");
      }
      tabList = str.toString() ;
    }
    
    public String getBaseVersion() { return baseVersion; }
    public void setBaseVersion(String s) { baseVersion = s;
    }
    public String getName() { return name; }
    public void setName(String s) { name = s; }
    
    public String getPermissions() { return permissions; }
    public void setPermissions(String s) { permissions = s; }
    
    public String getTabList() { return tabList; }
    public void setTabList(String ls) { tabList = ls; }
  }
}
