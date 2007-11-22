package org.exoplatform.services.cms.drives.impl;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

public class ManageDrivePlugin extends BaseComponentPlugin {

  private static String WORKSPACE = "exo:workspace".intern() ;
  private static String PERMISSIONS = "exo:permissions".intern() ;
  private static String VIEWS = "exo:views".intern() ;
  private static String ICON = "exo:icon".intern() ;
  private static String PATH = "exo:path".intern() ;
  private static String VIEW_REFERENCES = "exo:viewPreferences".intern() ;
  private static String VIEW_NON_DOCUMENT = "exo:viewNonDocument".intern() ;
  private static String VIEW_SIDEBAR = "exo:viewSideBar".intern() ;
  private static String SHOW_HIDDEN_NODE = "exo:showHiddenNode".intern() ;
  private static String ALLOW_CREATE_FOLDER = "exo:allowCreateFolder".intern() ;

  private RepositoryService repositoryService_;
  private NodeHierarchyCreator nodeHierarchyCreator_;
  private InitParams params_ ; 

  public ManageDrivePlugin(RepositoryService repositoryService, 
      InitParams params, NodeHierarchyCreator nodeHierarchyCreator) throws Exception {
    repositoryService_ = repositoryService;
    nodeHierarchyCreator_ = nodeHierarchyCreator ;
    params_ = params ;  
  }

  @SuppressWarnings("unchecked")
  public void init() throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    while(it.hasNext()){
      DriveData data = (DriveData)it.next().getObject() ;
      try{
        Session session  = getSession(data.getRepository());
        addDrive(data, session) ;
        session.logout();
      }catch(Exception e) {
        e.printStackTrace() ;
        System.out.println("[WARNING] ==> Can not init drive '"+ data.getName()
            +"' in repository '" + data.getRepository()+"'");
      }

    }
  }

  @SuppressWarnings("unchecked")
  public void init(String repository) throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    DriveData data = null ;
    Session session = null ;
    while(it.hasNext()){
      data = (DriveData)it.next().getObject() ;       
      try{
        if(data.getRepository().equals(repository)) { 
          session = getSession(repository) ;
          addDrive(data, session) ;
          session.logout();
        }
      }catch(Exception e) {        
      }      

    }     
  }

  private void addDrive(DriveData data, Session session) throws Exception {
    String drivesPath = nodeHierarchyCreator_.getJcrPath(BasePath.EXO_DRIVES_PATH);
    Node driveHome = (Node)session.getItem(drivesPath) ;
    Node driveNode = null ;
    if(!driveHome.hasNode(data.getName())){
      driveNode = driveHome.addNode(data.getName(), "exo:drive");
      driveNode.setProperty(WORKSPACE, data.getWorkspace()) ;
      driveNode.setProperty(PERMISSIONS, data.getPermissions()) ;
      driveNode.setProperty(PATH, data.getHomePath()) ;
      driveNode.setProperty(VIEWS, data.getViews()) ;
      driveNode.setProperty(ICON, data.getIcon()) ;
      driveNode.setProperty(VIEW_REFERENCES, Boolean.toString(data.getViewPreferences())) ;
      driveNode.setProperty(VIEW_NON_DOCUMENT, Boolean.toString(data.getViewNonDocument())) ;
      driveNode.setProperty(VIEW_SIDEBAR, Boolean.toString(data.getViewSideBar())) ;
      driveNode.setProperty(SHOW_HIDDEN_NODE, Boolean.toString(data.getShowHiddenNode())) ;
      driveNode.setProperty(ALLOW_CREATE_FOLDER, data.getAllowCreateFolder()) ;
      driveHome.save() ;
      session.save() ;
    }
  }

  private Session getSession(String repository)throws Exception {
    ManageableRepository manaRepository = repositoryService_.getRepository(repository) ;
    return manaRepository.getSystemSession(manaRepository.getConfiguration().getSystemWorkspaceName()) ;
  }
}
