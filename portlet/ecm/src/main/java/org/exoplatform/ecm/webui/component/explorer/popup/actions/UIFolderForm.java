/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.jcr.ECMNameValidator;
import org.exoplatform.ecm.jcr.JCRExceptionManager;
import org.exoplatform.ecm.jcr.UIPopupComponent;
import org.exoplatform.ecm.utils.Utils;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.component.UIApplication;
import org.exoplatform.webui.component.UIForm;
import org.exoplatform.webui.component.UIFormSelectBox;
import org.exoplatform.webui.component.UIFormStringInput;
import org.exoplatform.webui.component.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.component.model.SelectItemOption;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/UIFormWithOutTitle.gtmpl",
    events = {
      @EventConfig(listeners = UIFolderForm.SaveActionListener.class),
      @EventConfig(listeners = UIFolderForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)

public class UIFolderForm extends UIForm implements UIPopupComponent {
  final static public String FIELD_NAME = "name" ;
  final static public String FIELD_TYPE = "type" ;

  public UIFolderForm() throws Exception {
    addUIFormInput(new UIFormSelectBox(FIELD_TYPE, FIELD_TYPE, getOptions())) ;
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).addValidator(ECMNameValidator.class)) ;
    setActions(new String[]{"Save", "Cancel"}) ;
  }

  public List<SelectItemOption<String>> getOptions() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance() ;
    PortletPreferences preferences = context.getRequest().getPreferences() ;
    String folderDisplay = preferences.getValue(Utils.DRIVE_FOLDER, "") ;
    if(folderDisplay.indexOf(",") > -1) {
      for(String folder : folderDisplay.split(",")) {
        options.add(new SelectItemOption<String>(folder, folder)) ;
      }
    } else {
      options.add(new SelectItemOption<String>(folderDisplay, folderDisplay)) ;
    }
    return options ;
  }

  static  public class SaveActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      UIFolderForm uiFolderForm = event.getSource() ;
      UIJCRExplorer uiExplorer = uiFolderForm.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiFolderForm.getAncestorOfType(UIApplication.class);
      String name = uiFolderForm.getUIStringInput(FIELD_NAME).getValue() ;
      Node node = uiExplorer.getCurrentNode() ;
      if(node.isLocked()) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      if(name != null) {
        String type = uiFolderForm.getUIFormSelectBox(FIELD_TYPE).getValue() ;
        try {
          node.addNode(name, type) ;
          node.save() ;
          node.getSession().refresh(false) ;
          if(!uiExplorer.getPreference().isJcrEnable())uiExplorer.getSession().save() ;
          uiExplorer.updateAjax(event) ;
        }catch(ConstraintViolationException cve) {  
          Object[] arg = { type } ;
          uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.constraint-violation", arg, 
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }catch(RepositoryException re) {
          uiApp.addMessage(new ApplicationMessage(re.getMessage(), null, 
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        } 
        catch (Exception e) {
          JCRExceptionManager.process(uiApp, e);
        }
      } else {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.name-invalid", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }

  public void activate() throws Exception { getUIStringInput(FIELD_NAME).setValue(null) ;}

  public void deActivate() throws Exception {}
}

