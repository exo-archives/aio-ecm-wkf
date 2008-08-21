/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.fastcontentcreator;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.jcr.ComponentSelector;
import org.exoplatform.ecm.jcr.JCRResourceResolver;
import org.exoplatform.ecm.jcr.UISelector;
import org.exoplatform.ecm.utils.SessionsUtils;
import org.exoplatform.ecm.utils.Utils;
import org.exoplatform.ecm.webui.component.UIJCRBrowser;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.upload.UploadService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormUploadInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Apr 24, 2007 11:56:19 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    events = {
      @EventConfig(listeners = UIFastContentCreatortForm.SaveActionListener.class),
      @EventConfig(listeners = UIFastContentCreatortForm.AddActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIFastContentCreatortForm.RemoveActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIFastContentCreatortForm.ShowComponentActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIFastContentCreatortForm.RemoveReferenceActionListener.class, confirm = "DialogFormField.msg.confirm-delete", phase = Phase.DECODE)
    }
)
public class UIFastContentCreatortForm extends UIDialogForm implements UISelector {

  private String documentType_ ;
  private JCRResourceResolver jcrTemplateResourceResolver_ ;

  public UIFastContentCreatortForm() throws Exception {
    setActions(new String[]{"Save"}) ;
  }

  public String getTemplate() {
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    String repository = getPortletPreferences().getValue(Utils.REPOSITORY, "") ;
    try {      
      if(SessionsUtils.isAnonim()) {
        return templateService.getTemplatePathByAnonymous(true, documentType_, repository);
      }
      return templateService.getTemplatePathByUser(true, documentType_, userName, repository) ;
    } catch (Exception e) {
      UIApplication uiApp = getAncestorOfType(UIApplication.class) ;
      Object[] arg = { documentType_ } ;
      uiApp.addMessage(new ApplicationMessage("UIFastContentCreatortForm.msg.not-support", arg, 
          ApplicationMessage.ERROR)) ;
      return null ;
    }
  }
  
  public void updateSelect(String selectField, String value) {
    this.isUpdateSelect = true ;
    getUIStringInput(selectField).setValue(value) ;
    UIFastContentCreatorPortlet uiContainer = getParent() ;
    uiContainer.removeChildById("PopupComponent") ;
  }

  public Node getCurrentNode() throws Exception {  
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    PortletPreferences preferences = getPortletPreferences() ;
    Session session = SessionProviderFactory.createSystemProvider().getSession(preferences.getValue("workspace", ""), 
        repositoryService.getRepository(preferences.getValue(Utils.REPOSITORY, ""))) ;
    return (Node) session.getItem(preferences.getValue("path", ""));
  }

  public void setTemplateNode(String type) { documentType_ = type ; }

  public boolean isEditing() { return false ; }

  @SuppressWarnings("unused")
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    if(jcrTemplateResourceResolver_ == null) newJCRTemplateResourceResolver() ; 
    return jcrTemplateResourceResolver_; 
  }

  private PortletPreferences getPortletPreferences() {
    PortletRequestContext portletContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance() ;
    return portletContext.getRequest().getPreferences() ;
  }

  public void newJCRTemplateResourceResolver() {
    PortletPreferences preferences = getPortletPreferences();       
    try {
      String repositoryName = preferences.getValue(Utils.REPOSITORY, "") ;
      String workspaceName = preferences.getValue("workspace", "") ;    
      ManageableRepository manageableRepository = 
        getApplicationComponent(RepositoryService.class).getRepository(repositoryName) ;
      Session session = SessionProviderFactory.createSystemProvider().getSession(workspaceName,manageableRepository) ;
      jcrTemplateResourceResolver_ = new JCRResourceResolver(session, "exo:templateFile") ;
    } catch(Exception e) { }
  }
  
  @SuppressWarnings("unchecked")
  static public class SaveActionListener extends EventListener<UIFastContentCreatortForm> {
    public void execute(Event<UIFastContentCreatortForm> event) throws Exception {
      UIFastContentCreatortForm uiForm = event.getSource() ;
      CmsService cmsService = uiForm.getApplicationComponent(CmsService.class) ;
      RepositoryService repositoryService = uiForm.getApplicationComponent(RepositoryService.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      PortletPreferences preferences = uiForm.getPortletPreferences() ;
      String repository = preferences.getValue(Utils.REPOSITORY, "") ;
      String prefLocate = preferences.getValue("path", "") ;
      String prefType = preferences.getValue("type", "") ;
      String workspace = preferences.getValue("workspace", "") ;
      Session session = null ;
      try {
        session = repositoryService.getRepository(repository).login(workspace) ;
      } catch(Exception e) {
        session = repositoryService.getRepository(repository).getSystemSession(workspace) ;
      }
      Map inputProperties = Utils.prepareMap(uiForm.getChildren(), uiForm.getInputProperties()) ;
      Node homeNode = null;
      Node newNode = null ;
      try {
        homeNode = (Node) session.getItem(prefLocate);
      } catch (AccessDeniedException ade){
        Object[] args = { prefLocate } ;
        uiApp.addMessage(new ApplicationMessage("UIFastContentCreatortForm.msg.access-denied", args, 
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return;
      } catch(PathNotFoundException pnfe) {
        Object[] args = { prefLocate } ;
        uiApp.addMessage(new ApplicationMessage("UIFastContentCreatortForm.msg.path-not-found", args, 
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return;
      }

      try {
        String addedPath = cmsService.storeNode(prefType, homeNode, inputProperties, true, repository);
        homeNode.getSession().save() ;
        if(homeNode.hasNode(addedPath.substring(addedPath.lastIndexOf("/") + 1))) {
          newNode = homeNode.getNode(addedPath.substring(addedPath.lastIndexOf("/") + 1)) ; 
        }
        uiForm.reset() ;
        uiForm.setIsResetForm(true) ;
        for(UIComponent uiChild : uiForm.getChildren()) {
          if(uiChild instanceof UIFormMultiValueInputSet) {
            ((UIFormMultiValueInputSet)uiChild).setValue(new ArrayList<Value>()) ;
          } else if(uiChild instanceof UIFormUploadInput) {
            UploadService uploadService = uiForm.getApplicationComponent(UploadService.class) ;
            uploadService.removeUpload(((UIFormUploadInput)uiChild).getUploadId()) ;
          }
        }
        session.save() ;
        session.refresh(false) ;
        homeNode.getSession().refresh(false) ;
        Object[] args = { prefLocate } ;
        uiApp.addMessage(new ApplicationMessage("UIFastContentCreatortForm.msg.saved-successfully", args)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
      } catch (AccessControlException ace) {
        throw new AccessDeniedException(ace.getMessage());
      } catch(VersionException ve) {
        uiApp.addMessage(new ApplicationMessage("UIFastContentCreatortForm.msg.in-versioning", null, 
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return;
      } catch(AccessDeniedException e) {
        Object[] args = { prefLocate } ;
        String key = "UIFastContentCreatortForm.msg.access-denied" ;
        uiApp.addMessage(new ApplicationMessage(key, args, ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return;
      } catch(LockException lock) {
        Object[] args = { prefLocate } ;
        String key = "UIFastContentCreatortForm.msg.node-locked" ;
        uiApp.addMessage(new ApplicationMessage(key, args, ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return;
      } catch(ItemExistsException item) {
        Object[] args = { prefLocate } ;
        String key = "UIFastContentCreatortForm.msg.node-isExist" ;
        uiApp.addMessage(new ApplicationMessage(key, args, ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      } finally {
        if(session != null) {
          session.logout();
        }
      }
      event.getRequestContext().setAttribute("nodePath",newNode.getPath());
    }
  }  
    
  static public class ShowComponentActionListener extends EventListener<UIFastContentCreatortForm> {
    public void execute(Event<UIFastContentCreatortForm> event) throws Exception {
      UIFastContentCreatortForm uiForm = event.getSource() ;
      UIFastContentCreatorPortlet uiContainer = uiForm.getParent() ;
      uiForm.isShowingComponent = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Map fieldPropertiesMap = uiForm.componentSelectors.get(fieldName) ;
      String classPath = (String)fieldPropertiesMap.get("selectorClass") ;
      ClassLoader cl = Thread.currentThread().getContextClassLoader() ;
      Class clazz = Class.forName(classPath, true, cl) ;
      UIComponent uiComp = uiContainer.createUIComponent(clazz, null, null);
      if(uiComp instanceof UIJCRBrowser) {
        PortletPreferences preferences = uiForm.getPortletPreferences() ;
        String repositoryName = preferences.getValue("repository", "") ;
        SessionProvider provider = SessionsUtils.getSystemProvider() ;                
        ((UIJCRBrowser)uiComp).setRepository(repositoryName) ;
        ((UIJCRBrowser)uiComp).setSessionProvider(provider) ;
        String wsFieldName = (String)fieldPropertiesMap.get("workspaceField") ;
        if(wsFieldName != null && wsFieldName.length() > 0) {
          String wsName = (String)uiForm.<UIFormInputBase>getUIInput(wsFieldName).getValue() ;
          ((UIJCRBrowser)uiComp).setIsDisable(wsName, true) ;      
        }
        String selectorParams = (String)fieldPropertiesMap.get("selectorParams") ;
        if(selectorParams != null) {
          String[] arrParams = selectorParams.split(",") ;
          if(arrParams.length == 4) {
            ((UIJCRBrowser)uiComp).setFilterType(new String[] {Utils.NT_FILE}) ;
            ((UIJCRBrowser)uiComp).setRootPath(arrParams[2]) ;
            if(arrParams[3].indexOf(";") > -1) {
              ((UIJCRBrowser)uiComp).setMimeTypes(arrParams[3].split(";")) ;
            } else {
              ((UIJCRBrowser)uiComp).setMimeTypes(new String[] {arrParams[3]}) ;
            }
          }
        }
      }
      uiContainer.initPopup(uiComp) ;
      String param = "returnField=" + fieldName ;
      ((ComponentSelector)uiComp).setComponent(uiForm, new String[]{param}) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer) ;
    }
  }      
  
  static public class RemoveReferenceActionListener extends EventListener<UIFastContentCreatortForm> {
    public void execute(Event<UIFastContentCreatortForm> event) throws Exception {
      UIFastContentCreatortForm uiForm = event.getSource() ;
      uiForm.isRemovePreference = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiForm.getUIStringInput(fieldName).setValue(null) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }  

  static public class AddActionListener extends EventListener<UIFastContentCreatortForm> {
    public void execute(Event<UIFastContentCreatortForm> event) throws Exception {
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getParent()) ;
    }
  }

  static public class RemoveActionListener extends EventListener<UIFastContentCreatortForm> {
    public void execute(Event<UIFastContentCreatortForm> event) throws Exception {
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getParent()) ;
    }
  }  
}
