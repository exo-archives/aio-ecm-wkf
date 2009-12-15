eXo.require("eXo.projects.Module");
eXo.require("eXo.projects.Product");
eXo.require("eXo.projects.Workflow");

function getModule(params) {

  var kernel = params.kernel;
  var core = params.core;
  var eXoPortletContainer = params.eXoPortletContainer;
  var ws = params.ws;
  var jcr = params.eXoJcr;
  var portal = params.portal;  
  var module = new Module();
  
  module.version = "${project.version}";
  module.relativeMavenRepo =  "org/exoplatform/ecm/workflow";
  module.relativeSRCRepo =  "ecm/workflow";
  module.name =  "eXoWorkflow";
  module.portlet = {}
  module.portlet.workflow = new Workflow("",module.version).getPortlet();
  module.web = {}
  module.web.eXoWorkflowResources = 
    new Project("org.exoplatform.ecm.workflow", "exo.ecm.workflow.web.eXoWorkflowResources", "war", module.version) ;  
  module.web.eXoWorkflowResources.deployName = "eXoWorkflowResources" ;
  
  module.web.portal = 
    new Project("org.exoplatform.ecm.workflow", "exo.ecm.workflow.web.portal", "exo-portal", module.version).
    addDependency(portal.web.eXoResources).
    addDependency(portal.web.eXoMacSkin).
    addDependency(portal.web.eXoVistaSkin).
    addDependency(portal.webui.portal).
    addDependency(jcr.frameworks.command).
    addDependency(jcr.frameworks.web);  
    
  return module;
}
