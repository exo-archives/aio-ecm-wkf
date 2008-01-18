package org.exoplatform.services.cms.records.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.records.RecordsService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.audit.AuditService;
import org.exoplatform.services.log.ExoLogger;

public class RecordsServiceImpl implements RecordsService {

  final static public String ASCENDING = "ascending" ;
  final static public String DESCENDING = "dedcending" ;  

  final static public String BASE_STATEMENT = "/jcr:root$path//element(*,$recordType) order by @$orderProperty $orderType";
  final static public String CONSTRAINTS_STATEMENT = "/jcr:root$path//element(*,$recordType) $propertyConstraints order by @$orderProperty $orderType";

  private static Log log_ = ExoLogger.getLogger("services.cms.records");
  private ActionServiceContainer actionsService_;  

  private AuditService auditService_;

  public RecordsServiceImpl(ActionServiceContainer actionServiceContainer, AuditService auditService) {
    actionsService_ = actionServiceContainer;
    auditService_ = auditService;    
  }

  // TODO handle a lock
  public void addRecord(Node filePlan, Node record) throws RepositoryException {
    //TODO need filter nodetype whe register evenlistener for observation
    if(!record.isNodeType("nt:file")) return;
    long counter = filePlan.getProperty("rma:recordCounter").getLong() + 1;
    filePlan.setProperty("rma:recordCounter", counter);
    processDefaultRecordProperties(filePlan, record, counter);
    processVitalInformation(filePlan, record);
    processCutoffInformation(filePlan, record);

    //make the record auditable
    record.addMixin("exo:auditable");    
    record.save() ;
    filePlan.save();    
    filePlan.getSession().save() ;
  }

  public void bindFilePlanAction(Node filePlan, String repository) throws Exception {
    Map<String,JcrInputProperty> mappings = new HashMap<String,JcrInputProperty>();

    JcrInputProperty nodeTypeInputProperty = new JcrInputProperty();
    nodeTypeInputProperty.setJcrPath("/node");
    nodeTypeInputProperty.setValue("processRecords");
    mappings.put("/node", nodeTypeInputProperty);

    JcrInputProperty nameInputProperty = new JcrInputProperty();
    nameInputProperty.setJcrPath("/node/exo:name");
    nameInputProperty.setValue("processRecords");
    mappings.put("/node/exo:name", nameInputProperty);

    JcrInputProperty lifeCycleInputProperty = new JcrInputProperty();
    lifeCycleInputProperty.setJcrPath("/node/exo:lifecyclePhase");
    lifeCycleInputProperty.setValue("add");
    mappings.put("/node/exo:lifecyclePhase", lifeCycleInputProperty);

    JcrInputProperty descriptionInputProperty = new JcrInputProperty();
    descriptionInputProperty.setJcrPath("/node/exo:description");
    descriptionInputProperty
    .setValue("compute info such as holding dates on a new record added to that file plan");
    mappings.put("/node/exo:description", descriptionInputProperty);

    actionsService_.addAction(filePlan, repository, "exo:processRecordAction", mappings);
  }

  public void computeAccessions(Node filePlan) throws RepositoryException {
    log_.info("Compute records accession");     
    for(Node record:getAccessionableRecords(filePlan)){
      Calendar accessionDate = record.getProperty("rma:accessionableDate").getDate();
      Calendar currentDate = new GregorianCalendar();
      if (accessionDate.before(currentDate)) {
        Session session = record.getSession();
        String accessionLocation = filePlan
        .getProperty("rma:accessionLocation").getString();
        if (accessionLocation != null && !"".equals(accessionLocation)) {
          try {
            session.getWorkspace().copy(record.getPath(),
                accessionLocation + "/" + record.getName());
          } catch (ItemNotFoundException ex) {
            log_.warn(ex);
          }
        }
        record.setProperty("rma:accessionExecuted", true);
        record.save();
        filePlan.save() ;
      }
    }
    log_.info("Compute records accession over");
  }

  public void computeCutoffs(Node filePlan) throws RepositoryException {
    List<Node>toCutoffList = getCutoffRecords(filePlan) ; 
    for(Node record: toCutoffList){

      // check if it is obsolete
      if (cutoffObsolete(filePlan, record))
        return;

      // check if it is superseded
      if (cutoffSuperseded(filePlan, record))
        return;

      // check if it has expired
      if (cutoffHasExpired(filePlan, record))
        return;

      // check if the cutoff now flag is set
      if (cutoffNow(filePlan, record))
        return;

      // check if an event occured
      if (cutoffEvent(filePlan, record))
        return;

    }
  }

  public void computeDestructions(Node filePlan) throws RepositoryException { 
    List<Node> toDestroyList = getDestroyableRecords(filePlan) ;
    for(Node record:toDestroyList){
      Calendar destructionDate = record.getProperty("rma:destructionDate").getDate();
      Calendar currentDate = new GregorianCalendar();
      if (destructionDate.after(currentDate)) {
        record.remove();
        filePlan.save() ;
      }
    }
  }

  public void computeHolds(Node filePlan) throws RepositoryException {
    List<Node> toHoldList = getHolableRecords(filePlan) ; 
    for(Node record:toHoldList){
      // check if the record is frozen, which extends the hold period
      boolean isFrozenRecord = record.getProperty("rma:freeze").getBoolean();
      if (!isFrozenRecord) {
        if (record.hasProperty("rma:holdsDiscretionary")
            && record.getProperty("rma:holdsDiscretionary").getBoolean()) {
          // holds is discretionary
          String eventToWaitFor = record.getProperty("rma:holdUntilEvent").getString();
          // TODO allow to plug events handler here
          record.setProperty("rma:holdExecuted", true);
          record.save();
        } else {
          Calendar holdUntil = record.getProperty("rma:holdUntil").getDate();
          Calendar currentDate = new GregorianCalendar();
          if (holdUntil.before(currentDate)) {
            // need to move to the next phase, either transfer or destruction
            boolean processTransfer = filePlan.getProperty("rma:processTransfer").getBoolean();
            boolean processDestruction = filePlan.getProperty("rma:processDestruction").getBoolean();
            if (processTransfer) {
              setupTransfer(filePlan, record);
            } else if (processDestruction) {
              setupDestruction(filePlan, record);
            }
            record.setProperty("rma:holdExecuted", true);
            record.save();
            filePlan.save() ;
          } else
            log_.info("Record still in holding");
        }
      }
    }
    filePlan.save() ;
  }

  public void computeTransfers(Node filePlan) throws RepositoryException {
    log_.info("Compute records transfer");
    List<Node> toTransfer = getTransferableRecords(filePlan) ;
    for (Node record:toTransfer) {
      Calendar transferDate = record.getProperty("rma:transferDate").getDate();
      Calendar currentDate = new GregorianCalendar();
      if (transferDate.before(currentDate)) {
        Session session = record.getSession();
        String transferLocation = record.getProperty("rma:transferLocation").getString();
        log_.info("Transfer record to: " + transferLocation);
        if (transferLocation != null && !"".equals(transferLocation)) {
          try {
            session.getWorkspace().copy(record.getPath(),transferLocation + "/" + record.getName());
          } catch (ItemNotFoundException ex) {
            log_.error(ex.getMessage(), ex);
          }
        }
        record.setProperty("rma:transferExecuted", true);
        record.save() ;
        filePlan.save() ;
      }
    }
    log_.info("Transfer records over");
  }

  public List<Node> getAccessionableRecords(Node filePlan) throws RepositoryException {
    String statement = makeConstraintsStatement("[@rma:accessionExecuted= 'false']");
    return getRecordsByQuery(filePlan,statement,"rma:accessionable","rma:dateReceived",ASCENDING);        
  }

  public List<Node> getCutoffRecords(Node filePlan) throws RepositoryException {
    String statement = makeConstraintsStatement("[@rma:cutoffExecuted= 'false']");    
    return getRecordsByQuery(filePlan,statement,"rma:cutoffable","rma:dateReceived",ASCENDING);
  }

  public List<Node> getDestroyableRecords(Node filePlan) throws RepositoryException {
    return getRecordsByQuery(filePlan,BASE_STATEMENT,"rma:destroyable","rma:dateReceived",ASCENDING);
  }

  public List<Node> getHolableRecords(Node filePlan) throws RepositoryException {    
    String statement = makeConstraintsStatement("[@rma:holdExecuted= 'false']");
    return getRecordsByQuery(filePlan,statement,"rma:holdable","rma:dateReceived",ASCENDING);
  }

  public List<Node> getObsoleteRecords(Node filePlan) throws RepositoryException {        
    String statement = makeConstraintsStatement("[@rma:isObsolete= 'true']");
    return getRecordsByQuery(filePlan,statement,"rma:record","rma:dateReceived",ASCENDING);    
  }

  public List<Node> getRecords(Node filePlan) throws RepositoryException {
    return getRecordsByQuery(filePlan,BASE_STATEMENT, "rma:record","rma:dateReceived",ASCENDING);   
  }

  public List<Node> getSupersededRecords(Node filePlan) throws RepositoryException {
    String statement = makeConstraintsStatement("[@rma:superseded = 'true']") ;    
    return getRecordsByQuery(filePlan,statement,"rma:record","rma:dateReceived",ASCENDING);    
  }

  public List<Node> getTransferableRecords(Node filePlan) throws RepositoryException {    
    String statement = makeConstraintsStatement("[@rma:transferExecuted = 'false']") ;
    return getRecordsByQuery(filePlan,statement,"rma:transferable","rma:dateReceived",ASCENDING);       
  }

  public List<Node> getVitalRecords(Node filePlan) throws RepositoryException {    
    return getRecordsByQuery(filePlan,BASE_STATEMENT,"rma:vitalRecord","rma:nextReviewDate",DESCENDING);
  }

  // after holding a process can be moved to a national agency
  private void setupAccession(Node filePlan, Node record) {
    try {
      boolean processAccession = filePlan.getProperty("rma:processAccession")
      .getBoolean();
      if (processAccession) {
        record.addMixin("rma:accessionable");
        // By convention the current date is set as the transfer one
        Calendar currentDate = new GregorianCalendar();
        currentDate.add(Calendar.MINUTE, 5);
        record.setProperty("rma:accessionDate", currentDate);
        record.save();
        filePlan.save() ;
      }
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }

  // after cutoff or holding a record maybe detroyed
  private void setupDestruction(Node filePlan, Node record) {
    try {
      record.addMixin("rma:destroyable");
      // By convention the current date is set as the destruction one plus 5
      // minutes
      Calendar currentDate = new GregorianCalendar();
      currentDate.add(Calendar.MINUTE, 5);
      record.setProperty("rma:destructionDate", currentDate);
      record.save() ;
      filePlan.save() ;
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }

  // after cutoff or holding a process can be transfered
  private void setupTransfer(Node filePlan, Node record) {
    try {
      record.addMixin("rma:transferable");
      // fill the transfer location
      String location = filePlan.getProperty("rma:defaultTransferLocation")
      .getString();
      record.setProperty("rma:transferLocation", location);

      // By convention the current date is set as the transfer one plus 5
      // minutes
      Calendar currentDate = new GregorianCalendar();
      currentDate.add(Calendar.MINUTE, 5);
      record.setProperty("rma:transferDate", currentDate);
      record.save() ;
      filePlan.save() ;
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }

  private void calculateNextRevDate(Calendar currentDate, String period) {
    if ("one minute".equals(period)) {
      currentDate.add(Calendar.MINUTE, 1);
    } else if ("hourly".equals(period)) {
      currentDate.add(Calendar.HOUR, 1);
    } else if ("daily".equals(period)) {
      currentDate.add(Calendar.HOUR, 24);
    } else if ("monthly".equals(period)) {
      currentDate.add(Calendar.MONTH, 1);
    } else if ("quarterly".equals(period)) {
      currentDate.add(Calendar.MONTH, 4);
    } else if ("yearly".equals(period)) {
      currentDate.add(Calendar.YEAR, 1);
    } else if ("ten years".equals(period)) {
      currentDate.add(Calendar.YEAR, 10);
    }
  }

  // determine if the next phase is a hold, transfer or destruction
  private void computeNextRecordPhaseAfterCutoff(Node filePlan, Node record) throws RepositoryException {
    boolean processHold = filePlan.getProperty("rma:processHold").getBoolean();
    boolean processTransfer = filePlan.getProperty("rma:processTransfer").getBoolean();
    boolean processDestruction = filePlan.getProperty("rma:processDestruction").getBoolean();
    if (processHold) {
      record.addMixin("rma:holdable");
      // check if the hold is discretionary, aka if the hold period ends after a
      // dedicated event
      boolean discretionaryHold = filePlan.getProperty("rma:discretionaryHold").getBoolean();
      if (discretionaryHold) {
        record.setProperty("rma:holdsDiscretionary", true);
        record.setProperty("rma:holdUntilEvent", "EventToWaitFor");
      } else {
        // if not, check if the hold should expired after a dedicated date and
        // compute that date
        String holdPeriod = filePlan.getProperty("rma:holdPeriod").getString();
        if (holdPeriod != null) {
          Calendar currentDate = new GregorianCalendar();
          calculateNextRevDate(currentDate, holdPeriod);
          record.setProperty("rma:holdUntil", currentDate);
        }
      }
    } else if (processTransfer) {
      setupTransfer(filePlan, record);
    } else if (processDestruction) {
      setupDestruction(filePlan, record);
    }
    record.setProperty("rma:cutoffExecuted", true);
    record.save();
    filePlan.save() ;
  }

  private boolean cutoffEvent(Node filePlan, Node record)
  throws RepositoryException {
    //String cutoffEvent = record.getProperty("rma:cutoffEvent").getString();
    // TODO find a way to plug event handler
    return false;
  }

  private boolean cutoffHasExpired(Node filePlan, Node record)
  throws RepositoryException {
    Calendar cutoffDateTime = record.getProperty("rma:cutoffDateTime").getDate();
    Calendar currentDate = new GregorianCalendar();
    if (currentDate.after(cutoffDateTime)) {
      log_.info("Cutoff has expired");
      computeNextRecordPhaseAfterCutoff(filePlan, record);
      return true;
    }
    return false;
  }

  private boolean cutoffNow(Node filePlan, Node record)
  throws RepositoryException {
    boolean cutoffNow = record.getProperty("rma:cutoffNow").getBoolean();
    if (cutoffNow) {
      log_.info("Cutoff record now");
      computeNextRecordPhaseAfterCutoff(filePlan, record);
      return true;
    }
    return false;
  }

  private boolean cutoffObsolete(Node filePlan, Node record)
  throws RepositoryException {
    boolean cutoffIsObsolete = record.getProperty("rma:isObsolete").getBoolean();
    if (cutoffIsObsolete) {
      log_.info("Cutoff is obsolete");
      computeNextRecordPhaseAfterCutoff(filePlan, record);
      return true;
    }
    return false;
  }

  private boolean cutoffSuperseded(Node filePlan, Node record) throws RepositoryException {    
    boolean cutoffIsSuperseded = record.getProperty("rma:superseded").getBoolean();
    if(cutoffIsSuperseded) {
      log_.info("Cutoff is superseded");
      computeNextRecordPhaseAfterCutoff(filePlan, record);
      return true; 
    }          
    return false;
  }

  private List<Node> getRecordsByQuery(Node filePlan, String templateStatement, String recordType,String orderProperty,String orderType) throws RepositoryException{
    List<Node> list = new ArrayList<Node>();
    String statement = StringUtils.replace(templateStatement,"$path",filePlan.getPath());
    statement = StringUtils.replace(statement,"$recordType",recordType);
    statement = StringUtils.replace(statement,"$orderProperty",orderProperty);
    statement = StringUtils.replace(statement,"$orderType",orderType);    
    QueryManager queryManager = null;
    try {
      queryManager = filePlan.getSession().getWorkspace().getQueryManager(); 
    } catch (Exception e) {
      return list;
    }    
    Query query = queryManager.createQuery(statement,Query.XPATH);
    QueryResult queryResult = query.execute();    
    for(NodeIterator iterator = queryResult.getNodes();iterator.hasNext();) {
      Node node = iterator.nextNode();
      list.add(node);
    }
    return list;
  }

  private String makeConstraintsStatement(String constraints) {
    return StringUtils.replace(CONSTRAINTS_STATEMENT,"$propertyConstraints",constraints);
  }

  private void processCutoffInformation(Node filePlan, Node record) {
    boolean isCutoffable;
    try {
      isCutoffable = filePlan.getProperty("rma:processCutoff").getBoolean();
      if (isCutoffable) {
        record.addMixin("rma:cutoffable");

        // check if there is a cutoff period, and if so calculate the cutoff
        // date
        String cutoffPeriod = filePlan.getProperty("rma:cutoffPeriod")
        .getString();
        if (cutoffPeriod != null) {
          Calendar currentDate = new GregorianCalendar();
          calculateNextRevDate(currentDate, cutoffPeriod);
          record.setProperty("rma:cutoffDateTime", currentDate);
        }

        // check if the record can be cutoff on obsolescence.
        boolean cutoffObsolete = filePlan.getProperty("rma:cutoffOnObsolete")
        .getBoolean();
        if (cutoffObsolete) {
          record.setProperty("rma:cutoffObsolete", true);
        }

        // check if the record can be cutoff on superseded.
        boolean cutoffSuperseded = filePlan.getProperty(
        "rma:cutoffOnSuperseded").getBoolean();
        if (cutoffSuperseded) {
          record.setProperty("rma:cutoffSuperseded", true);
        }

        // check if some events can trigger the cutoff, then fill the record
        // with the event type
        try {
          String eventTrigger = filePlan.getProperty("rma:eventTrigger").getString();
          if (eventTrigger != null) {
            record.setProperty("rma:cutoffEvent", eventTrigger);
          } 
        } catch (Exception e) { }        
      }
      record.save() ;
      filePlan.save() ;        
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }

  private void processDefaultRecordProperties(Node filePlan, Node record,
      long counter) throws RepositoryException {
    record.addMixin("rma:record");

    record.setProperty("rma:dateReceived", new GregorianCalendar());
    record.setProperty("rma:originator", ((ExtendedNode) record).getACL()
        .getOwner());

    String recordCategoryIdentifier = filePlan.getProperty(
    "rma:recordCategoryIdentifier").getString();
    String recordIdentifier = recordCategoryIdentifier + "-" + counter + " "
    + record.getName();
    record.setProperty("rma:recordIdentifier", recordIdentifier);

    String defaultOriginatingOrganization = filePlan.getProperty(
    "rma:defaultOriginatingOrganization").getString();
    record.setProperty("rma:originatingOrganization",
        defaultOriginatingOrganization);

    Node dcNode = null;
    Item primaryItem = null;
    try {
      primaryItem = record.getPrimaryItem();
      if (primaryItem.isNode())
        dcNode = (Node) primaryItem;
      dcNode = record; 
    } catch (ItemNotFoundException e) {
      dcNode = record;
    }

    if (dcNode.isNodeType("dc:elementSet")) {
      if (dcNode.hasProperty("dc:subject")) {
        Value[] subjects = dcNode.getProperty("dc:subject").getValues();
        if (subjects != null && subjects.length > 0) {
          record.setProperty("rma:subject", subjects[0].getString());
        }
      }
      if (dcNode.hasProperty("dc:date")) {
        Value[] dates = dcNode.getProperty("dc:date").getValues();
        if (dates != null && dates.length > 0) {
          record.setProperty("rma:dateFiled", dates[0].getDate());
        }
      }
      if (dcNode.hasProperty("dc:format")) {
        Value[] formats = dcNode.getProperty("dc:format").getValues();
        if (formats != null && formats.length > 0) {
          record.setProperty("rma:format", formats[0].getString());
        }
      }
    }   
    record.save() ;
    filePlan.save() ;       
  }

  private void processVitalInformation(Node filePlan, Node record) {
    try {
      boolean isVital = filePlan.getProperty("rma:vitalRecordIndicator").getBoolean();
      if (isVital) {
        record.addMixin("rma:vitalRecord");
        String vitalReviewPeriod = filePlan.getProperty("rma:vitalRecordReviewPeriod").getString();    
        Calendar previousReviewDate = null ;
        Calendar currentDate = new GregorianCalendar();
        if(record.hasProperty("rma:nextReviewDate")) {
          previousReviewDate = record.getProperty("rma:nextReviewDate").getDate() ;           
        }else {
          previousReviewDate = currentDate ;
        }                      
        record.setProperty("rma:prevReviewDate",previousReviewDate) ;
        calculateNextRevDate(currentDate, vitalReviewPeriod);
        record.setProperty("rma:nextReviewDate", currentDate);
        record.save() ;
        filePlan.save() ;         
      }
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }

}
