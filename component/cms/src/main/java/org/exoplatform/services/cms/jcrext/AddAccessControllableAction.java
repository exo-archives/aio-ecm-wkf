/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.cms.jcrext;

import javax.jcr.Node;

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham	
 *          hoa.pham@exoplatform.com
 * Jul 12, 2007  
 */
public class AddAccessControllableAction implements Action{
  public boolean execute(Context context) throws Exception {
    Node node =(Node)context.get("currentItem") ;
    if(node.canAddMixin("exo:accessControllable")) {
      node.addMixin("exo:accessControllable") ;
    }
    return false;
  }

}
