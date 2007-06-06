/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.action;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.nodetype.NodeType;

import org.exoplatform.services.cms.actions.ActionPlugin;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.component.UIApplication;
import org.exoplatform.webui.component.UIForm;
import org.exoplatform.webui.component.UIFormCheckBoxInput;
import org.exoplatform.webui.component.UIFormMultiValueInputSet;
import org.exoplatform.webui.component.UIFormSelectBox;
import org.exoplatform.webui.component.UIFormStringInput;
import org.exoplatform.webui.component.UIPopupWindow;
import org.exoplatform.webui.component.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.component.model.SelectItemOption;
import org.exoplatform.webui.component.validator.EmptyFieldValidator;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;

/**
 * Created by The eXo Platform SARL 
 * Author : pham tuan
 * phamtuanchip@yahoo.de September 20, 2006 04:27:15 PM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/component/UIForm.gtmpl", 
    events = {
      @EventConfig(listeners = UIActionTypeForm.SaveActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIActionTypeForm.ChangeTypeActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIActionTypeForm.CancelActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIActionTypeForm.AddActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIActionTypeForm.RemoveActionListener.class)
    }
)
public class UIActionTypeForm extends UIForm {

  final static public String FIELD_ACTIONTYPE = "actionType" ;
  final static public String FIELD_EXECUTEACTION = "executeAction" ;
  final static public String FIELD_NAME = "name" ;
  final static public String FIELD_ISMOVE = "isMove" ;
  final static public String FIELD_VARIABLES = "variables" ;

  public UIFormSelectBox actionExecutables ;
  public UIFormMultiValueInputSet uiFormMultiValue = null ;

  public UIActionTypeForm() throws Exception {
    List<SelectItemOption<String>> actionOptions = new ArrayList<SelectItemOption<String>>() ;
    UIFormSelectBox actionType = 
      new UIFormSelectBox(FIELD_ACTIONTYPE, FIELD_ACTIONTYPE, actionOptions) ;
    actionType.setOnChange("ChangeType") ;
    addUIFormInput(actionType) ;
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).
        addValidator(EmptyFieldValidator.class)) ;
    addUIFormInput(new UIFormCheckBoxInput<Boolean>(FIELD_ISMOVE, FIELD_ISMOVE, null)) ;
    List<SelectItemOption<String>> executableOptions = new ArrayList<SelectItemOption<String>>() ;
    actionExecutables = new UIFormSelectBox(FIELD_EXECUTEACTION,FIELD_EXECUTEACTION, 
                                            executableOptions);
    addUIFormInput(actionExecutables) ;
    setActions( new String[]{"Save", "Cancel"}) ;
  }

  private List<SelectItemOption<String>> getActionTypesValues() throws Exception { 
    ActionServiceContainer actionServiceContainer = 
      getApplicationComponent(ActionServiceContainer.class) ;
    List <String> actionsTypes= (List <String>) actionServiceContainer.getActionPluginNames();
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    for(int i =0 ; i< actionsTypes.size() ; i++){     
      options.add(new SelectItemOption<String>(actionsTypes.get(i),actionsTypes.get(i))) ;
    }
    return options ;
  }

  private void initMultiValuesField() throws Exception {
    if( uiFormMultiValue != null ) removeChildById(FIELD_VARIABLES);
    uiFormMultiValue = createUIComponent(UIFormMultiValueInputSet.class, null, null) ;
    uiFormMultiValue.setId(FIELD_VARIABLES) ;
    uiFormMultiValue.setName(FIELD_VARIABLES) ;
    uiFormMultiValue.setType(UIFormStringInput.class) ;
    List<String> list = new ArrayList<String>() ;
    list.add("");
    uiFormMultiValue.setValue(list) ;
    addUIFormInput(uiFormMultiValue) ;
  }

  @SuppressWarnings("unchecked")
  private List<SelectItemOption<String>> getExecutableOptions(String actionTypeName) throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    ActionServiceContainer actionServiceContainer = 
      getApplicationComponent(ActionServiceContainer.class) ;    
    ActionPlugin actionPlugin = actionServiceContainer.getActionPluginForActionType(actionTypeName) ;
    List<String> executables = (List)actionPlugin.getActionExecutables();
    for(String actionExec : executables) {
      options.add(new SelectItemOption<String>(actionExec, actionExec)) ;
    } 
    return options ;
  }

  public void refresh() throws Exception{
    reset() ;
    List<SelectItemOption<String>> actionOptions = getActionTypesValues() ;
    String actionTypeName = actionOptions.get(0).getValue() ;
    getUIFormSelectBox(FIELD_ACTIONTYPE).setOptions(actionOptions) ;
    getUIFormSelectBox(FIELD_ACTIONTYPE).setValue(actionTypeName) ;
    getUIStringInput(FIELD_NAME).setValue("") ;
    getUIFormCheckBoxInput(FIELD_ISMOVE).setChecked(false) ;
    List<SelectItemOption<String>> executableOptions = getExecutableOptions(actionTypeName) ;
    actionExecutables.setOptions(executableOptions) ;
    actionExecutables.setName(actionTypeName.replace(":", "_")) ;
    initMultiValuesField() ;
  }

  static public class ChangeTypeActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource() ;
      String actionTypeName = uiForm.getUIFormSelectBox(FIELD_ACTIONTYPE).getValue() ;
      uiForm.actionExecutables.setName(actionTypeName.replace(":", "_")) ;
      uiForm.actionExecutables.setOptions(uiForm.getExecutableOptions(actionTypeName));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }

  @SuppressWarnings("unused")
  static public class SaveActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource() ;
      UIActionManager uiActionManager = uiForm.getAncestorOfType(UIActionManager.class) ;
      ActionServiceContainer actionServiceContainer = 
        uiForm.getApplicationComponent(ActionServiceContainer.class) ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      String selectValue = uiForm.getUIFormSelectBox(FIELD_ACTIONTYPE).getValue() ;
      System.out.println("\n\nselectvalue====?>" +selectValue+ "\n\n");
      String actionName = uiForm.getUIStringInput(FIELD_NAME).getValue();
      Object[] args = {actionName} ;
      if(!actionName.startsWith("exo:")) { 
        uiApp.addMessage(new ApplicationMessage("UIActionTypeForm.msg.action-name-invalid", args,
                                                 ApplicationMessage.WARNING)) ; 
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }      
      List<String> variables = new ArrayList<String>();     
      List values = uiForm.uiFormMultiValue.getValue();
      if(values != null && values.size() > 0) {
        for(Object value : values) {
          variables.add((String)value) ;
        }
      }
      for(NodeType nodeType : actionServiceContainer.getCreatedActionTypes()) {
        if(actionName.equals(nodeType.getName())) {
          uiApp.addMessage(new ApplicationMessage("UIActionTypeForm.msg.action-exist", null)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }
      try {
        boolean isMove = uiForm.getUIFormCheckBoxInput(FIELD_ISMOVE).isChecked() ;
        String execute = uiForm.actionExecutables.getValue() ;
        System.out.println("\n\nselectvalue====?>" +selectValue+ "\n\n");
        System.out.println("\n\nsexecute====?>" +execute+ "\n\n");
        actionServiceContainer.createActionType(actionName, selectValue, execute, variables, isMove);
        uiActionManager.refresh() ;
        uiForm.refresh() ;
        uiActionManager.removeChild(UIPopupWindow.class) ;
      } catch(Exception e) {
        uiApp.addMessage(new ApplicationMessage("UIActionTypeForm.msg.action-type-create-error", args,
                                                ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionManager) ;
    }
  }
  
  static public class CancelActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource();
      uiForm.reset() ;
      UIActionManager uiActionManager = uiForm.getAncestorOfType(UIActionManager.class) ;
      uiActionManager.removeChild(UIPopupWindow.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionManager) ;
    }
  }
  
  static public class AddActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }
  
  static public class RemoveActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiForm = event.getSource();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }
}