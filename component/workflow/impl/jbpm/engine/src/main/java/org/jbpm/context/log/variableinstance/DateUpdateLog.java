/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.jbpm.context.log.variableinstance;

import java.util.*;
import org.jbpm.context.exe.*;
import org.jbpm.context.log.*;

public class DateUpdateLog extends VariableUpdateLog {

  private static final long serialVersionUID = 1L;

  Date oldValue = null;
  Date newValue = null;

  public DateUpdateLog() {
  }

  public DateUpdateLog(VariableInstance variableInstance, Date oldValue, Date newValue) {
    super(variableInstance);
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public Object getOldValue() {
    return oldValue;
  }

  public Object getNewValue() {
    return newValue;
  }
}
