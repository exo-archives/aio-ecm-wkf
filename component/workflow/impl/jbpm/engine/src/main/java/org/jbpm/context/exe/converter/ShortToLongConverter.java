package org.jbpm.context.exe.converter;

import org.jbpm.context.exe.*;

public class ShortToLongConverter implements Converter {
  
  private static final long serialVersionUID = 1L;

  public boolean supports(Class clazz) {
    return (clazz==Short.class);
  }

  public Object convert(Object o) {
    return new Long( ((Number)o).longValue() );
  }
  
  public Object revert(Object o) {
    return new Short(((Long)o).shortValue());
  }
}
