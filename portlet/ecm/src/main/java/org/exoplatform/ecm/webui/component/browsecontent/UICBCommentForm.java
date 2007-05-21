/***************************************************************************
 * Copyright 2001-2007 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.browsecontent;
import javax.jcr.Node;

import org.exoplatform.ecm.jcr.UIPopupComponent;
import org.exoplatform.ecm.utils.Utils;
import org.exoplatform.ecm.webui.component.UIPopupAction;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.webui.component.UIForm;
import org.exoplatform.webui.component.UIFormStringInput;
import org.exoplatform.webui.component.UIFormTextAreaInput;
import org.exoplatform.webui.component.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.component.validator.EmailAddressValidator;
import org.exoplatform.webui.component.validator.EmptyFieldValidator;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@gmail.com
 * Jan 30, 2007  
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/UIFormWithOutTitle.gtmpl",
    events = {
      @EventConfig(listeners = UICBCommentForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UICBCommentForm.CancelActionListener.class)
    }
) 

public class UICBCommentForm extends UIForm implements UIPopupComponent {
  final public static String DEFAULT_LANGUAGE = "default".intern() ;
  final private static String FIELD_NAME = "name" ;
  final private static String FIELD_EMAIL = "email" ;
  final private static String FIELD_WEBSITE = "website" ;
  final private static String FIELD_COMMENT = "comment" ;
  private Node docNode_ ;


  public UICBCommentForm() throws Exception {
    addChild(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null)) ;
    addChild(new UIFormStringInput(FIELD_EMAIL, FIELD_EMAIL, null).addValidator(EmailAddressValidator.class)) ;
    addChild(new UIFormStringInput(FIELD_WEBSITE, FIELD_WEBSITE, null)) ;
    addChild(new UIFormTextAreaInput(FIELD_COMMENT, FIELD_COMMENT, null).addValidator(EmptyFieldValidator.class)) ;
    setActions(new String[] {"Save", "Cancel"}) ;
  }

  public Node getDocument() { return docNode_ ;}
  public void setDocument(Node node) { docNode_ = node ;}

  public static class CancelActionListener extends EventListener<UICBCommentForm>{
    public void execute(Event<UICBCommentForm> event) throws Exception {
      UICBCommentForm uiForm = event.getSource() ;
      uiForm.reset() ;
      UIPopupAction uiPopupAction = uiForm.getAncestorOfType(UIPopupAction.class) ;
      uiPopupAction.deActivate() ;
    }
  }  

  public static class SaveActionListener extends EventListener<UICBCommentForm>{
    public void execute(Event<UICBCommentForm> event) throws Exception {
      UICBCommentForm uiForm = event.getSource() ;
      String name = uiForm.getUIStringInput(FIELD_NAME).getValue() ;
      String email = uiForm.getUIStringInput(FIELD_EMAIL).getValue() ;
      String website = uiForm.getUIStringInput(FIELD_WEBSITE).getValue() ;
      String comment = uiForm.getUIFormTextAreaInput(FIELD_COMMENT).getValue() ;
      try {
        String language = uiForm.getAncestorOfType(UIBrowseContentPortlet.class).
                                 findFirstComponentOfType(UIDocumentDetail.class).getLanguage() ;
        if(DEFAULT_LANGUAGE.equals(language)) { 
          if(!uiForm.getDocument().hasProperty(Utils.EXO_LANGUAGE)){
            uiForm.getDocument().addMixin("mix:i18n") ;
            uiForm.getDocument().save() ;
            language = DEFAULT_LANGUAGE ;
          } else {
            language = uiForm.getDocument().getProperty(Utils.EXO_LANGUAGE).getString() ;
          }
        }
        CommentsService commentsService = uiForm.getApplicationComponent(CommentsService.class) ; 
        commentsService.addComment(uiForm.getDocument(), name, email, website, comment, language) ;
      } catch (Exception e) {        
        e.printStackTrace() ;
      }
      UIPopupAction uiPopupAction = uiForm.getAncestorOfType(UIPopupAction.class) ;
      uiPopupAction.deActivate() ;
    }
  }

  public void activate() throws Exception {
    // TODO Auto-generated method stub

  }

  public void deActivate() throws Exception {
    // TODO Auto-generated method stub

  }

}