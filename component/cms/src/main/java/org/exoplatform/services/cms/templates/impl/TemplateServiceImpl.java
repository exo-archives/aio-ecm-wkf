/*
 * Created on Mar 1, 2005
 */
package org.exoplatform.services.cms.templates.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.CmsConfigurationService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.SecurityService;
import org.picocontainer.Startable;

/**
 * @author benjaminmestrallet
 */
public class TemplateServiceImpl implements TemplateService, Startable {

  private RepositoryService repositoryService_;  
  private SecurityService securityService_;
  private String cmsTemplatesBasePath_ ;  
  private List<TemplatePlugin> plugins_ = new ArrayList<TemplatePlugin>();

  public TemplateServiceImpl(RepositoryService jcrService, CmsConfigurationService cmsConfigService,
      SecurityService securityService) throws Exception {    
    securityService_ = securityService;
    repositoryService_ = jcrService;
    cmsTemplatesBasePath_ = cmsConfigService.getJcrPath(BasePath.CMS_TEMPLATES_PATH) ;
  }

  public void start() {
    try {
      for(TemplatePlugin plugin : plugins_) {
        plugin.init() ;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stop() {}

  public void addTemplates(ComponentPlugin plugin) {
    if (plugin instanceof TemplatePlugin)  plugins_.add((TemplatePlugin) plugin);
  }

  public void init(String repository) throws Exception {    
    for(TemplatePlugin plugin : plugins_) {
      plugin.init(repository) ;
    }
  }

  public Node getTemplatesHome(String repository, SessionProvider provider) throws Exception {
    Session session = getSession(repository,provider) ;
    return (Node)session.getItem(cmsTemplatesBasePath_) ;
  }  

  public boolean isManagedNodeType(String nodeTypeName, String repository) throws Exception {
    SessionProvider provider = SessionProvider.createSystemProvider() ;
    Session session = getSession(repository,provider) ;
    Node systemTemplatesHome = (Node)session.getItem(cmsTemplatesBasePath_) ;
    boolean b = false ;
    if(systemTemplatesHome.hasNode(nodeTypeName)) {      
      b = true ;
    }
    provider.close() ;
    return b ;
  }

  public NodeIterator getAllTemplatesOfNodeType(boolean isDialog, String nodeTypeName, String repository,SessionProvider provider) throws Exception {
    Node nodeTypeHome = getTemplatesHome(repository,provider).getNode(nodeTypeName);
    if (isDialog)
      return nodeTypeHome.getNode(DIALOGS).getNodes();    
    return nodeTypeHome.getNode(VIEWS).getNodes();
  }

  public String getDefaultTemplatePath(boolean isDialog, String nodeTypeName) {
    if (isDialog)
      return cmsTemplatesBasePath_ + "/" + nodeTypeName + DEFAULT_DIALOGS_PATH;    
    return cmsTemplatesBasePath_   + "/" + nodeTypeName + DEFAULT_VIEWS_PATH;
  }

  public Node getTemplateNode(boolean isDialog, String nodeTypeName, String templateName, String repository,SessionProvider provider) throws Exception {
    String type = DIALOGS;
    if (!isDialog) type = VIEWS;
    Node nodeTypeNode = getTemplatesHome(repository,provider).getNode(nodeTypeName);
    return nodeTypeNode.getNode(type).getNode(templateName);
  }
  
  public String getTemplatePathByUser(boolean isDialog, String nodeTypeName, String userName, String repository) throws Exception {    
    Session session = getSession(repository) ;
    Node templateHomeNode = (Node)session.getItem(cmsTemplatesBasePath_) ;
    String type = DIALOGS;
    if (!isDialog) type = VIEWS;
    Node nodeTypeNode = templateHomeNode.getNode(nodeTypeName);
    NodeIterator templateIter = nodeTypeNode.getNode(type).getNodes();    
    while (templateIter.hasNext()) {
      Node node = templateIter.nextNode();
      Value[] roles = node.getProperty(EXO_ROLES_PROP).getValues();
      for (int i = 0; i < roles.length; i++) {
        String templateRole = roles[i].getString();
        if ("*".equals(templateRole)) {
          session.logout();
          return node.getPath();
        }else if(userName != null && userName.equals(templateRole)) {
          session.logout();
          return node.getPath();  
        }else if (userName != null && securityService_.hasMembershipInGroup(userName, templateRole)){
          session.logout();
          return node.getPath();
        } 
      }
    }
    session.logout();
    return null ;    
  }

  public String getTemplatePath(boolean isDialog, String nodeTypeName, String templateName, String repository) throws Exception {    
    Session session = getSession(repository) ;
    Node templateNode = getTemplateNode(session,isDialog, nodeTypeName, templateName, repository);
    String path = templateNode.getPath() ;
    session.logout();
    return path;
  }


  public String getTemplateLabel(String nodeTypeName, String repository)  throws Exception {
    SessionProvider provider = SessionProvider.createSystemProvider() ;
    Node templateHome = getTemplatesHome(repository,provider);
    Node nodeType = templateHome.getNode(nodeTypeName) ;
    String label = "" ;
    if(nodeType.hasProperty("label")) {
      label = nodeType.getProperty("label").getString() ;
    }
    provider.close() ;
    return label ;
  }

  public String getTemplate(boolean isDialog, String nodeTypeName, String templateName, String repository) throws Exception {
    Session session = getSession(repository) ;
    Node templateNode = getTemplateNode(session,isDialog, nodeTypeName, templateName, repository);
    String template = templateNode.getProperty(EXO_TEMPLATE_FILE_PROP).getString();
    session.logout();
    return template ;
  }

  public String getTemplateRoles(boolean isDialog, String nodeTypeName, String templateName, String repository) throws Exception {
    Session session = getSession(repository) ;    
    Node templateNode = getTemplateNode(session,isDialog, nodeTypeName, templateName, repository);
    Value[] values = templateNode.getProperty(EXO_ROLES_PROP).getValues() ;
    StringBuffer roles = new StringBuffer() ;
    for(int i = 0 ; i < values.length ; i ++ ){
      if(roles.length() > 0 )roles.append("; ") ;
      roles.append(values[i].getString()) ;
    }
    session.logout();
    return roles.toString();
  }
  
  private Node getTemplateNode(Session session, boolean isDialog, String nodeTypeName, String templateName, String repository) throws Exception {
    String type = DIALOGS;
    if (!isDialog) type = VIEWS;
    Node homeNode = (Node)session.getItem(cmsTemplatesBasePath_) ;
    Node nodeTypeNode = homeNode.getNode(nodeTypeName);
    return nodeTypeNode.getNode(type).getNode(templateName);
  }
  
  public void removeTemplate(boolean isDialog, String nodeTypeName, String templateName, String repository) throws Exception {
    Session session = getSession(repository) ;
    Node templatesHome = (Node)session.getItem(cmsTemplatesBasePath_) ;
    Node nodeTypeHome = templatesHome.getNode(nodeTypeName);
    Node specifiedTemplatesHome = null;
    if (isDialog) {
      specifiedTemplatesHome = nodeTypeHome.getNode(DIALOGS);
    } else {
      specifiedTemplatesHome = nodeTypeHome.getNode(VIEWS);
    }
    Node contentNode = specifiedTemplatesHome.getNode(templateName);
    contentNode.remove() ;
    nodeTypeHome.save() ;
    session.save();
    session.logout();
  }

  public void removeManagedNodeType(String nodeTypeName, String repository) throws Exception {
    Session session = getSession(repository) ;
    Node templatesHome = (Node)session.getItem(cmsTemplatesBasePath_) ;
    Node managedNodeType = templatesHome.getNode(nodeTypeName);
    managedNodeType.remove() ;
    templatesHome.save() ;
    session.save();
    session.logout();
  }

  public String addTemplate(boolean isDialog, String nodeTypeName, String label, boolean isDocumentTemplate, 
      String templateName, String[] roles, String templateFile, String repository) throws Exception {
    Session session = getSession(repository) ;
    Node templatesHome = (Node)session.getItem(cmsTemplatesBasePath_) ;
    Node nodeTypeHome = null;
    if (!templatesHome.hasNode(nodeTypeName)){
      nodeTypeHome = Utils.makePath(templatesHome, nodeTypeName, NT_UNSTRUCTURED);
      if(isDocumentTemplate){
        nodeTypeHome.setProperty(DOCUMENT_TEMPLATE_PROP, true) ;        
      }
      else 
        nodeTypeHome.setProperty(DOCUMENT_TEMPLATE_PROP, false) ;
      nodeTypeHome.setProperty(TEMPLATE_LABEL, label) ;
    } else {
      nodeTypeHome = templatesHome.getNode(nodeTypeName);
    }

    Node specifiedTemplatesHome = null;
    if (isDialog) {
      if (!nodeTypeHome.hasNode(DIALOGS)) {
        specifiedTemplatesHome = Utils.makePath(nodeTypeHome, DIALOGS, NT_UNSTRUCTURED);
      } else {
        specifiedTemplatesHome = nodeTypeHome.getNode(DIALOGS);
      }
    } else {
      if (!nodeTypeHome.hasNode(VIEWS)) {
        specifiedTemplatesHome = Utils.makePath(nodeTypeHome, VIEWS, NT_UNSTRUCTURED);
      } else {
        specifiedTemplatesHome = nodeTypeHome.getNode(VIEWS);
      }
    }

    Node contentNode = null;
    if (specifiedTemplatesHome.hasNode(templateName)) {
      contentNode = specifiedTemplatesHome.getNode(templateName); 
    } else {
      contentNode = specifiedTemplatesHome.addNode(templateName, EXO_TEMPLATE);
    }
    contentNode.setProperty(EXO_ROLES_PROP, roles);
    contentNode.setProperty(EXO_TEMPLATE_FILE_PROP, templateFile);

    templatesHome.save();
    session.save();
    session.logout();
    return contentNode.getPath() ;
  }

  public List<String> getDocumentTemplates(String repository) throws Exception {
    List<String> templates = new ArrayList<String>() ;
    Session session = getSession(repository) ;    
    Node templatesHome = (Node)session.getItem(cmsTemplatesBasePath_) ;    
    for(NodeIterator templateIter = templatesHome.getNodes(); templateIter.hasNext() ; ) {
      Node template = templateIter.nextNode() ;      
      if(template.getProperty(DOCUMENT_TEMPLATE_PROP).getBoolean())        
        templates.add(template.getName()) ;
    }
    session.logout();
    return templates ;
  }      

  private Session getSession(String repository) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository) ;
    String systemWorksapce = manageableRepository.getConfiguration().getDefaultWorkspaceName() ;
    return manageableRepository.getSystemSession(systemWorksapce) ;
  }
  
  private Session getSession(String repository,SessionProvider provider) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository) ;
    String systemWorksapce = manageableRepository.getConfiguration().getDefaultWorkspaceName() ;
    return provider.getSession(systemWorksapce,manageableRepository) ;
  }
}
