/*
 * Copyright 2001-2006 The eXo platform SARL All rights reserved.
 * Please look at license.txt in info directory for more license detail. 
 */
 
package org.exoplatform.portlets.jcrconsole;

import java.io.IOException;
import java.util.ArrayList;
import java.io.PrintWriter;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceURL;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;

import org.exoplatform.frameworks.jcr.cli.CliAppContext;
import org.exoplatform.services.command.impl.CommandService;
import org.exoplatform.services.jcr.RepositoryService;

public class JcrConsole extends GenericPortlet {

  //public static final String SESSION_CONTAINER = "exoplatform.exocontainer";
  
  //private static final String JSP_CODE = "/WEB-INF/console.jsp";
  
 
  protected void doView(RenderRequest renderRequest,
      RenderResponse renderResponse) throws PortletException, IOException {

    ExoContainer container = ExoContainerContext.getCurrentContainer();
    
    renderResponse.setContentType("text/html; charset=UTF-8");
    PortletContext context = getPortletContext();
    
    ResourceURL resourceURL = renderResponse.createResourceURL();
    String resourceString = resourceURL.toString();

    
    while (resourceString.indexOf("&amp;") != -1 )
    {
      resourceString = resourceString.replace("&amp;", "&");
    }
    
    System.out.println("resourceString:" + resourceString);
    
    PrintWriter w = renderResponse.getWriter();
    w.println("<SCRIPT LANGUAGE=\"JavaScript\" TYPE=\"text/javascript\" SRC=\"/jcr-console/scripts/console.js\"></SCRIPT>");
    w.println("<LINK REL=\"stylesheet\"  HREF=\"/jcr-console/styles/styles.css\" TYPE=\"text/css\">");
    w.println("<DIV ID=\"termDiv\" STYLE=\"position:relative; top:20px; left:100px;\"></DIV>");
    w.println("<SCRIPT LANGUAGE=\"JavaScript\">");
    w.println("var action =\"" + resourceString + "\";");
    w.println("termOpen();");
    w.println("</SCRIPT>");


    
  }
  
  
  private void parseQuery(String query, ArrayList params) {
    try {
      params.clear();
      if (query.indexOf("\"") == -1) {
        while (!query.equals("")) {
          String item = query.substring(0, (query.indexOf(" ") < 0) ? query.length() : query
              .indexOf(" "));
          params.add(item);
          query = query.substring(query.indexOf(item) + item.length());
          query = query.trim();
        }
      } else {
        while (!query.equals("")) {
          String item = "";
          if (query.startsWith("\"")) {
            item = query.substring(query.indexOf("\"") + 1, (query.indexOf("\"", 1) < 0) ? query
                .length() : query.indexOf("\"", 1));
          } else {
            item = query.substring(0, (query.indexOf(" ") < 0) ? query.length() : query
                .indexOf(" "));
          }
          item = item.trim();
          if (item != null && !(item.equals(""))) {
            params.add(item);
          }
          int index = query.indexOf(item) + item.length() + 1;
          if (query.length() > index) {
            query = query.substring(query.indexOf(item) + item.length() + 1);
            query = query.trim();
          } else {
            query = "";
          }
        }
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  

  public void processAction(ActionRequest actionRequest,
      ActionResponse actionResponse) throws PortletException, IOException {

  }
  
  
  
  public void serveResource (ResourceRequest resourceRequest, ResourceResponse resourceResponse)
  throws PortletException, IOException {
    
    CliAppContext context = (CliAppContext) resourceRequest.getAttribute("context");
    
    //CliAppContext context = null;
    
    ArrayList<String> params = new ArrayList<String>();
    String PARAMETERS_KEY = "parameterss";
    resourceResponse.setContentType("text/html");
    PrintWriter printWriter = resourceResponse.getWriter();
    try {
      ExoContainer container = ExoContainerContext.getCurrentContainer();

      
      
      String commandLine = resourceRequest.getParameter("myaction").trim();
      String commandFromCommandLine = commandLine.substring(0,
          (commandLine.indexOf(" ") < 0) ? commandLine.length() : commandLine.indexOf(" "));

      commandLine = commandLine.substring(commandLine.indexOf(commandFromCommandLine)
          + commandFromCommandLine.length());
      commandLine = commandLine.trim();
      CommandService cservice = (CommandService) container
          .getComponentInstanceOfType(CommandService.class);
      Catalog catalog = cservice.getCatalog("CLI");

      parseQuery(commandLine, params);

      if (context == null) {
        RepositoryService repService = (RepositoryService) container
            .getComponentInstanceOfType(RepositoryService.class);

        String workspace = repService.getRepository().getConfiguration().getDefaultWorkspaceName();
        
        context = new CliAppContext(repService.getRepository(), PARAMETERS_KEY);
        context.setCurrentWorkspace(workspace);
        context.setCurrentItem(context.getSession().getRootNode());
      }
      Command commandToExecute = catalog.getCommand(commandFromCommandLine);
      context.put(PARAMETERS_KEY, params);
      if (commandToExecute != null) {
          commandToExecute.execute(context);
          printWriter.print(context.getOutput());
      } else {
        printWriter.print("Command not found \n");
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("[ERROR] [jcr-concole] Can't execute command - " + e.getMessage());
      printWriter.print("Invalid command\n");
    }
    finally {
      resourceRequest.setAttribute("context", context);
    }

    

  }
  
}