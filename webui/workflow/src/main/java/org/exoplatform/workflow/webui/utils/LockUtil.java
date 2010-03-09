/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.workflow.webui.utils;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.lock.LockManagerImpl;

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
    httpSession.setAttribute(LockManagerImpl.class.getName(),lockedNodesInfo);
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
