/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.search;

import org.exoplatform.webui.component.UIContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;

/**
 * Created by The eXo Platform SARL
 * Author : le bien thuy  
 *          lebienthuyt@gmail.com
 * Oct 2, 2006
 * 10:08:51 AM 
 * Editor: pham tuan Oct 27, 2006
 */

@ComponentConfig( 
    template = "app:/groovy/webui/component/UIECMTabPane.gtmpl" 
)

public class UISearch extends UIContainer {
  public UISearch() throws Exception {
    addChild(UIMetadataSearch.class, null, null) ;
    addChild(UIJCRAdvancedSearch.class, null, null).setRendered(false);
    addChild(UISavedQuery.class, null, null).setRendered(false) ;
    addChild(UISearchResult.class, null, null).setRendered(false) ;
  }
}