/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.jcr.UIPopupComponent;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * July 3, 2006
 * 10:07:15 AM
 * Edit: lxchiati 2006/10/16
 * Edit: phamtuan Oct 27, 2006
 */

@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/popup/info/UIReferencesList.gtmpl",
    events = { @EventConfig (listeners = UIReferencesList.CloseActionListener.class)}
)

public class UIReferencesList extends UIGrid implements UIPopupComponent{

  private static String[] REFERENCES_BEAN_FIELD = {"workspace", "path"} ;

  public UIReferencesList() throws Exception {}

  public void activate() throws Exception {
    configure("workspace", REFERENCES_BEAN_FIELD, null) ;
    updateGrid() ;
  }  

  public void deActivate() {}

  public void updateGrid() throws Exception {
    ObjectPageList objPageList = new ObjectPageList(getReferences(), 10) ;
    getUIPageIterator().setPageList(objPageList) ; 
  }

  private List<ReferenceBean> getReferences() throws Exception {
    List<ReferenceBean> referBeans = new ArrayList<ReferenceBean>() ; 
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    Node currentNode = uiJCRExplorer.getCurrentNode() ;
    String uuid = currentNode.getUUID() ;        
    String repositoryName = uiJCRExplorer.getRepositoryName() ;
    ManageableRepository repository = repositoryService.getRepository(repositoryName) ;
    Session session = null ;
    for(String workspace : repository.getWorkspaceNames()) {
      session = repository.getSystemSession(workspace) ;
      try{
        Node lookupNode = session.getNodeByUUID(uuid) ;
        PropertyIterator iter = lookupNode.getReferences() ;
        if(iter != null) {
          while(iter.hasNext()) {
            Node refNode = iter.nextProperty().getParent() ;
            referBeans.add(new ReferenceBean(workspace, refNode.getPath())) ;
          }
        }
      } catch(Exception e) { }
     session.logout() ; 
    }
    return referBeans ;
  }

  static public class CloseActionListener extends EventListener<UIReferencesList> {
    public void execute(Event<UIReferencesList> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }

  public class ReferenceBean {    
    private String workspace_ ;
    private String path_ ;

    public ReferenceBean ( String workspace, String path) {
      workspace_ = workspace ;
      path_ = path ;
    }

    public String getPath() { return path_ ;}
    public void setPath(String path) { this.path_ = path ;}

    public String getWorkspace() { return workspace_ ;}
    public void setWorkspace(String workspace) { this.workspace_ = workspace ;}
  }
}