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

package hero.hook;

import hero.interfaces.BnNodeLocal;
import hero.interfaces.Constants;
import hero.interfaces.ProjectSessionLocal;
import hero.interfaces.ProjectSessionLocalHome;
import hero.interfaces.ProjectSessionUtil;
import hero.util.HeroHookException;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.jcr.RepositoryService;

/**
 * This Node Hook moves the document into the destination Workspace
 * 
 * Created by Bull R&D
 * @author Brice Revenant
 * Mar 16, 2006
 */
public class ContentValidationPublicationHook implements NodeHookI {

  /* (non-Javadoc)
   * @see hero.hook.NodeHookI#getMetadata()
   */
  public String getMetadata() {

    // Return Metadata information
    return Constants.Nd.BEFORETERMINATE;
  }

  public void beforeStart(Object arg0, BnNodeLocal arg1)
      throws HeroHookException {
  }

  public void afterStart(Object arg0, BnNodeLocal arg1)
      throws HeroHookException {
  }

  /* (non-Javadoc)
   * @see hero.hook.NodeHookI#beforeTerminate(java.lang.Object, hero.interfaces.BnNodeLocal)
   */
  public void beforeTerminate(Object obj, BnNodeLocal node)
      throws HeroHookException {
    
    ProjectSessionLocal projectSession = null;
    try {
      // Initialize Project Session
      ProjectSessionLocalHome projectSessionHome =
        ProjectSessionUtil.getLocalHome();
      projectSession = projectSessionHome.create();
      projectSession.initProject(node.getBnProject().getName());

      /*
       * This Hook may not have been invoked by an eXo Thread in case a
       * Deadline occured so it is needed to retrieve the Portal Container by
       * name.
       */
      // Retrieve Workflow properties
      String actionName =
        projectSession.getProperty("actionName").getTheValue();
      String nodePath =
        projectSession.getProperty("nodePath").getTheValue();
      String srcPath =
        projectSession.getProperty("srcPath").getTheValue();
      String srcWorkspace =
        projectSession.getProperty("srcWorkspace").getTheValue();
      String repository =
        projectSession.getProperty("repository").getTheValue();
      Date startDate = new Date (Long.parseLong(projectSession.
        getNodeProperty(node.getName(), "startDate").getTheValue()));
      Date endDate = new Date (Long.parseLong(projectSession.
        getNodeProperty(node.getName(), "endDate").getTheValue()));
    
      // Retrieve references to Services
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      RepositoryService repositoryService = (RepositoryService)
        container.getComponentInstanceOfType(RepositoryService.class);
      ActionServiceContainer actionServiceContainer = (ActionServiceContainer)
        container.getComponentInstanceOfType(ActionServiceContainer.class);
      CmsService cmsService = (CmsService)
        container.getComponentInstanceOfType(CmsService.class);

      // Open a JCR session
      Session session = repositoryService.getRepository(repository).
        getSystemSession(srcWorkspace);
      
      // Retrieve information from the Action that triggered the Worflow
      Node actionableNode = (Node) session.getItem(srcPath);
      if(!actionableNode.isNodeType("exo:actionable")) {
        actionableNode = (Node) session.getItem(nodePath);
      }
      Node actionNode = actionServiceContainer.getAction(actionableNode, actionName);
      String destWorkspace = actionNode.getProperty(
        "exo:destWorkspace").getString();
      String destPath = actionNode.getProperty("exo:destPath").getString();
      
      // Add a Mixin to the document and set publication properties
      Node srcNode = (Node) session.getItem(nodePath);
      srcNode.addMixin("exo:published");      
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(startDate);
      srcNode.setProperty("exo:startPublication", calendar);
      if (endDate != null) {
        calendar = new GregorianCalendar();
        calendar.setTime(endDate);
        srcNode.setProperty("exo:endPublication", calendar);
      }
      srcNode.save();

      // Move the Node to the target Workspace
      String relPath = nodePath.substring(srcPath.length() + 1); 
      if(!relPath.startsWith("/"))
        relPath = "/" + relPath;
      relPath = relPath.replaceAll("\\[\\d*\\]", "");
      cmsService.moveNode(nodePath,srcWorkspace,destWorkspace,destPath + relPath,repository );
      session.logout();
    }catch(Exception e) {
      // TODO Use logging system instead
      e.printStackTrace();
    }
    finally {
      try {
        projectSession.remove();
      }
      catch(Exception ignore) {
      }
    }
  }

  public void afterTerminate(Object arg0, BnNodeLocal arg1)
      throws HeroHookException {
  }

  public void anticipate(Object arg0, BnNodeLocal arg1)
      throws HeroHookException {
  }

  public void onCancel(Object arg0, BnNodeLocal arg1) throws HeroHookException {
  }

  public void onDeadline(Object arg0, BnNodeLocal arg1)
      throws HeroHookException {
  }

  public void onReady(Object arg0, BnNodeLocal arg1) throws HeroHookException {
  }
}
