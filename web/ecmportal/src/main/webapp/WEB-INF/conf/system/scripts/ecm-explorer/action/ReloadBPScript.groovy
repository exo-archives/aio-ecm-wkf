import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.workflow.WorkflowFormsService;
import org.exoplatform.services.workflow.WorkflowFileDefinitionService;

/**
 * Created by Bull R&D
 * @author Silani Patrick
 * E-mail: patrick.silani@gmail.com
 * July 18, 2006
 */
public class ReloadBPScript implements CmsScript {

  /**
   * Reference to the Workflow Forms Service.
   * This service manages the definition of Forms displayed in Portlets.
   */
  private WorkflowFormsService formsService = null;
  
  /**
   * Reference to the Workflow File Definition Service.
   * This service abstracts the storage of the Workflow definitions.
   */
  private WorkflowFileDefinitionService fileDefinitionService = null;

  /**
   * Constructor.
   * Caches references to the required services obtained from the eXo container.
   *
   * @param formsService reference to the Workflow Forms Service
   * @param fileDefinitionService reference to the Workflow File Definition
   *        Service
   */
  public ReloadBPScript(WorkflowFormsService formsService,
                        WorkflowFileDefinitionService fileDefinitionService) {
                        
    this.formsService          = formsService;
    this.fileDefinitionService = fileDefinitionService;
  }

  /**
   * Implementation of the script logic.
   * The Forms and File Definition of the reloaded Workflow are invalidated.
   *
   * @param context Context of execution
   */
  public void execute(Object context) {

    try {
      // Retrieve Business Process Model Node
      Node actionNode = (Node) ((Map) context).get("actionNode");
      Node folderNode = actionNode.getParent();
     
      if (folderNode.hasProperty("exo:businessProcessId")) {
        String id = folderNode.getProperty("exo:businessProcessId").getString();
        fileDefinitionService.removeFromCache(id);
        formsService.removeForms(id);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setParams(String[] params) {}
}
