package org.exoplatform.services.cms;


public class JcrInputProperty {
  
  public static final int PROPERTY = 0;
  public static final int NODE = 1;
  public static final int SINGLE_VALUE = 0;
  public static final int MULTI_VALUE = 1;
  public static final int BYTE_VALUE = 2;
  
  private String jcrPath;
  private int type = PROPERTY;
  private String nodetype;
  private String mixintype;
  private Object value;
  private int valueType = 0;
  
  public String getJcrPath() {
    return jcrPath;
  }
  public void setJcrPath(String jcrPath) {
    this.jcrPath = jcrPath;
  }
  public String getNodetype() {
    return nodetype;
  }
  public void setNodetype(String nodetype) {
    this.nodetype = nodetype;
  }
  public String getMixintype() {
    return mixintype;
  }
  public void setMixintype(String mixintype) {
    this.mixintype = mixintype;
  }
  public int getType() {
    return type;
  }
  public void setType(int type) {
    this.type = type;
  }
    
  public void setValue(Object value) {
    this.value = value;    
  }
  public Object getValue() {
    return value;
  }
  
  public void setValueType(int type){ valueType = type ; }
  public int  getValueType(){ return valueType ; }
  
}
