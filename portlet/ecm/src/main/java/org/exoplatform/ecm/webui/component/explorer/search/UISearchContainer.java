/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.search;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.ecm.jcr.UIPopupComponent;
import org.exoplatform.webui.component.UIContainer;
import org.exoplatform.webui.component.UIPopupWindow;
import org.exoplatform.webui.component.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.config.annotation.ComponentConfig;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 27, 2006  
 * 2:04:24 PM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UISearchContainer extends UIContainer implements UIPopupComponent{
  
  private String selectedValue_ = "dc:elementSet";
  
  public UISearchContainer() throws Exception {
    addChild(UISearch.class, null, null) ;
  }
  
  public void setSelectedValue(String selectedValue) { selectedValue_ = selectedValue ; }
  
  public void initMetadataPopup() throws Exception {
    removeChild(UIPopupWindow.class) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, null) ;
    UIMetadataSelectForm uiSelectForm = createUIComponent(UIMetadataSelectForm.class, null, null) ;
    UIMetadataSearch uiMetadata = findFirstComponentOfType(UIMetadataSearch.class) ;
    String properties = uiMetadata.getUIStringInput(UIMetadataSearch.TYPE_SEARCH).getValue() ;
    List<String> selected = new ArrayList<String> () ;
    if(properties != null && properties.length() > 0) {
      String[] array = properties.split(",") ;
      for(int i = 0; i < array.length; i ++) {
        selected.add(array[i].trim()) ;
      }
    }
    uiPopup.setUIComponent(uiSelectForm) ;
    uiPopup.setWindowSize(600, 500) ;
    uiSelectForm.renderProperties(selectedValue_) ;
    uiSelectForm.setMetadataOptions() ;
    uiPopup.setRendered(true) ;
    uiPopup.setShow(true) ;
  }

  public void activate() throws Exception {
    UISearch uiSearch = getChild(UISearch.class) ;
    UIJCRAdvancedSearch advanceSearch = uiSearch.getChild(UIJCRAdvancedSearch.class);
    advanceSearch.update();
    UISavedQuery uiQuery = uiSearch.getChild(UISavedQuery.class);
    uiQuery.updateGrid();
  }

  public void deActivate() throws Exception {
  }
}
