/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.workflow.webui.component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 15, 2009  
 */
public class JCRResourceResolver extends ResourceResolver {      
  protected String repository ; 
  protected String workspace ;      
  protected String propertyName ;

  /**
   * Instantiates a new jCR resource resolver 
   * to load template that stored as a property of node in jcr
   * 
   * @param repository the repository
   * @param workspace the workspace
   * @param propertyName the property name
   */
  public JCRResourceResolver(String repository,String workspace,String propertyName) {
    this.repository = repository ;
    this.workspace = workspace;    
    this.propertyName = propertyName ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getResource(java.lang.String)
   */
  @SuppressWarnings("unused")
  public URL getResource(String url) throws Exception {
    throw new Exception("This method is not  supported") ;  
  }

  /** 
   * @param url URL must be like jcr:path with path is node path 
   * @see org.exoplatform.resolver.ResourceResolver#getInputStream(java.lang.String)
   */
  public InputStream getInputStream(String url) throws Exception  {
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    RepositoryService repositoryService = 
      (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class) ;
    ManageableRepository manageableRepository = repositoryService.getRepository(repository) ;
    //Use system session to access jcr resource
    SessionProvider provider = SessionProviderFactory.createSystemProvider();
    Session session = provider.getSession(workspace,manageableRepository);
    Node node = (Node)session.getItem(removeScheme(url)) ;
    return new ByteArrayInputStream(node.getProperty(propertyName).getString().getBytes()) ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getResources(java.lang.String)
   */
  @SuppressWarnings("unused")
  public List<URL> getResources(String url) throws Exception {
    throw new Exception("This method is not  supported") ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getInputStreams(java.lang.String)
   */
  public List<InputStream> getInputStreams(String url) throws Exception {
    ArrayList<InputStream>  inputStreams = new ArrayList<InputStream>(1) ;
    inputStreams.add(getInputStream(url)) ;
    return inputStreams ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#isModified(java.lang.String, long)
   */
  @SuppressWarnings("unused")
  public boolean isModified(String url, long lastAccess) {  return false ; }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#createResourceId(java.lang.String)
   */
  public String createResourceId(String url) { return url ; }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getResourceScheme()
   */
  public String getResourceScheme() {  return "jcr:" ; }

}
