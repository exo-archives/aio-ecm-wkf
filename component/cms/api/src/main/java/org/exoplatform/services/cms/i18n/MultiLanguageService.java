package org.exoplatform.services.cms.i18n;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Value;


/**
 * Author : Hung Nguyen Quang
 *          nguyenkequanghung@yahoo.com
 */

public interface MultiLanguageService {
  
  public static final String LANGUAGES = "languages" ;
  public static final String EXO_LANGUAGE = "exo:language" ;
  
  
  public List<String> getSupportedLanguages(Node node) throws Exception ;
  public void setDefault(Node node, String language) throws Exception ;
  public void addLanguage(Node node, Map inputs, String language, boolean isDefault) throws Exception ;
  public void addFileLanguage(Node node, Value value, String language, boolean isDefault) throws Exception ;
  public String getDefault(Node node) throws Exception ;
  public Node getLanguage(Node node, String language) throws Exception ;
  
}
