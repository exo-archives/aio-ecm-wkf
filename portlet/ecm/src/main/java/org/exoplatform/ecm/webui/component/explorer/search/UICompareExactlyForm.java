/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.search;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.ecm.jcr.UIPopupComponent;
import org.exoplatform.ecm.webui.component.UIPopupAction;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.EmptyFieldValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 27, 2006  
 * 10:18:56 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/search/UICompareExactlyForm.gtmpl",
    events = {
      @EventConfig(listeners = UICompareExactlyForm.SelectActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UICompareExactlyForm.CancelActionListener.class)
    }    
)
public class UICompareExactlyForm extends UIForm implements UIPopupComponent {
  private static final String FILTER = "filter" ;
  private static final String RESULT = "result";
  private static final String TEMP_RESULT = "tempSel";
  
  public UICompareExactlyForm() throws Exception {}
  
  public void activate() throws Exception {
    List<SelectItemOption<String>> opts = new ArrayList<SelectItemOption<String>>();
    addUIFormInput(new UIFormStringInput(FILTER, FILTER, null)) ;
    addUIFormInput(new UIFormSelectBox(RESULT, RESULT, opts).setSize(15).addValidator(EmptyFieldValidator.class)) ;
    addUIFormInput(new UIFormSelectBox(TEMP_RESULT, TEMP_RESULT, opts)) ;
    
    UISearchContainer uiSearchContainer = getAncestorOfType(UISearchContainer.class);
    UIJCRExplorer uiExplorer = uiSearchContainer.getAncestorOfType(UIJCRExplorer.class);
    UIConstraintsForm uiConstraint = uiSearchContainer.findFirstComponentOfType(UIConstraintsForm.class);
    String prop = uiConstraint.getUIStringInput(UIConstraintsForm.PROPERTY1).getValue() ;
    String operator = uiConstraint.getUIFormSelectBox(UIConstraintsForm.EXACTLY_OPERATOR).getValue() ;
    String[] properties = {};
    if(prop.indexOf(",") > -1) properties = prop.split(",") ;
    String statement = "select * from nt:base where " ;
    String whereClause = "" ;
    if(properties.length > 0) {
      for(String pro : properties) {
        if(whereClause.length() == 0) whereClause = "("+pro+" is not null)" ;
        else whereClause = whereClause + " " + operator + " " + "("+ pro +" is not null)" ;
      }
      statement = statement + whereClause ;
    } else {
      statement = statement + ""+prop+" is not null" ;
    }
    QueryManager queryManager = uiExplorer.getSession().getWorkspace().getQueryManager() ;
    Query query = queryManager.createQuery(statement, Query.SQL) ;
    QueryResult result = query.execute() ;
    if(result != null){
      NodeIterator iter = result.getNodes() ;
      while(iter.hasNext()) {
        Node node = iter.nextNode() ;
        if(properties.length > 0) {
          for(String pro : properties) {
            if(node.hasProperty(pro)) {
              Property property = node.getProperty(pro) ;
              setPropertyResult(property, opts) ;
            }
          }
        } else {
          if(node.hasProperty(prop)) {
            Property property = node.getProperty(prop) ;
            setPropertyResult(property, opts) ;
          }
        }
      }
    }
  }
  public void deActivate() throws Exception {}

  public void setPropertyResult(Property property, List<SelectItemOption<String>> opts) throws Exception {
    if(property.getDefinition().isMultiple()) {
      Value[] values = property.getValues() ;
      for(Value value : values) {
        opts.add(new SelectItemOption<String>(value.getString(), value.getString())) ;
      }
    } else {
      Value value = property.getValue() ;
      opts.add(new SelectItemOption<String>(value.getString(), value.getString())) ;
    }
  }
  
  static  public class CancelActionListener extends EventListener<UICompareExactlyForm> {
    public void execute(Event<UICompareExactlyForm> event) throws Exception {
      UISearchContainer uiSearchContainer = event.getSource().getAncestorOfType(UISearchContainer.class) ;
      UIPopupAction uiPopup = uiSearchContainer.getChild(UIPopupAction.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }
  
  static  public class SelectActionListener extends EventListener<UICompareExactlyForm> {
    public void execute(Event<UICompareExactlyForm> event) throws Exception {
      UICompareExactlyForm uiForm = event.getSource() ;
      String value = uiForm.getUIFormSelectBox(RESULT).getValue();
      UIPopupAction uiPopupAction = uiForm.getAncestorOfType(UIPopupAction.class);
      UISearchContainer uiSearchContainer = uiPopupAction.getParent() ;
      UIConstraintsForm uiConstraintsForm =
        uiSearchContainer.findFirstComponentOfType(UIConstraintsForm.class) ;
      uiConstraintsForm.getUIStringInput(UIConstraintsForm.CONTAIN_EXACTLY).setValue(value) ;
      uiPopupAction.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConstraintsForm) ;
    }
  }
}
