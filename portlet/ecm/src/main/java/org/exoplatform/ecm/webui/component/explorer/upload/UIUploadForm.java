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
package org.exoplatform.ecm.webui.component.explorer.upload;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.portlet.PortletPreferences;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIMultiLanguageForm;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIMultiLanguageManager;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.popup.UIPopupComponent;
import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectmany.UICategoriesSelector;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.categories.CategoriesService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.upload.UploadService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormUploadInput;

/**
 * Created by The eXo Platform SARL
 * Author : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * July 3, 2006
 * 10:07:15 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIUploadForm.SaveActionListener.class), 
      @EventConfig(listeners = UIUploadForm.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIUploadForm.AddTaxonomyActionListener.class),
      @EventConfig(listeners = UIUploadForm.RemoveTaxonomyActionListener.class)
    }
)

public class UIUploadForm extends UIForm implements UIPopupComponent, UISelectable {

  final static public String FIELD_NAME =  "name" ;
  final static public String FIELD_UPLOAD = "upload" ;  
  final static public String JCRCONTENT = "jcr:content";
  final static public String FIELD_TAXONOMY = "fieldTaxonomy";
  final static public String FIELD_LISTTAXONOMY = "fieldListTaxonomy";
  final static public String POPUP_TAXONOMY = "UIPopupTaxonomy";
  
  private boolean isMultiLanguage_ = false ;
  private String language_ = null ;
  private boolean isDefault_ = false ;
  private List<String> listTaxonomy = new ArrayList<String>();
  private List<String> listTaxonomy_Temp = new ArrayList<String>();
  private List<String> listTaxonomy_TempName = new ArrayList<String>();
  
  public UIUploadForm() throws Exception {
    setMultiPart(true) ;
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null)) ;
    UIFormUploadInput uiInput = new UIFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD) ;
    addUIFormInput(uiInput);
  }
  
  public void initFieldInput() throws Exception {
    CategoriesService categoriesService = getApplicationComponent(CategoriesService.class);
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    Node currentNode = uiExplorer.getCurrentNode();
    if (categoriesService.hasCategories(currentNode)) {
      Value[] values = currentNode.getProperty("exo:category").getValues();
      for (int i = 0; i < values.length; i++) {
        String path  = uiExplorer.getSession().getNodeByUUID(values[i].getString()).getPath();
        if (!listTaxonomy.contains(path)) listTaxonomy.add(path);
        if (!listTaxonomy_Temp.contains(path)) {
          listTaxonomy_Temp.add(path);
          listTaxonomy_TempName.add(cutPath(path));
        }
      }
    }
    UIFormInputSetWithAction uiInputAct = new UIFormInputSetWithAction("AddTaxonomy");
    uiInputAct.addUIFormInput(new UIFormInputInfo(FIELD_TAXONOMY, FIELD_TAXONOMY, null));
    uiInputAct.setActionInfo(FIELD_TAXONOMY, new String[] {"AddTaxonomy"});
    addUIComponentInput(uiInputAct);
    
    UIFormInputSetWithAction uiInputArray = new UIFormInputSetWithAction("ListTaxonomy");
    uiInputArray.addUIFormInput(new UIFormInputInfo(FIELD_LISTTAXONOMY, FIELD_LISTTAXONOMY, null));
    addUIComponentInput(uiInputArray);
  }
  
  private String cutPath(String path) {
    String[] array = path.split("/");
    String value = "";
    if (array.length > 4) {
      for (int i = 4; i < array.length; i++) {
        value += array[i]; 
        if (i < array.length - 1) value += "/";
      }
    } else value = path;
    return value;
  }
  
  public List<String> getListTaxonomy() {
    return listTaxonomy;
  }
  
  public List<String> getlistTaxonomy_Temp() {
    return listTaxonomy_Temp;
  }
  
  public List<String> getlistTaxonomy_TempName() {
    return listTaxonomy_TempName;
  }
  
  public void setListTaxonomy(List<String> listTaxonomyNew) {
    listTaxonomy = listTaxonomyNew;
  }
  
  public String[] getActions() { return new String[] {"Save", "Cancel"}; }

  public void setIsMultiLanguage(boolean isMultiLanguage, String language) { 
    isMultiLanguage_ = isMultiLanguage ;
    language_ = language ;
  }
  
  public void resetComponent() {
    removeChildById(FIELD_UPLOAD);
    addUIFormInput(new UIFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD));
  }  

  public boolean isMultiLanguage() { return isMultiLanguage_ ; }

  public void setIsDefaultLanguage(boolean isDefault) { isDefault_ = isDefault ; }

  private String getLanguageSelected() { return language_ ; }

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public void doSelect(String selectField, Object value) throws Exception {
    String valueTaxonomy = String.valueOf(value);
    String[] arrayTaxonomy = valueTaxonomy.split(",");
    if (arrayTaxonomy.length > 0) {
      if (arrayTaxonomy[0].startsWith("[")) arrayTaxonomy[0] = arrayTaxonomy[0].substring(1, arrayTaxonomy[0].length());
      if (arrayTaxonomy[arrayTaxonomy.length - 1].endsWith("]")) {
        arrayTaxonomy[arrayTaxonomy.length - 1] = arrayTaxonomy[arrayTaxonomy.length - 1].substring(0, arrayTaxonomy[arrayTaxonomy.length - 1].length() - 1);  
      }
      for (String taxonomy : arrayTaxonomy) {
        if (!listTaxonomy_Temp.contains(taxonomy.trim())) {
          listTaxonomy_Temp.add(taxonomy.trim());
          listTaxonomy_TempName.add(cutPath(taxonomy.trim()));
        }
      }
    }
    updateAdvanceTaxonomy();
    UIUploadManager uiUploadManager = getParent();
    uiUploadManager.removeChildById(POPUP_TAXONOMY);
  }
  
  private void updateAdvanceTaxonomy() { 
    UIFormInputSetWithAction inputInfor = getChildById("ListTaxonomy");
    inputInfor.setIsDeleteOnly(true);
    inputInfor.setListInfoField(FIELD_LISTTAXONOMY, listTaxonomy_TempName);
    String[] actionInfor = {"RemoveTaxonomy"};
    inputInfor.setActionInfo(FIELD_LISTTAXONOMY, actionInfor);
  }
  
  static  public class SaveActionListener extends EventListener<UIUploadForm> {
    public void execute(Event<UIUploadForm> event) throws Exception {
      UIUploadForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class) ;
      UIFormUploadInput input = (UIFormUploadInput)uiForm.getUIInput(FIELD_UPLOAD);
      CmsService cmsService = uiForm.getApplicationComponent(CmsService.class) ;
      CategoriesService categoriesService = uiForm.getApplicationComponent(CategoriesService.class);
      if(input.getUploadResource() == null) {
        uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-error", null, 
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      if(uiExplorer.getCurrentNode().isLocked()) {
        String lockToken = LockUtil.getLockToken(uiExplorer.getCurrentNode());
        if(lockToken != null) uiExplorer.getSession().addLockToken(lockToken);
      }
      PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
      PortletPreferences portletPref = pcontext.getRequest().getPreferences();
      String categoryMandatoryWhenFileUpload =  portletPref.getValue(Utils.CATEGORY_MANDATORY, "").trim();    
      if (categoryMandatoryWhenFileUpload.equalsIgnoreCase("true") && uiForm.getlistTaxonomy_Temp().size() == 0 && !uiExplorer.getCurrentNode().hasNode(JCRCONTENT)) {
        uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.taxonomyPath-error", null, 
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      String fileName = input.getUploadResource().getFileName();
      MultiLanguageService multiLangService = uiForm.getApplicationComponent(MultiLanguageService.class) ;
      if(fileName == null || fileName.length() == 0) {
          uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-error", null, 
                                                  ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
      }      
      String[] arrFilterChar = {"&", "$", "@", ":", "]", "[", "*", "%", "!", "+", "(", ")", "'", "#", ";", "}", "{"} ;
      for(String filterChar : arrFilterChar) {
        if (fileName.indexOf(filterChar) > -1) {
          uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-invalid", null, ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }

      InputStream inputStream = input.getUploadDataAsStream();
      String name = uiForm.getUIStringInput(FIELD_NAME).getValue();
      if (name == null) name = fileName;      
      else name = name.trim();
      for(String filterChar : arrFilterChar) {
        if(name.indexOf(filterChar) > -1) {
          uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-invalid", null, ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }
      MimeTypeResolver mimeTypeSolver = new MimeTypeResolver() ;
      String mimeType = mimeTypeSolver.getMimeType(fileName) ;
      Node selectedNode = uiExplorer.getCurrentNode();      
      boolean isExist = selectedNode.hasNode(name) ;
      String newNodeUUID = null;
      try {
        String pers = PermissionType.ADD_NODE + "," + PermissionType.SET_PROPERTY ;
        selectedNode.getSession().checkPermission(selectedNode.getPath(), pers);
        String[] arrayTaxonomy = new String[uiForm.getlistTaxonomy_Temp().size()];
        for (int i = 0; i < arrayTaxonomy.length; i++) {
          arrayTaxonomy[i] = uiForm.getlistTaxonomy_Temp().get(i);
        }
        if(uiForm.isMultiLanguage()) {
          ValueFactoryImpl valueFactory = (ValueFactoryImpl) uiExplorer.getSession().getValueFactory() ;
          Value contentValue = valueFactory.createValue(inputStream) ;
          multiLangService.addFileLanguage(selectedNode, name, contentValue, mimeType, uiForm.getLanguageSelected(), uiExplorer.getRepositoryName(), uiForm.isDefault_) ;
          uiExplorer.setIsHidePopup(true) ;
          UIMultiLanguageManager uiManager = uiForm.getAncestorOfType(UIMultiLanguageManager.class) ;
          UIMultiLanguageForm uiMultiForm = uiManager.getChild(UIMultiLanguageForm.class) ;
          uiMultiForm.doSelect(uiExplorer.getCurrentNode()) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
        } else {
          if(!isExist) {            
            Map<String,JcrInputProperty> inputProperties = new HashMap<String,JcrInputProperty>() ;            
            JcrInputProperty nodeInput = new JcrInputProperty() ;
            nodeInput.setJcrPath("/node") ;
            nodeInput.setValue(name) ;
            nodeInput.setMixintype("mix:i18n,mix:votable,mix:commentable") ;
            nodeInput.setType(JcrInputProperty.NODE) ;
            inputProperties.put("/node",nodeInput) ;

            JcrInputProperty jcrContent = new JcrInputProperty() ;
            jcrContent.setJcrPath("/node/jcr:content") ;
            jcrContent.setValue("") ;
            jcrContent.setMixintype("dc:elementSet") ;
            jcrContent.setNodetype(Utils.NT_RESOURCE) ;
            jcrContent.setType(JcrInputProperty.NODE) ;
            inputProperties.put("/node/jcr:content",jcrContent) ;

            JcrInputProperty jcrData = new JcrInputProperty() ;
            jcrData.setJcrPath("/node/jcr:content/jcr:data") ;            
            jcrData.setValue(inputStream) ;          
            inputProperties.put("/node/jcr:content/jcr:data",jcrData) ; 

            JcrInputProperty jcrMimeType = new JcrInputProperty() ;
            jcrMimeType.setJcrPath("/node/jcr:content/jcr:mimeType") ;
            jcrMimeType.setValue(mimeType) ;          
            inputProperties.put("/node/jcr:content/jcr:mimeType",jcrMimeType) ;

            JcrInputProperty jcrLastModified = new JcrInputProperty() ;
            jcrLastModified.setJcrPath("/node/jcr:content/jcr:lastModified") ;
            jcrLastModified.setValue(new GregorianCalendar()) ;
            inputProperties.put("/node/jcr:content/jcr:lastModified",jcrLastModified) ;

            JcrInputProperty jcrEncoding = new JcrInputProperty() ;
            jcrEncoding.setJcrPath("/node/jcr:content/jcr:encoding") ;
            jcrEncoding.setValue("UTF-8") ;
            inputProperties.put("/node/jcr:content/jcr:encoding",jcrEncoding) ;          
            String repository = uiForm.getAncestorOfType(UIJCRExplorer.class).getRepositoryName() ;
            newNodeUUID = cmsService.storeNodeByUUID(Utils.NT_FILE, selectedNode, inputProperties, true,repository) ;
            
            selectedNode.save() ;
            Node newNode = selectedNode.getSession().getNodeByUUID(newNodeUUID);
            categoriesService.addMultiCategory(newNode, arrayTaxonomy, uiExplorer.getRepositoryName());
            selectedNode.getSession().save() ;                        
          } else {
            Node node = selectedNode.getNode(name) ;
            if(!node.getPrimaryNodeType().isNodeType(Utils.NT_FILE)) {
              Object[] args = { name } ;
              uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.name-is-exist", args, 
                                                      ApplicationMessage.WARNING)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
              return ;
            }
            Node contentNode = node.getNode(Utils.JCR_CONTENT);
            if(node.isNodeType(Utils.MIX_VERSIONABLE)) {              
              if(!node.isCheckedOut()) node.checkout() ; 
              contentNode.setProperty(Utils.JCR_DATA, inputStream);
              contentNode.setProperty(Utils.JCR_MIMETYPE, mimeType);
              contentNode.setProperty(Utils.JCR_LASTMODIFIED, new GregorianCalendar());
              node.save() ;       
              node.checkin() ;
              node.checkout() ;
            }else {
              contentNode.setProperty(Utils.JCR_DATA, inputStream);              
            }
            if(node.isNodeType("exo:datetime")) {
              node.setProperty("exo:dateModified",new GregorianCalendar()) ;
            }
            categoriesService.addMultiCategory(node, arrayTaxonomy, uiExplorer.getRepositoryName());
            node.save();
          }
        }
        uiExplorer.getSession().save() ;
        UIUploadManager uiManager = uiForm.getParent() ;
        UIUploadContainer uiUploadContainer = uiManager.getChild(UIUploadContainer.class) ;
        UploadService uploadService = uiForm.getApplicationComponent(UploadService.class) ;
        UIFormUploadInput uiChild = uiForm.getChild(UIFormUploadInput.class) ;
        if(uiForm.isMultiLanguage_) {
          uiUploadContainer.setUploadedNode(selectedNode) ; 
        } else {
          Node newNode = null ;
          if(!isExist) {
            newNode = uiExplorer.getSession().getNodeByUUID(newNodeUUID) ;
          } else {
            newNode = selectedNode.getNode(name) ;
          }
          uiUploadContainer.setUploadedNode(newNode) ;
        }
        UIUploadContent uiUploadContent = uiManager.findFirstComponentOfType(UIUploadContent.class) ;
        double size = uploadService.getUploadResource(uiChild.getUploadId()).getEstimatedSize()/1024;
        String fileSize = Double.toString(size);     
        String[] arrValues = {fileName, name, fileSize +" Kb", mimeType} ;
        uiUploadContent.setUploadValues(arrValues) ;
        inputStream.close();
        
        List<String> listTaxonomyNew = new ArrayList<String>();
        for (String categoryPath : uiForm.getlistTaxonomy_Temp()) {
          listTaxonomyNew.add(categoryPath);
        }
        uiForm.setListTaxonomy(listTaxonomyNew);
        uploadService.removeUpload(uiChild.getUploadId()) ;
        uiManager.setRenderedChild(UIUploadContainer.class) ;
        uiExplorer.setIsHidePopup(true) ;
        uiExplorer.updateAjax(event) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
      } catch(ConstraintViolationException con) {
        Object[] args = {name, } ;
        throw new MessageException(new ApplicationMessage("UIUploadForm.msg.contraint-violation", 
                                                           args, ApplicationMessage.WARNING)) ;
      } catch(LockException lock) {
        throw new MessageException(new ApplicationMessage("UIUploadForm.msg.lock-exception", 
            null, ApplicationMessage.WARNING)) ;        
      } catch(AccessDeniedException ace) {
        throw new MessageException(new ApplicationMessage("UIActionBar.msg.access-add-denied", 
            null, ApplicationMessage.WARNING)) ; 
      } catch(Exception e) {
        e.printStackTrace() ;
        JCRExceptionManager.process(uiApp, e);
        return ;
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UIUploadForm> {
    public void execute(Event<UIUploadForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }
  
  static  public class AddTaxonomyActionListener extends EventListener<UIUploadForm> {
    public void execute(Event<UIUploadForm> event) throws Exception {
      UIUploadForm uiUploadForm = event.getSource();
      UIUploadManager uiUploadManager = uiUploadForm.getParent();
      UICategoriesSelector uiCategoriesSelector = uiUploadManager.createUIComponent(UICategoriesSelector.class, null, null);
      if (uiUploadForm.getListTaxonomy().size() == uiUploadForm.getlistTaxonomy_Temp().size()) {
        uiCategoriesSelector.setExistedCategoryList(uiUploadForm.getListTaxonomy());
      } else {
        uiCategoriesSelector.setExistedCategoryList(uiUploadForm.getlistTaxonomy_Temp());
      }
      uiCategoriesSelector.init();
      String param = "returnField=" + FIELD_TAXONOMY ;
      ((ComponentSelector)uiCategoriesSelector).setSourceComponent(uiUploadForm, new String[]{param}) ;
      uiUploadManager.removeChildById(POPUP_TAXONOMY);
      UIPopupWindow uiPopupWindow = uiUploadManager.addChild(UIPopupWindow.class, null, POPUP_TAXONOMY);
      uiPopupWindow.setWindowSize(700, 350);
      uiPopupWindow.setUIComponent(uiCategoriesSelector);
      uiPopupWindow.setRendered(true);
      uiPopupWindow.setShow(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadManager);
    }
  }
  
  static  public class RemoveTaxonomyActionListener extends EventListener<UIUploadForm> {
    public void execute(Event<UIUploadForm> event) throws Exception {
      int intIndex = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      UIUploadForm uiUploadForm = event.getSource();
      UIUploadManager uiUploadManager = uiUploadForm.getParent();
      uiUploadForm.getlistTaxonomy_Temp().remove(intIndex);
      uiUploadForm.getlistTaxonomy_TempName().remove(intIndex);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadManager);
    }
  }
}
