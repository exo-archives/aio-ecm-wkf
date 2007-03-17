package org.jbpm.taskmgmt.def;

import java.io.Serializable;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.exe.Assignable;

/**
 * assigns {@link org.jbpm.taskmgmt.exe.TaskInstance}s or 
 * {@link org.jbpm.taskmgmt.exe.SwimlaneInstance}s to 
 * actors.
 */
public interface AssignmentHandler extends Serializable {

  /**
   * assigns the assignable (={@link org.jbpm.taskmgmt.exe.TaskInstance} or 
   * a {@link org.jbpm.taskmgmt.exe.SwimlaneInstance} to an swimlaneActorId or 
   * a set of {@link org.jbpm.taskmgmt.exe.PooledActor}s.
   * 
   * <p>The swimlaneActorId is the user that is responsible for the given task or swimlane.
   * The pooledActors represents a pool of actors to which the task or swimlane is 
   * offered.  Any actors from the pool can then take a TaskInstance by calling
   * {@link org.jbpm.taskmgmt.exe.TaskInstance#setActorId(String)}.
   * </p>
   */
  void assign( Assignable assignable, ExecutionContext executionContext ) throws Exception;
}
