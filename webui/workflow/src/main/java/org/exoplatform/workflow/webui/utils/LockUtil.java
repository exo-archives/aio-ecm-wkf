/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.workflow.webui.utils;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.lock.LockManager;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 15, 2009  
 */
public class LockUtil {
  public static void changeLockToken(String srcPath, Node newNode) throws Exception {
    PortalRequestContext requestContext = Util.getPortalRequestContext();
    HttpSession httpSession = requestContext.getRequest().getSession();
    String newKey = createLockKey(newNode);
    String oldKey = getOldLockKey(srcPath, newNode);
    Map<String,String> lockedNodesInfo = (Map<String,String>)httpSession.getAttribute(LockManager.class.getName());
    if(lockedNodesInfo.containsKey(oldKey)) {
      lockedNodesInfo.put(newKey, lockedNodesInfo.get(oldKey));
      lockedNodesInfo.remove(oldKey);
    }
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    httpSession.setAttribute(LockManager.class.getName(),lockedNodesInfo);
  }
  
  public static String createLockKey(Node node) throws Exception {
    StringBuffer buffer = new StringBuffer();
    Session session = node.getSession();
    String repositoryName = ((ManageableRepository)session.getRepository()).getConfiguration().getName();    
    buffer.append(repositoryName).append("/::/")
          .append(session.getWorkspace().getName()).append("/::/")
          .append(session.getUserID()).append(":/:")
          .append(node.getPath());      
    return buffer.toString();
  }
  
  public static String getOldLockKey(String srcPath, Node node) throws Exception {
    StringBuffer buffer = new StringBuffer();
    Session session = node.getSession();
    String repositoryName = ((ManageableRepository)session.getRepository()).getConfiguration().getName();    
    buffer.append(repositoryName).append("/::/")
          .append(session.getWorkspace().getName()).append("/::/")
          .append(session.getUserID()).append(":/:")
          .append(srcPath);      
    return buffer.toString();
  }
}
