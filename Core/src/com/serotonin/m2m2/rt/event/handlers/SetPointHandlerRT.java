/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.util.ConfigurationExportData;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.io.NullWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.SetPointWorkItem;
import com.serotonin.m2m2.rt.script.CompiledScriptExecutor;
import com.serotonin.m2m2.rt.script.EventInstanceWrapper;
import com.serotonin.m2m2.rt.script.JsonImportExclusion;
import com.serotonin.m2m2.rt.script.OneTimePointAnnotation;
import com.serotonin.m2m2.rt.script.ResultTypeException;
import com.serotonin.m2m2.rt.script.ScriptLog;
import com.serotonin.m2m2.rt.script.ScriptLog.LogLevel;
import com.serotonin.m2m2.rt.script.ScriptPermissions;
import com.serotonin.m2m2.rt.script.ScriptPermissionsException;
import com.serotonin.m2m2.rt.script.ScriptPointValueSetter;
import com.serotonin.m2m2.rt.script.ScriptUtils;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;

public class SetPointHandlerRT extends EventHandlerRT<SetPointEventHandlerVO> implements SetPointSource {
    private static final Log LOG = LogFactory.getLog(SetPointHandlerRT.class);
    private CompiledScript activeScript;
    private CompiledScript inactiveScript;
    private CompiledScript acknowledgeScript;
    private final List<JsonImportExclusion> importExclusions;
    public static final PrintWriter NULL_WRITER = new PrintWriter(new NullWriter());
    private final SetCallback setCallback;

    public SetPointHandlerRT(SetPointEventHandlerVO vo) {
        super(vo);
        if(vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
        	try {
        		activeScript = CompiledScriptExecutor.compile(vo.getActiveScript());
        	} catch(ScriptException e) {
        		raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidActiveScriptError", 
        				e.getMessage() == null ? e.getCause().getMessage() : e.getMessage()), null);
        	}
        } else
        	activeScript = null;
        
        if(vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
        	try {
        		inactiveScript = CompiledScriptExecutor.compile(vo.getInactiveScript());
        	} catch(ScriptException e) {
        		raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidInactiveScriptError", 
        				e.getMessage() == null ? e.getCause().getMessage() : e.getMessage()), null);
        	}
        } else
        	inactiveScript = null;
        
        if(activeScript != null || inactiveScript != null) {
        	importExclusions = new ArrayList<JsonImportExclusion>();
        	importExclusions.add(new JsonImportExclusion("xid", vo.getXid()) {
				@Override
				public String getImporterType() {
					return ConfigurationExportData.EVENT_HANDLERS;
				}
        	});
        } else
        	importExclusions = null;
        
        if(activeScript != null || inactiveScript != null)
        	setCallback = new SetCallback(vo.getScriptPermissions());
        else
        	setCallback = null;
    }

    @Override
    public void eventRaised(EventInstance evt) {
        if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE)
            return;

        // Validate that the target point is available.
        DataPointRT targetPoint = Common.runtimeManager.getDataPoint(vo.getTargetPointId());
        if (targetPoint == null) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetPointMissing"), evt.getEventType());
            return;
        }

        if (!targetPoint.getPointLocator().isSettable()) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetNotSettable"), evt.getEventType());
            return;
        }

        int targetDataType = targetPoint.getVO().getPointLocator().getDataTypeId();

        DataValue value;
        if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
            // Get the source data point.
            DataPointRT sourcePoint = Common.runtimeManager.getDataPoint(vo.getActivePointId());
            if (sourcePoint == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.activePointMissing"), evt.getEventType());
                return;
            }

            PointValueTime valueTime = sourcePoint.getPointValue();
            if (valueTime == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.activePointValue"), evt.getEventType());
                return;
            }

            if (DataTypes.getDataType(valueTime.getValue()) != targetDataType) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.activePointDataType"), evt.getEventType());
                return;
            }

            value = valueTime.getValue();
        }
        else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE) {
            value = DataValue.stringToValue(vo.getActiveValueToSet(), targetDataType);
        }
        else if (vo.getActiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
        	if(activeScript == null) {
        		raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidActiveScript"), evt.getEventType());
        		return;
        	}
        	Map<String, IDataPointValueSource> context = new HashMap<String, IDataPointValueSource>();
        	context.put(SetPointEventHandlerVO.TARGET_CONTEXT_KEY, targetPoint);
        	Map<String, Object> additionalContext = new HashMap<String, Object>();
        	additionalContext.put(SetPointEventHandlerVO.EVENT_CONTEXT_KEY, new EventInstanceWrapper(evt));
        	try {
        		for(IntStringPair cxt : vo.getAdditionalContext()) {
        			DataPointRT dprt = Common.runtimeManager.getDataPoint(cxt.getKey());
        			if(dprt != null)
        				context.put(cxt.getValue(), dprt);
        		}
	        	PointValueTime pvt = CompiledScriptExecutor.execute(activeScript, context, additionalContext, evt.getActiveTimestamp(), 
	        			targetPoint.getDataTypeId(), evt.getActiveTimestamp(), vo.getScriptPermissions(), NULL_WRITER, new ScriptLog(NULL_WRITER, LogLevel.FATAL), 
	        			setCallback, importExclusions, false);

	        	value = pvt.getValue();
        	} catch(ScriptPermissionsException e) {
                raiseFailureEvent(e.getTranslatableMessage(), evt.getEventType());
                return;
            } catch(ScriptException e) {
        		raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidActiveScriptError", e.getCause().getMessage()), evt.getEventType());
        		return;
        	} catch(ResultTypeException e) {
        		raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidActiveScriptError", e.getMessage()), evt.getEventType());
        		return;
        	}
        }
        else
            throw new ShouldNeverHappenException("Unknown active action: " + vo.getActiveAction());

        // Queue a work item to perform the set point.
        if(CompiledScriptExecutor.UNCHANGED != value)
        	Common.backgroundProcessing.addWorkItem(new SetPointWorkItem(vo.getTargetPointId(), new PointValueTime(value,
                evt.getActiveTimestamp()), this));
    }

    @Override
    public void eventInactive(EventInstance evt) {
        if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_NONE)
            return;

        // Validate that the target point is available.
        DataPointRT targetPoint = Common.runtimeManager.getDataPoint(vo.getTargetPointId());
        if (targetPoint == null) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetPointMissing"), evt.getEventType());
            return;
        }

        if (!targetPoint.getPointLocator().isSettable()) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetNotSettable"), evt.getEventType());
            return;
        }

        int targetDataType = targetPoint.getVO().getPointLocator().getDataTypeId();

        DataValue value;
        if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
            // Get the source data point.
            DataPointRT sourcePoint = Common.runtimeManager.getDataPoint(vo.getInactivePointId());
            if (sourcePoint == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.inactivePointMissing"), evt.getEventType());
                return;
            }

            PointValueTime valueTime = sourcePoint.getPointValue();
            if (valueTime == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.inactivePointValue"), evt.getEventType());
                return;
            }

            if (DataTypes.getDataType(valueTime.getValue()) != targetDataType) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.inactivePointDataType"), evt.getEventType());
                return;
            }

            value = valueTime.getValue();
        }
        else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE)
            value = DataValue.stringToValue(vo.getInactiveValueToSet(), targetDataType);
        else if (vo.getInactiveAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
        	if(inactiveScript == null) {
        		raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidInactiveScript"), evt.getEventType());
        		return;
        	}
        	Map<String, IDataPointValueSource> context = new HashMap<String, IDataPointValueSource>();
        	context.put("target", targetPoint);
        	try {
	        	PointValueTime pvt = CompiledScriptExecutor.execute(inactiveScript, context, new HashMap<String, Object>(), evt.getRtnTimestamp(), 
	        			targetPoint.getDataTypeId(), evt.getRtnTimestamp(), vo.getScriptPermissions(), NULL_WRITER, new ScriptLog(NULL_WRITER, LogLevel.FATAL),
	        			setCallback, importExclusions, false);
	        	value = pvt.getValue();
        	} catch(ScriptPermissionsException e) {
        	    raiseFailureEvent(e.getTranslatableMessage(), evt.getEventType());
        	    return;
        	} catch(ScriptException e) {
        		raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidInactiveScriptError", e.getCause().getMessage()), evt.getEventType());
        		return;
        	} catch(ResultTypeException e) {
        		raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidInactiveScriptError", e.getMessage()), evt.getEventType());
        		return;
        	}
        }
        else
            throw new ShouldNeverHappenException("Unknown inactive action: " + vo.getInactiveAction());

        Common.backgroundProcessing.addWorkItem(new SetPointWorkItem(vo.getTargetPointId(), new PointValueTime(value,
                evt.getRtnTimestamp()), this));
    }
    
    @Override
    public void eventAcknowledged(EventInstance evt) {
        if (vo.getAcknowledgeAction() == SetPointEventHandlerVO.SET_ACTION_NONE || (!evt.isActive() && !vo.isAcknowledgeActionEvenIfInactive()))
            return;
        
        // Validate that the target point is available.
        DataPointRT targetPoint = Common.runtimeManager.getDataPoint(vo.getTargetPointId());
        if (targetPoint == null) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetPointMissing"), evt.getEventType());
            return;
        }

        if (!targetPoint.getPointLocator().isSettable()) {
            raiseFailureEvent(new TranslatableMessage("event.setPoint.targetNotSettable"), evt.getEventType());
            return;
        }

        int targetDataType = targetPoint.getVO().getPointLocator().getDataTypeId();

        DataValue value;
        if (vo.getAcknowledgeAction() == SetPointEventHandlerVO.SET_ACTION_POINT_VALUE) {
            // Get the source data point.
            DataPointRT sourcePoint = Common.runtimeManager.getDataPoint(vo.getAcknowledgePointId());
            if (sourcePoint == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.acknowledgePointMissing"), evt.getEventType());
                return;
            }

            PointValueTime valueTime = sourcePoint.getPointValue();
            if (valueTime == null) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.acknowledgePointValue"), evt.getEventType());
                return;
            }

            if (DataTypes.getDataType(valueTime.getValue()) != targetDataType) {
                raiseFailureEvent(new TranslatableMessage("event.setPoint.acknowledgePointDataType"), evt.getEventType());
                return;
            }

            value = valueTime.getValue();
        }
        else if (vo.getAcknowledgeAction() == SetPointEventHandlerVO.SET_ACTION_STATIC_VALUE)
            value = DataValue.stringToValue(vo.getAcknowledgeValueToSet(), targetDataType);
        else if (vo.getAcknowledgeAction() == SetPointEventHandlerVO.SET_ACTION_SCRIPT_VALUE) {
            if(acknowledgeScript == null) {
                raiseFailureEvent(new TranslatableMessage("eventHandlers.acknowledgeInactiveScript"), evt.getEventType());
                return;
            }
            Map<String, IDataPointValueSource> context = new HashMap<String, IDataPointValueSource>();
            context.put("target", targetPoint);
            try {
                PointValueTime pvt = CompiledScriptExecutor.execute(acknowledgeScript, context, new HashMap<String, Object>(), evt.getRtnTimestamp(), 
                        targetPoint.getDataTypeId(), evt.getRtnTimestamp(), vo.getScriptPermissions(), NULL_WRITER, new ScriptLog(NULL_WRITER, LogLevel.FATAL),
                        setCallback, importExclusions, false);
                value = pvt.getValue();
            } catch(ScriptPermissionsException e) {
                raiseFailureEvent(e.getTranslatableMessage(), evt.getEventType());
                return;
            } catch(ScriptException e) {
                raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidAcknowledgeScriptError", e.getCause().getMessage()), evt.getEventType());
                return;
            } catch(ResultTypeException e) {
                raiseFailureEvent(new TranslatableMessage("eventHandlers.invalidAcknowledgeScriptError", e.getMessage()), evt.getEventType());
                return;
            }
        }
        else
            throw new ShouldNeverHappenException("Unknown acknowledge action: " + vo.getAcknowledgeAction());

        Common.backgroundProcessing.addWorkItem(new SetPointWorkItem(vo.getTargetPointId(), new PointValueTime(value,
                evt.getRtnTimestamp()), this));
    }

    private void raiseFailureEvent(TranslatableMessage message, EventType et) {
        if (et != null && et.isSystemMessage()) {
            if (((SystemEventType) et).getSystemEventType().equals(SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE)) {
                // The set point attempt failed for an event that is a set point handler failure in the first place.
                // Do not propagate the event, but rather just write a log message.
                LOG.warn("A set point event due to a set point handler failure itself failed. The failure event "
                        + "has been discarded: " + message.translate(Common.getTranslations()));
                return;
            }
        }

        SystemEventType eventType = new SystemEventType(SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE, vo.getId());
        if (StringUtils.isBlank(vo.getAlias()))
            message = new TranslatableMessage("event.setPointFailed", message);
        else
            message = new TranslatableMessage("event.setPointFailed.alias", vo.getAlias(), message);
        SystemEventType.raiseEvent(eventType, Common.timer.currentTimeMillis(), false, message);
    }

    //
    // SetPointSource implementation
    //
    @Override
    public String getSetPointSourceType() {
        return "SET_POINT_EVENT_HANDLER";
    }

    public int getSetPointSourceId() {
        return vo.getId();
    }

    @Override
    public TranslatableMessage getSetPointSourceMessage() {
        return new TranslatableMessage("annotation.eventHandler");
    }

    public void raiseRecursionFailureEvent() {
        raiseFailureEvent(new TranslatableMessage("event.setPoint.recursionFailure"), null);
    }
    
    class SetCallback extends ScriptPointValueSetter {
        public SetCallback(ScriptPermissions permissions) {
			super(permissions);
		}

		/*
         * (non-Javadoc)
         * 
         * @see
         * com.serotonin.mango.util.script.ScriptPointValueSetter#setImpl(com.serotonin.mango.rt.dataImage.IDataPointValueSource,
         * java.lang.Object, long)
         */
        @Override
        public void setImpl(IDataPointValueSource point, Object value, long timestamp, String annotation) {
            DataPointRT dprt = (DataPointRT) point;

            // We may, however, need to coerce the given value.
            try {
                DataValue mangoValue = ScriptUtils.coerce(value, dprt.getDataTypeId());
                SetPointSource source;
                PointValueTime newValue = new PointValueTime(mangoValue, timestamp);
                if(StringUtils.isBlank(annotation))
                	source = SetPointHandlerRT.this;
                else
                	source = new OneTimePointAnnotation(SetPointHandlerRT.this, annotation);
                
                DataSourceRT<? extends DataSourceVO<?>> dsrt = Common.runtimeManager.getRunningDataSource(dprt.getDataSourceId());
                dsrt.setPointValue(dprt, newValue, source);
            }
            catch (ResultTypeException e) {
                // Raise an event
            	raiseFailureEvent(e.getTranslatableMessage(), null);
            }
        }
    }
}
