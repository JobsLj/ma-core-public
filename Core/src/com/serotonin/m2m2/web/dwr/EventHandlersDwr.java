/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.WebContextFactory;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.SchemaDefinition;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.ProcessEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.SetPointEventHandlerDefinition;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.handlers.EmailHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.SetPointHandlerRT;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.ProcessWorkItem;
import com.serotonin.m2m2.rt.script.CompiledScriptExecutor;
import com.serotonin.m2m2.rt.script.EventInstanceWrapper;
import com.serotonin.m2m2.rt.script.ResultTypeException;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptLog.LogLevel;
import com.serotonin.m2m2.rt.script.ScriptPermissions;
import com.serotonin.m2m2.rt.script.ScriptPermissionsException;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointExtendedNameComparator;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.dwr.beans.DataPointBean;
import com.serotonin.m2m2.web.dwr.beans.EventSourceBean;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;

public class EventHandlersDwr extends BaseDwr {
    private static final Log LOG = LogFactory.getLog(EventHandlersDwr.class);

    @DwrPermission(user = true)
    public Map<String, Object> getInitData() {
        User user = Common.getHttpUser();
        Permissions.ensureDataSourcePermission(user);

        Map<String, Object> model = new HashMap<>();

        // Get the data sources.
        List<DataSourceVO<?>> dss = DataSourceDao.instance.getDataSources();

        // Create a lookup of data sources to quickly determine data point permissions.
        Map<Integer, DataSourceVO<?>> dslu = new HashMap<>();
        for (DataSourceVO<?> ds : dss)
            dslu.put(ds.getId(), ds);

        // Get the data points
        List<DataPointBean> allPoints = new ArrayList<>();
        List<EventSourceBean> dataPoints = new ArrayList<>();
        List<DataPointVO> dps = DataPointDao.instance.getDataPoints(DataPointExtendedNameComparator.instance, true);
        final boolean admin = Permissions.hasAdmin(user);
        
        for (DataPointVO dp : dps) {
            if (!admin && !Permissions.hasDataSourcePermission(user, dslu.get(dp.getDataSourceId())))
                continue;

            allPoints.add(new DataPointBean(dp));

            if (dp.getEventDetectors().size() > 0) {
                EventSourceBean source = new EventSourceBean();
                source.setId(dp.getId());
                source.setName(dp.getExtendedName());

                for (AbstractPointEventDetectorVO<?> ped : dp.getEventDetectors()) {
                    EventTypeVO dpet = ped.getEventType();
                    dpet.setHandlers(EventHandlerDao.instance.getEventHandlers(dpet));
                    source.getEventTypes().add(dpet);
                }

                dataPoints.add(source);
            }
        }

        // Get the data sources
        List<EventSourceBean> dataSources = new ArrayList<>();
        for (DataSourceVO<?> ds : dss) {
            if (!admin && !Permissions.hasDataSourcePermission(user, ds))
                continue;

            if (ds.getEventTypes().size() > 0) {
                EventSourceBean source = new EventSourceBean();
                source.setId(ds.getId());
                source.setName(ds.getName());

                for (EventTypeVO dset : ds.getEventTypes()) {
                    dset.setHandlers(EventHandlerDao.instance.getEventHandlers(dset));
                    source.getEventTypes().add(dset);
                }

                dataSources.add(source);
            }
        }

        Map<String, Map<String, Object>> userEventTypes = new LinkedHashMap<>();
        model.put("userEventTypes", userEventTypes);
        for (EventTypeDefinition def : ModuleRegistry.getDefinitions(EventTypeDefinition.class)) {
            if (!def.getHandlersRequireAdmin()) {
                List<EventTypeVO> vos = def.getEventTypeVOs();

                for (EventTypeVO vo : vos)
                    vo.setHandlers(EventHandlerDao.instance.getEventHandlers(vo));

                Map<String, Object> info = new HashMap<>();
                info.put("vos", vos);
                info.put("iconPath", def.getIconPath());
                info.put("description", translate(def.getDescriptionKey()));

                userEventTypes.put(def.getTypeName(), info);
            }
        }

        if (admin) {
            // Get the publishers
            List<EventSourceBean> publishers = new ArrayList<>();
            for (PublisherVO<? extends PublishedPointVO> p : PublisherDao.instance
                    .getPublishers(new PublisherDao.PublisherNameComparator())) {
                if (p.getEventTypes().size() > 0) {
                    EventSourceBean source = new EventSourceBean();
                    source.setId(p.getId());
                    source.setName(p.getName());

                    for (EventTypeVO pet : p.getEventTypes()) {
                        pet.setHandlers(EventHandlerDao.instance.getEventHandlers(pet));
                        source.getEventTypes().add(pet);
                    }

                    publishers.add(source);
                }
            }
            model.put(SchemaDefinition.PUBLISHERS_TABLE, publishers);

            // Get the system events
            List<EventTypeVO> systemEvents = new ArrayList<>();
            for (EventTypeVO sets : SystemEventType.EVENT_TYPES) {
                sets.setHandlers(EventHandlerDao.instance.getEventHandlers(sets));
                systemEvents.add(sets);
            }
            model.put("systemEvents", systemEvents);

            // Get the audit events
            List<EventTypeVO> auditEvents = new ArrayList<>();
            for (EventTypeVO aets : AuditEventType.EVENT_TYPES) {
                aets.setHandlers(EventHandlerDao.instance.getEventHandlers(aets));
                auditEvents.add(aets);
            }
            model.put("auditEvents", auditEvents);

            Map<String, Map<String, Object>> adminEventTypes = new LinkedHashMap<>();
            model.put("adminEventTypes", adminEventTypes);
            for (EventTypeDefinition def : ModuleRegistry.getDefinitions(EventTypeDefinition.class)) {
                if (def.getHandlersRequireAdmin()) {
                    List<EventTypeVO> vos = def.getEventTypeVOs();

                    for (EventTypeVO vo : vos)
                        vo.setHandlers(EventHandlerDao.instance.getEventHandlers(vo));

                    Map<String, Object> info = new HashMap<>();
                    info.put("vos", vos);
                    info.put("iconPath", def.getIconPath());
                    info.put("description", translate(def.getDescriptionKey()));

                    adminEventTypes.put(def.getTypeName(), info);
                }
            }
            
        }

        model.put("userNewScriptPermissions", new ScriptPermissions(user));
        
        // Get the mailing lists.
        model.put(SchemaDefinition.MAILING_LISTS_TABLE, MailingListDao.instance.getMailingLists());

        // Get the users.
        model.put(SchemaDefinition.USERS_TABLE, UserDao.instance.getUsers());

        model.put("allPoints", allPoints);
        model.put(SchemaDefinition.DATAPOINTS_TABLE, dataPoints);
        model.put(SchemaDefinition.DATASOURCES_TABLE, dataSources);

        return model;
    }

    @DwrPermission(user = true)
    public String createSetValueContent(int pointId, String valueStr, String idSuffix) {
        DataPointVO pointVO = DataPointDao.instance.getDataPoint(pointId);
        Permissions.ensureDataSourcePermission(Common.getHttpUser(), pointVO.getDataSourceId());

        DataValue value = DataValue.stringToValue(valueStr, pointVO.getPointLocator().getDataTypeId());

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("point", pointVO);
        model.put("idSuffix", idSuffix);
        model.put("text", pointVO.getTextRenderer().getText(value, TextRenderer.HINT_FULL));
        model.put("rawText", pointVO.getTextRenderer().getText(value, TextRenderer.HINT_RAW));
        model.put("valueStr", valueStr);
        String snippet = pointVO.getTextRenderer().getSetPointSnippetFilename();
        return generateContent(WebContextFactory.get().getHttpServletRequest(), snippet, model);
    }

    @DwrPermission(user = true)
    public ProcessResult saveSetPointEventHandler(String eventType, String eventSubtype, int eventTypeRef1,
            int eventTypeRef2, int handlerId, String xid, String alias, boolean disabled, int targetPointId,
            int activeAction, String activeValueToSet, int activePointId, String activeScript, int inactiveAction,
            String inactiveValueToSet, int inactivePointId, String inactiveScript, int acknowledgeAction, boolean acknowledgeActionEvenIfInactive,
            String acknowledgeValueToSet, int acknowledgePointId, String acknowledgeScript, List<IntStringPair> additionalContext,
            ScriptPermissions scriptPermissions) {
        SetPointEventHandlerVO handler = new SetPointEventHandlerVO();
        handler.setDefinition(ModuleRegistry.getEventHandlerDefinition(SetPointEventHandlerDefinition.TYPE_NAME));
        handler.setTargetPointId(targetPointId);
        handler.setActiveAction(activeAction);
        handler.setActiveValueToSet(activeValueToSet);
        handler.setActivePointId(activePointId);
        handler.setActiveScript(activeScript);
        handler.setInactiveAction(inactiveAction);
        handler.setInactiveValueToSet(inactiveValueToSet);
        handler.setInactivePointId(inactivePointId);
        handler.setInactiveScript(inactiveScript);
        handler.setAcknowledgeAction(acknowledgeAction);
        handler.setAcknowledgeActionEvenIfInactive(acknowledgeActionEvenIfInactive);
        handler.setAcknowledgeValueToSet(acknowledgeValueToSet);
        handler.setAcknowledgePointId(acknowledgePointId);
        handler.setAcknowledgeScript(acknowledgeScript);
        handler.setAdditionalContext(additionalContext);
        handler.setScriptPermissions(scriptPermissions);
        return save(eventType, eventSubtype, eventTypeRef1, eventTypeRef2, handler, handlerId, xid, alias, disabled);
    }

    @DwrPermission(user = true)
    public ProcessResult saveEmailEventHandler(String eventType, String eventSubtype, int eventTypeRef1,
            int eventTypeRef2, int handlerId, String xid, String alias, boolean disabled,
            List<RecipientListEntryBean> activeRecipients, String customTemplate, boolean sendEscalation, boolean repeatEscalations,
            int escalationDelayType, int escalationDelay, List<RecipientListEntryBean> escalationRecipients, boolean sendInactive,
            boolean inactiveOverride, List<RecipientListEntryBean> inactiveRecipients, boolean sendAcknowledge, boolean sendAcknowledgeEvenIfInactive, 
            boolean acknowledgeOverride, List<RecipientListEntryBean> acknowledgeRecipients, boolean includeSystemInfo, 
            int includePointValueCount, boolean includeLogfile, List<IntStringPair> additionalContext, 
            ScriptPermissions permissions, String script) {
        EmailEventHandlerVO handler = new EmailEventHandlerVO();
        handler.setDefinition(ModuleRegistry.getEventHandlerDefinition(EmailEventHandlerDefinition.TYPE_NAME));
        handler.setActiveRecipients(activeRecipients);
        handler.setCustomTemplate(customTemplate);
        handler.setSendEscalation(sendEscalation);
        handler.setRepeatEscalations(repeatEscalations);
        handler.setEscalationDelayType(escalationDelayType);
        handler.setEscalationDelay(escalationDelay);
        handler.setEscalationRecipients(escalationRecipients);
        handler.setSendInactive(sendInactive);
        handler.setInactiveOverride(inactiveOverride);
        handler.setInactiveRecipients(inactiveRecipients);
        handler.setSendAcknowledge(sendAcknowledge);
        handler.setSendAcknowledgeEvenIfInactive(sendAcknowledgeEvenIfInactive);
        handler.setAcknowledgeOverride(acknowledgeOverride);
        handler.setAcknowledgeRecipients(acknowledgeRecipients);
        handler.setIncludeSystemInfo(includeSystemInfo);
        handler.setIncludePointValueCount(includePointValueCount);
        handler.setIncludeLogfile(includeLogfile);
        handler.setAdditionalContext(additionalContext);
        handler.setScriptPermissions(permissions);
        handler.setScript(script);
        return save(eventType, eventSubtype, eventTypeRef1, eventTypeRef2, handler, handlerId, xid, alias, disabled);
    }

    @DwrPermission(user = true)
    public ProcessResult saveProcessEventHandler(String eventType, String eventSubtype, int eventTypeRef1,
            int eventTypeRef2, int handlerId, String xid, String alias, boolean disabled, String activeProcessCommand,
            int activeProcessTimeout, String inactiveProcessCommand, int inactiveProcessTimeout, String acknowledgeProcessCommand,
            int acknowledgeProcessTimeout, boolean acknowledgeProcessEvenIfInactive) {
        ProcessEventHandlerVO handler = new ProcessEventHandlerVO();
        handler.setDefinition(ModuleRegistry.getEventHandlerDefinition(ProcessEventHandlerDefinition.TYPE_NAME));
        handler.setActiveProcessCommand(activeProcessCommand);
        handler.setActiveProcessTimeout(activeProcessTimeout);
        handler.setInactiveProcessCommand(inactiveProcessCommand);
        handler.setInactiveProcessTimeout(inactiveProcessTimeout);
        handler.setAcknowledgeProcessCommand(acknowledgeProcessCommand);
        handler.setAcknowledgeProcessTimeout(acknowledgeProcessTimeout);
        handler.setAcknowledgeProcessEvenIfInactive(acknowledgeProcessEvenIfInactive);
        return save(eventType, eventSubtype, eventTypeRef1, eventTypeRef2, handler, handlerId, xid, alias, disabled);
    }

    private ProcessResult save(String eventType, String eventSubtype, int eventTypeRef1, int eventTypeRef2,
            AbstractEventHandlerVO<?> vo, int handlerId, String xid, String alias, boolean disabled) {
        EventTypeVO type = new EventTypeVO(eventType, eventSubtype, eventTypeRef1, eventTypeRef2);
        Permissions.ensureEventTypePermission(Common.getHttpUser(), type);

        vo.setId(handlerId);
        vo.setXid(StringUtils.isBlank(xid) ? EventHandlerDao.instance.generateUniqueXid() : xid);
        vo.setAlias(alias);
        vo.setDisabled(disabled);
        
        vo.addAddedEventType(type.createEventType());
        
        ProcessResult response = new ProcessResult();
        vo.validate(response);

        if (!response.getHasMessages()) {
        	EventHandlerDao.instance.saveFull(vo);
            response.addData("handler", vo);
        }

        return response;
    }

    @DwrPermission(user = true)
    public void deleteEventHandler(int handlerId) {
        User user = Common.getHttpUser();
        List<EventType> eventTypes = EventHandlerDao.instance.getEventTypesForHandler(handlerId);
        for(EventType et : eventTypes)
            Permissions.ensureEventTypePermission(user, et);
        EventHandlerDao.instance.delete(handlerId);
    }

    @DwrPermission(user = true)
    public TranslatableMessage testProcessCommand(String command, int timeout) {
    	
    	//Ensure only Data Source Level users can access this
    	Permissions.ensureDataSourcePermission(Common.getHttpUser());
    	
        if (StringUtils.isBlank(command))
            return null;

        try {
            ProcessWorkItem.executeProcessCommand(command, timeout);
            return new TranslatableMessage("eventHandlers.commandTest.result");
        }
        catch (IOException e) {
            LOG.warn("Process error", e);
            return new TranslatableMessage("common.default", e.getMessage());
        }
    }
    
    @DwrPermission(user = true)
    public ProcessResult validateScript(String script, Integer targetPointId, int type, 
            List<IntStringPair> additionalContext, ScriptPermissions scriptPermissions) {
        ProcessResult response = new ProcessResult();
        TranslatableMessage message;

        Map<String, IDataPointValueSource> context = new HashMap<String, IDataPointValueSource>();
        int targetDataType;
        if(type == SetPointEventHandlerDefinition.ACTIVE_SCRIPT_TYPE || type == SetPointEventHandlerDefinition.INACTIVE_SCRIPT_TYPE) {
            DataPointRT target = targetPointId == null ? null : Common.runtimeManager.getDataPoint(targetPointId.intValue());
            if(target == null){
                DataPointVO targetVo = targetPointId == null ? null : DataPointDao.instance.getDataPoint(targetPointId.intValue(), false);
                if(targetVo == null) {
                    if(type == SetPointEventHandlerDefinition.ACTIVE_SCRIPT_TYPE) //These are passed in the validateScript of eventHandlers.jsp
                        response.addMessage("activeScript", new TranslatableMessage("eventHandlers.noTargetPoint"));
                    else if(type == SetPointEventHandlerDefinition.INACTIVE_SCRIPT_TYPE)
                        response.addMessage("inactiveScript", new TranslatableMessage("eventHandlers.noTargetPoint"));
                    return response;
                }
                if(targetVo.getDefaultCacheSize() == 0)
                    targetVo.setDefaultCacheSize(1);
                target = new DataPointRT(targetVo, targetVo.getPointLocator().createRuntime(), DataSourceDao.instance.getDataSource(targetVo.getDataSourceId()), null);
                target.resetValues();
                context.put(SetPointEventHandlerVO.TARGET_CONTEXT_KEY, target);
            }
            targetDataType = target.getDataTypeId();
        } else {
            targetDataType = DataTypes.ALPHANUMERIC;
        }
        
        for(IntStringPair cxt : additionalContext) {
            DataPointRT dprt = Common.runtimeManager.getDataPoint(cxt.getKey());
            if(dprt == null) {
                DataPointVO dpvo = DataPointDao.instance.getDataPoint(cxt.getKey(), false);
                if(dpvo == null) {
                    if(type == SetPointEventHandlerDefinition.ACTIVE_SCRIPT_TYPE)
                        response.addMessage("activeScript", new TranslatableMessage("event.script.contextPointMissing", cxt.getValue(), cxt.getKey()));
                    else if(type == SetPointEventHandlerDefinition.INACTIVE_SCRIPT_TYPE)
                        response.addMessage("inactiveScript", new TranslatableMessage("event.script.contextPointMissing", cxt.getValue(), cxt.getKey()));
                    else if(type == SetPointEventHandlerDefinition.ACKNOWLEDGE_SCRIPT_TYPE)
                        response.addMessage("acknowledgeScript", new TranslatableMessage("event.script.contextPointMissing", cxt.getValue(), cxt.getKey()));
                    else if(type == EmailEventHandlerDefinition.EMAIL_SCRIPT_TYPE)
                        response.addMessage("emailScript", new TranslatableMessage("event.script.contextPointMissing", cxt.getValue(), cxt.getKey()));
                    return response;
                }
                if(dpvo.getDefaultCacheSize() == 0)
                    dpvo.setDefaultCacheSize(1);
                dprt = new DataPointRT(dpvo, dpvo.getPointLocator().createRuntime(), DataSourceDao.instance.getDataSource(dpvo.getDataSourceId()), null);
                dprt.resetValues();
            }
            context.put(cxt.getValue(), dprt);
        }
        
        Map<String, Object> otherContext = new HashMap<String, Object>();
        otherContext.put(SetPointEventHandlerVO.EVENT_CONTEXT_KEY, getTestEvent());
        if(type == EmailEventHandlerDefinition.EMAIL_SCRIPT_TYPE) {
            otherContext.put("model", new HashMap<String, Object>());
            otherContext.put(EmailHandlerRT.DO_NOT_SEND_KEY, CompiledScriptExecutor.UNCHANGED);
        }
         

        final StringWriter scriptOut = new StringWriter();
        final PrintWriter scriptWriter = new PrintWriter(scriptOut);
        
        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYY HH:mm:ss");
        ScriptPointValueSetter loggingSetter = new ScriptPointValueSetter(scriptPermissions) {
            @Override
            public void set(IDataPointValueSource point, Object value, long timestamp, String annotation) {
                DataPointRT dprt = (DataPointRT) point;
                if(!dprt.getVO().getPointLocator().isSettable()) {
                    scriptOut.append("Point " + dprt.getVO().getExtendedName() + " not settable.");
                    return;
                }
                
                if(!Permissions.hasPermission(dprt.getVO().getSetPermission(), permissions.getDataPointSetPermissions())) {
                    scriptOut.write(new TranslatableMessage("pointLinks.setTest.permissionDenied", dprt.getVO().getXid()).translate(Common.getTranslations()));
                    return;
                }

                scriptOut.append("Setting point " + dprt.getVO().getName() + " to " + value + " @" + sdf.format(new Date(timestamp)) + "\r\n");
            }

            @Override
            protected void setImpl(IDataPointValueSource point, Object value, long timestamp, String annotation) {
                // not really setting
            }
        };

        try {
            CompiledScript compiledScript = CompiledScriptExecutor.compile(script);
            PointValueTime pvt = CompiledScriptExecutor.execute(compiledScript, context, otherContext, System.currentTimeMillis(),
                    targetDataType, System.currentTimeMillis(), scriptPermissions, scriptWriter, 
                    new ScriptLog(SetPointHandlerRT.NULL_WRITER, LogLevel.FATAL), loggingSetter, null, true);
            if (pvt.getValue() == null)
                message = new TranslatableMessage("eventHandlers.script.nullResult");
            else if(CompiledScriptExecutor.UNCHANGED == pvt.getValue()) {
                if(type == EmailEventHandlerDefinition.EMAIL_SCRIPT_TYPE)
                    message = new TranslatableMessage("eventHandlers.script.successNoEmail");
                else
                    message = new TranslatableMessage("eventHandlers.script.successUnchanged");
            } else
                message = new TranslatableMessage("eventHandlers.script.success", pvt.getValue());
            //Add the script logging output
            response.addData("out", scriptOut.toString().replaceAll("\n", "<br/>"));
        }
        catch(ScriptPermissionsException e) {
            message = e.getTranslatableMessage();
        }
        catch (ScriptException e) {
            message = new TranslatableMessage("eventHandlers.script.failure", e.getMessage());
        }
        catch (ResultTypeException e) {
            message = e.getTranslatableMessage();
        }

        if(type == SetPointEventHandlerDefinition.ACTIVE_SCRIPT_TYPE)
            response.addMessage("activeScript", message);
        else if(type == SetPointEventHandlerDefinition.INACTIVE_SCRIPT_TYPE)
            response.addMessage("inactiveScript", message);
        else if(type == SetPointEventHandlerDefinition.ACKNOWLEDGE_SCRIPT_TYPE)
            response.addMessage("acknowledgeScript", message);
        else if(type == EmailEventHandlerDefinition.EMAIL_SCRIPT_TYPE)
            response.addMessage("emailScript", message);
        return response;
    }
    
    private EventInstanceWrapper getTestEvent() {
    	EventInstance ei = new EventInstance(new EventType() {

			@Override
			public String getEventType() {
				return EventType.EventTypeNames.SYSTEM;
			}

			@Override
			public String getEventSubtype() {
				return "Set Point Handler Test";
			}

			@Override
			public int getDuplicateHandling() {
				return DuplicateHandling.IGNORE;
			}

			@Override
			public int getReferenceId1() {
				return -1;
			}

			@Override
			public int getReferenceId2() {
				return -2;
			}

			@Override
			public EventTypeModel asModel() {
				return null; //Not to be used
			}
			/* (non-Javadoc)
			 * @see com.serotonin.m2m2.rt.event.type.EventType#hasPermission(com.serotonin.m2m2.vo.User)
			 */
			@Override
			public boolean hasPermission(User user) {
			    return true;
			}
		}, Common.timer.currentTimeMillis(), false, AlarmLevels.INFORMATION, 
    			new TranslatableMessage("common.test"), null);
    	return new EventInstanceWrapper(ei);
    }
}
