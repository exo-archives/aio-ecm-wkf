/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.component.UIContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 25, 2007  
 * 2:05:40 PM
 */
@ComponentConfig(
    events = {
      @EventConfig(listeners = UIViewMetadataTemplate.EditPropertyActionListener.class),
      @EventConfig(listeners = UIViewMetadataTemplate.CancelActionListener.class)
    }
)
public class UIViewMetadataTemplate extends UIContainer {

  private String documentType_ ;
  
  public UIViewMetadataTemplate() throws Exception {
  }

  public void setTemplateType(String type) {documentType_ = type ;}
  
  public String getViewTemplatePath() {    
    MetadataService metadataService = getApplicationComponent(MetadataService.class) ;
    try {
      return metadataService.getMetadataPath(documentType_, false) ;
    } catch (Exception e) {
      e.printStackTrace() ;
    } 
    return null ;
  }
  
  public String getTemplate() { return getViewTemplatePath() ; }
  
  @SuppressWarnings("unused")
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver();
  }

  public Node getViewNode(String nodeType) throws Exception { 
    return getAncestorOfType(UIJCRExplorer.class).getViewNode(nodeType) ;
  }
  
  public List<String> getMultiValues(Node node, String name) throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getMultiValues(node, name) ;
  }
  
  static public class EditPropertyActionListener extends EventListener<UIViewMetadataTemplate> {
    public void execute(Event<UIViewMetadataTemplate> event) throws Exception {
      UIViewMetadataTemplate uiViewTemplate = event.getSource() ;
      String nodeType = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIViewMetadataManager uiMetaManager = uiViewTemplate.getAncestorOfType(UIViewMetadataManager.class) ;
      uiMetaManager.initMetadataFormPopup(nodeType) ;
      UIViewMetadataContainer uiContainer = uiViewTemplate.getParent() ;
      uiContainer.setRenderedChild(nodeType) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer.getParent()) ;
    }
  }
  
  static public class CancelActionListener extends EventListener<UIViewMetadataTemplate> {
    public void execute(Event<UIViewMetadataTemplate> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }
}
