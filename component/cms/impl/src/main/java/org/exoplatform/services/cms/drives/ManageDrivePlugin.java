package org.exoplatform.services.cms.drives;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.CmsConfigurationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;

public class ManageDrivePlugin extends BaseComponentPlugin {

  private static String WORKSPACE = "exo:workspace".intern() ;
  private static String PERMISSIONS = "exo:permissions".intern() ;
  private static String VIEWS = "exo:views".intern() ;
  private static String ICON = "exo:icon".intern() ;
  private static String PATH = "exo:path".intern() ;
  private static String VIEW_REFERENCES = "exo:viewPreferences".intern() ;
  private static String VIEW_NON_DOCUMENT = "exo:viewNonDocument".intern() ;
  private static String VIEW_SIDEBAR = "exo:viewSideBar".intern() ;
  private static String ALLOW_CREATE_FOLDER = "exo:allowCreateFolder".intern() ;

  private RepositoryService repositoryService_;
  private CmsConfigurationService cmsConfigService_;
  private InitParams params_ ; 

  //private Session session_ ;  
  public ManageDrivePlugin(RepositoryService repositoryService, 
      InitParams params, CmsConfigurationService cmsConfigService) throws Exception {
    repositoryService_ = repositoryService;
    cmsConfigService_ = cmsConfigService ;
    params_ = params ;
    //init() ;
  }

  public void init() throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    while(it.hasNext()){
      DriveData data = (DriveData)it.next().getObject() ;
      if(data.getAutoCreatedInNewRepository()) {
        List<RepositoryEntry> repositories = repositoryService_.getConfig().getRepositoryConfigurations() ;
        for(RepositoryEntry repo : repositories) {
          try{
            addDrive(data, getSession(repo.getName())) ;
          }catch(Exception e) {
            System.out.println("[WARNING] ==> Can not init drive '"+ data.getName()
                +"' in repository '" + repo.getName()+"'");
          }          
        }        
      }else {
        try{
          addDrive(data, getSession(data.getRepository())) ;
        }catch(Exception e) {
          System.out.println("[WARNING] ==> Can not init drive '"+ data.getName()
              +"' in repository '" + data.getRepository()+"'");
        }
        
      }
    }
  }
  
  public void init(String repository) throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    while(it.hasNext()){
      DriveData data = (DriveData)it.next().getObject() ;       
      if(data.getAutoCreatedInNewRepository() || repository.equals(data.getRepository())) {
        addDrive(data, getSession(repository)) ;
      }       
    }
     
  }
  
  private void addDrive(DriveData data, Session session) throws Exception {
    String drivesPath = cmsConfigService_.getJcrPath(BasePath.EXO_DRIVES_PATH);
    Node driveHome = (Node)session.getItem(drivesPath) ;
    if(!driveHome.hasNode(data.getName())){
      Node driveNode = driveHome.addNode(data.getName(), "exo:drive");
      driveNode.setProperty(WORKSPACE, data.getWorkspace()) ;
      driveNode.setProperty(PERMISSIONS, data.getPermissions()) ;
      driveNode.setProperty(PATH, data.getHomePath()) ;
      driveNode.setProperty(VIEWS, data.getViews()) ;
      driveNode.setProperty(ICON, data.getIcon()) ;
      driveNode.setProperty(VIEW_REFERENCES, Boolean.toString(data.getViewPreferences())) ;
      driveNode.setProperty(VIEW_NON_DOCUMENT, Boolean.toString(data.getViewNonDocument())) ;
      driveNode.setProperty(VIEW_SIDEBAR, Boolean.toString(data.getViewSideBar())) ;
      driveNode.setProperty(ALLOW_CREATE_FOLDER, data.getAllowCreateFolder()) ;
      driveHome.save() ;
    }
    session.save() ;
  }
  
  private Session getSession(String repository)throws Exception {
    return repositoryService_.getRepository(repository)
    .getSystemSession(cmsConfigService_.getWorkspace(repository)) ;
  }
}
