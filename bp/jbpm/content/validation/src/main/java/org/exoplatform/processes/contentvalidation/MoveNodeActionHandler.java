/*
 * Created on Mar 24, 2005
 */
package org.exoplatform.processes.contentvalidation;

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
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * @author benjaminmestrallet
 */
public class MoveNodeActionHandler implements ActionHandler {

  private static final long serialVersionUID = 1L;
  
  private boolean executed = false;

  public void execute(ExecutionContext context) {
    try {      
      if(executed)
        return;      
      executed = true;
      moveNode(context);      
    } catch (Exception e) {
      e.printStackTrace();
    } finally {         
      context.getToken().signal("move-done");
      context.getSchedulerInstance().cancel("publicationTimer", context.getToken());
    }
  }
  
  protected void moveNode(ExecutionContext context) throws Exception{
    String actionName = (String) context.getVariable("actionName");
    String nodePath = (String) context.getVariable("nodePath");
    String srcPath = (String) context.getVariable("srcPath");
    String srcWorkspace = (String) context.getVariable("srcWorkspace");
    String repository = (String) context.getVariable("repository");
    Date startDate = (Date) context.getVariable("startDate");
    Date endDate = (Date) context.getVariable("endDate");      
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService) container
        .getComponentInstanceOfType(RepositoryService.class);
    ActionServiceContainer actionServiceContainer = (ActionServiceContainer) container
        .getComponentInstanceOfType(ActionServiceContainer.class);     
    Session session = repositoryService.getRepository(repository).getSystemSession(srcWorkspace);
    Node actionableNode = (Node) session.getItem(srcPath);
    if(!actionableNode.isNodeType("exo:actionable")) {
    	  actionableNode = (Node) session.getItem(nodePath);
    } 
    Node actionNode = actionServiceContainer.getAction(actionableNode, actionName);
    String destWorkspace = actionNode.getProperty("exo:destWorkspace").getString();
    String destPath = actionNode.getProperty("exo:destPath").getString();
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

    CmsService cmsService = (CmsService) container.getComponentInstanceOfType(CmsService.class);
    
    String relPath = nodePath.substring(srcPath.length() + 1); 
    if(!relPath.startsWith("/"))
      relPath = "/" + relPath;
    relPath = relPath.replaceAll("\\[\\d*\\]", "");
    cmsService.moveNode(nodePath, srcWorkspace, destWorkspace, destPath + relPath, repository);
    session.logout();
  }

}