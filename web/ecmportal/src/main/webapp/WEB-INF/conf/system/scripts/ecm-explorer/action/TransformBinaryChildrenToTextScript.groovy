/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/

import java.util.Map;

import javax.jcr.Property;
import javax.jcr.Node ;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.document.DocumentReaderService;

/*
* Will need to get The MailService when it has been moved to exo-platform
*/
public class TransformBinaryChildrenToTextScript implements CmsScript {
  
  private DocumentReaderService readerService_;
  
  public TransformBinaryChildrenToTextScript(DocumentReaderService readerService) {  
    readerService_ = readerService;
  }
  
  public void execute(Object context) {
    Map variables = (Map) context;       

    Node actionNode = (Node) variables.get("actionNode");
    Node folderNode = actionNode.getParent();    
    
    try {
      NodeIterator iter = folderNode.getNodes();
      while(iter.hasNext()) {
        Node childNode = iter.nextNode();
        if("nt:file".equals(childNode.getPrimaryNodeType().getName())) {
          Node content = childNode.getNode("jcr:content");
          Property mime = content.getProperty("jcr:mimeType");
          if (!mime.getString().startsWith("text")) {          
            String text = readerService_.getContentAsText(mime.getString(), content
              .getProperty("jcr:data").getStream());
            Node file = null;           
            try {
              file = folderNode.getNode(childNode.getName() + ".txt");
            } catch (PathNotFoundException e) {
              file = folderNode.addNode(childNode.getName() + ".txt", "nt:file");
            }
            Node contentNode = file.addNode("jcr:content", "nt:resource");
            contentNode.setProperty("jcr:encoding", "UTF-8");
            contentNode.setProperty("jcr:mimeType", "text/html");    
            contentNode.setProperty("jcr:data", text);
            contentNode.setProperty("jcr:lastModified", new GregorianCalendar());                              
          }
        }        
      }
      folderNode.save();
    } catch (Exception e) {      
      e.printStackTrace();
    }  
  }

  public void setParams(String[] params) {}

}