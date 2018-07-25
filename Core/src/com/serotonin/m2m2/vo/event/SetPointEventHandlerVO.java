/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonBoolean;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.SetPointHandlerRT;
import com.serotonin.m2m2.rt.script.CompiledScriptExecutor;
import com.serotonin.m2m2.rt.script.ScriptPermissions;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.util.VarNames;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers.AbstractEventHandlerModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers.SetPointEventHandlerModel;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 *
 */
public class SetPointEventHandlerVO extends AbstractEventHandlerVO<SetPointEventHandlerVO>{

    public static final int SET_ACTION_NONE = 0;
    public static final int SET_ACTION_POINT_VALUE = 1;
    public static final int SET_ACTION_STATIC_VALUE = 2;
    public static final int SET_ACTION_SCRIPT_VALUE = 3;
    
    public static final String TARGET_CONTEXT_KEY = "target";
    public static final String EVENT_CONTEXT_KEY = "event";

    public static ExportCodes SET_ACTION_CODES = new ExportCodes();
    static {
        SET_ACTION_CODES.addElement(SET_ACTION_NONE, "NONE", "eventHandlers.action.none");
        SET_ACTION_CODES.addElement(SET_ACTION_POINT_VALUE, "POINT_VALUE", "eventHandlers.action.point");
        SET_ACTION_CODES.addElement(SET_ACTION_STATIC_VALUE, "STATIC_VALUE", "eventHandlers.action.static");
        SET_ACTION_CODES.addElement(SET_ACTION_SCRIPT_VALUE, "SCRIPT_VALUE", "eventHandlers.action.script");
    }
	
    private int targetPointId;
    private int activeAction;
    private String activeValueToSet;
    private int activePointId;
    private int inactiveAction;
    private String inactiveValueToSet;
    private int inactivePointId;
    private int acknowledgeAction;
    private boolean acknowledgeActionEvenIfInactive;
    private String acknowledgeValueToSet;
    private int acknowledgePointId;
    private String activeScript;
    private String inactiveScript;
    private String acknowledgeScript;
    private ScriptPermissions scriptPermissions;
    private List<IntStringPair> additionalContext;
    
    public int getTargetPointId() {
        return targetPointId;
    }

    public void setTargetPointId(int targetPointId) {
        this.targetPointId = targetPointId;
    }

    public int getActiveAction() {
        return activeAction;
    }

    public void setActiveAction(int activeAction) {
        this.activeAction = activeAction;
    }

    public int getInactiveAction() {
        return inactiveAction;
    }

    public void setInactiveAction(int inactiveAction) {
        this.inactiveAction = inactiveAction;
    }
    
    public String getActiveValueToSet() {
        return activeValueToSet;
    }

    public void setActiveValueToSet(String activeValueToSet) {
        this.activeValueToSet = activeValueToSet;
    }

    public int getActivePointId() {
        return activePointId;
    }

    public void setActivePointId(int activePointId) {
        this.activePointId = activePointId;
    }

    public String getInactiveValueToSet() {
        return inactiveValueToSet;
    }

    public void setInactiveValueToSet(String inactiveValueToSet) {
        this.inactiveValueToSet = inactiveValueToSet;
    }

    public int getInactivePointId() {
        return inactivePointId;
    }

    public void setInactivePointId(int inactivePointId) {
        this.inactivePointId = inactivePointId;
    }

    public int getAcknowledgeAction() {
        return acknowledgeAction;
    }

    public void setAcknowledgeAction(int acknowledgeAction) {
        this.acknowledgeAction = acknowledgeAction;
    }

    public boolean isAcknowledgeActionEvenIfInactive() {
        return acknowledgeActionEvenIfInactive;
    }

    public void setAcknowledgeActionEvenIfInactive(boolean acknowledgeActionEvenIfInactive) {
        this.acknowledgeActionEvenIfInactive = acknowledgeActionEvenIfInactive;
    }

    public String getAcknowledgeValueToSet() {
        return acknowledgeValueToSet;
    }

    public void setAcknowledgeValueToSet(String acknowledgeValueToSet) {
        this.acknowledgeValueToSet = acknowledgeValueToSet;
    }

    public int getAcknowledgePointId() {
        return acknowledgePointId;
    }

    public void setAcknowledgePointId(int acknowledgePointId) {
        this.acknowledgePointId = acknowledgePointId;
    }
    
    public String getActiveScript() {
    	return activeScript;
    }
    
    public void setActiveScript(String activeScript) {
    	this.activeScript = activeScript;
    }
    
    public String getInactiveScript() {
    	return inactiveScript;
    }
    
    public void setInactiveScript(String inactiveScript) {
    	this.inactiveScript = inactiveScript;
    }

    public String getAcknowledgeScript() {
        return acknowledgeScript;
    }

    public void setAcknowledgeScript(String acknowledgeScript) {
        this.acknowledgeScript = acknowledgeScript;
    }
    
    public ScriptPermissions getScriptPermissions() {
    	return scriptPermissions;
    }
    
    public void setScriptPermissions(ScriptPermissions scriptPermissions) {
    	this.scriptPermissions = scriptPermissions;
    }
    
    public List<IntStringPair> getAdditionalContext() {
    	return additionalContext;
    }
    
    public void setAdditionalContext(List<IntStringPair> additionalContext) {
    	this.additionalContext = additionalContext;
    }
    
    public void validate(ProcessResult response) {
    	super.validate(response);
        DataPointVO dp = DataPointDao.instance.getDataPoint(targetPointId, false);

        if (dp == null)
            response.addGenericMessage("eventHandlers.noTargetPoint");
        else {
            int dataType = dp.getPointLocator().getDataTypeId();

            if (activeAction == SET_ACTION_NONE && inactiveAction == SET_ACTION_NONE)
                response.addGenericMessage("eventHandlers.noSetPointAction");

            // Active
            if (activeAction == SET_ACTION_STATIC_VALUE && dataType == DataTypes.MULTISTATE) {
                try {
                    Integer.parseInt(activeValueToSet);
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidActiveValue");
                }
            }
            else if (activeAction == SET_ACTION_STATIC_VALUE && dataType == DataTypes.NUMERIC) {
                try {
                    Double.parseDouble(activeValueToSet);
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidActiveValue");
                }
            }
            else if (activeAction == SET_ACTION_POINT_VALUE) {
                DataPointVO dpActive = DataPointDao.instance.getDataPoint(activePointId, false);

                if (dpActive == null)
                    response.addGenericMessage("eventHandlers.invalidActiveSource");
                else if (dataType != dpActive.getPointLocator().getDataTypeId())
                    response.addGenericMessage("eventHandlers.invalidActiveSourceType");
            }
            else if (activeAction == SET_ACTION_SCRIPT_VALUE) {
            	if(StringUtils.isEmpty(activeScript))
            		response.addGenericMessage("eventHandlers.invalidActiveScript");
            	try {
            		CompiledScriptExecutor.compile(activeScript);
            	} catch(ScriptException e) {
            		response.addGenericMessage("eventHandlers.invalidActiveScriptError", e.getMessage() == null ? e.getCause().getMessage() : e.getMessage());
            	}
            }

            // Inactive
            if (inactiveAction == SET_ACTION_STATIC_VALUE && dataType == DataTypes.MULTISTATE) {
                try {
                    Integer.parseInt(inactiveValueToSet);
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidInactiveValue");
                }
            }
            else if (inactiveAction == SET_ACTION_STATIC_VALUE && dataType == DataTypes.NUMERIC) {
                try {
                    Double.parseDouble(inactiveValueToSet);
                }
                catch (NumberFormatException e) {
                    response.addGenericMessage("eventHandlers.invalidInactiveValue");
                }
            }
            else if (inactiveAction == SET_ACTION_POINT_VALUE) {
                DataPointVO dpInactive = DataPointDao.instance.getDataPoint(inactivePointId, false);

                if (dpInactive == null)
                    response.addGenericMessage("eventHandlers.invalidInactiveSource");
                else if (dataType != dpInactive.getPointLocator().getDataTypeId())
                    response.addGenericMessage("eventHandlers.invalidInactiveSourceType");
            }
            else if (inactiveAction == SET_ACTION_SCRIPT_VALUE) {
            	if(StringUtils.isEmpty(inactiveScript))
            		response.addGenericMessage("eventHandlers.invalidInactiveScript");
            	try {
            		CompiledScriptExecutor.compile(inactiveScript);
            	} catch(ScriptException e) {
            		response.addGenericMessage("eventHandlers.invalidInactiveScriptError", e.getMessage() == null ? e.getCause().getMessage() : e.getMessage());
            	}
            }
            
            List<String> varNameSpace = new ArrayList<String>();
            varNameSpace.add(TARGET_CONTEXT_KEY);
            for(IntStringPair cxt : additionalContext) {
            	if(DataPointDao.instance.getDataPoint(cxt.getKey(), false) == null)
            		response.addGenericMessage("event.script.contextPointMissing", cxt.getKey(), cxt.getValue());
            	
            	String varName = cxt.getValue();
                if (StringUtils.isBlank(varName)) {
                    response.addGenericMessage("validate.allVarNames");
                    break;
                }

                if (!VarNames.validateVarName(varName)) {
                    response.addGenericMessage("validate.invalidVarName", varName);
                    break;
                }

                if (varNameSpace.contains(varName)) {
                    response.addGenericMessage("validate.duplicateVarName", varName);
                    break;
                }

                varNameSpace.add(varName);
            }
        }
    }
    
    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 4;
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(targetPointId);
        out.writeInt(activeAction);
        SerializationHelper.writeSafeUTF(out, activeValueToSet);
        out.writeInt(activePointId);
        out.writeInt(inactiveAction);
        SerializationHelper.writeSafeUTF(out, inactiveValueToSet);
        out.writeInt(inactivePointId);
        out.writeInt(acknowledgeAction);
        out.writeBoolean(acknowledgeActionEvenIfInactive);
        SerializationHelper.writeSafeUTF(out, acknowledgeValueToSet);
        out.writeInt(acknowledgePointId);
        SerializationHelper.writeSafeUTF(out, activeScript);
        SerializationHelper.writeSafeUTF(out, inactiveScript);
        SerializationHelper.writeSafeUTF(out, acknowledgeScript);
        out.writeObject(additionalContext);
        out.writeObject(scriptPermissions);
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            targetPointId = in.readInt();
            activeAction = in.readInt();
            activeValueToSet = SerializationHelper.readSafeUTF(in);
            activePointId = in.readInt();
            inactiveAction = in.readInt();
            inactiveValueToSet = SerializationHelper.readSafeUTF(in);
            inactivePointId = in.readInt();
            acknowledgeAction = SET_ACTION_NONE;
            acknowledgeActionEvenIfInactive = false;
            acknowledgeValueToSet = null;
            acknowledgePointId = Common.NEW_ID;
            activeScript = inactiveScript = acknowledgeScript = null;
            additionalContext = new ArrayList<IntStringPair>();
            scriptPermissions = new ScriptPermissions();
        } else if (ver == 2) {
            targetPointId = in.readInt();
            activeAction = in.readInt();
            activeValueToSet = SerializationHelper.readSafeUTF(in);
            activePointId = in.readInt();
            inactiveAction = in.readInt();
            inactiveValueToSet = SerializationHelper.readSafeUTF(in);
            inactivePointId = in.readInt();
            acknowledgeAction = SET_ACTION_NONE;
            acknowledgeActionEvenIfInactive = false;
            acknowledgeValueToSet = null;
            acknowledgePointId = Common.NEW_ID;
            activeScript = SerializationHelper.readSafeUTF(in);
            inactiveScript = SerializationHelper.readSafeUTF(in);
            acknowledgeScript = null;
            additionalContext = new ArrayList<IntStringPair>();
            scriptPermissions = new ScriptPermissions();
        } else if (ver == 3) {
            targetPointId = in.readInt();
            activeAction = in.readInt();
            activeValueToSet = SerializationHelper.readSafeUTF(in);
            activePointId = in.readInt();
            inactiveAction = in.readInt();
            inactiveValueToSet = SerializationHelper.readSafeUTF(in);
            inactivePointId = in.readInt();
            acknowledgeAction = SET_ACTION_NONE;
            acknowledgeActionEvenIfInactive = false;
            acknowledgeValueToSet = null;
            acknowledgePointId = Common.NEW_ID;
            activeScript = SerializationHelper.readSafeUTF(in);
            inactiveScript = SerializationHelper.readSafeUTF(in);
            acknowledgeScript = null;
            additionalContext = (List<IntStringPair>) in.readObject();
            scriptPermissions = (ScriptPermissions) in.readObject();
        } else if (ver == 4) {
            targetPointId = in.readInt();
            activeAction = in.readInt();
            activeValueToSet = SerializationHelper.readSafeUTF(in);
            activePointId = in.readInt();
            inactiveAction = in.readInt();
            inactiveValueToSet = SerializationHelper.readSafeUTF(in);
            inactivePointId = in.readInt();
            acknowledgeAction = in.readInt();
            acknowledgeActionEvenIfInactive = in.readBoolean();
            acknowledgeValueToSet = SerializationHelper.readSafeUTF(in);
            acknowledgePointId = in.readInt();
            activeScript = SerializationHelper.readSafeUTF(in);
            inactiveScript = SerializationHelper.readSafeUTF(in);
            acknowledgeScript = SerializationHelper.readSafeUTF(in);;
            additionalContext = (List<IntStringPair>) in.readObject();
            scriptPermissions = (ScriptPermissions) in.readObject();
        }
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
    	super.jsonWrite(writer);
    	
    	String dpXid = DataPointDao.instance.getXidById(targetPointId);
        writer.writeEntry("targetPointId", dpXid);

        // Active
        writer.writeEntry("activeAction", SET_ACTION_CODES.getCode(activeAction));
        if (activeAction == SET_ACTION_POINT_VALUE) {
            dpXid = DataPointDao.instance.getXidById(activePointId);
            writer.writeEntry("activePointId", dpXid);
        }
        else if (activeAction == SET_ACTION_STATIC_VALUE)
            writer.writeEntry("activeValueToSet", activeValueToSet);
        else if (activeAction == SET_ACTION_SCRIPT_VALUE)
        	writer.writeEntry("activeScript", activeScript);

        // Inactive
        writer.writeEntry("inactiveAction", SET_ACTION_CODES.getCode(inactiveAction));
        if (inactiveAction == SET_ACTION_POINT_VALUE) {
            dpXid = DataPointDao.instance.getXidById(inactivePointId);
            writer.writeEntry("inactivePointId", dpXid);
        }
        else if (inactiveAction == SET_ACTION_STATIC_VALUE)
            writer.writeEntry("inactiveValueToSet", inactiveValueToSet);
        else if (inactiveAction == SET_ACTION_SCRIPT_VALUE)
        	writer.writeEntry("inactiveScript", inactiveScript);
        
        // Acknowledge
        writer.writeEntry("acknowledgeAction", SET_ACTION_CODES.getCode(acknowledgeAction));
        writer.writeEntry("acknowledgeActionEvenIfInactive", acknowledgeActionEvenIfInactive);
        if (acknowledgeAction == SET_ACTION_POINT_VALUE) {
            dpXid = DataPointDao.instance.getXidById(acknowledgePointId);
            writer.writeEntry("acknowledgePointId", dpXid);
        }
        else if (acknowledgeAction == SET_ACTION_STATIC_VALUE)
            writer.writeEntry("acknowledgeValueToSet", acknowledgeValueToSet);
        else if (acknowledgeAction == SET_ACTION_SCRIPT_VALUE)
            writer.writeEntry("inactiveScript", acknowledgeScript);
        
        JsonArray context = new JsonArray();
        for(IntStringPair pnt : additionalContext) {
        	DataPointVO dpvo = DataPointDao.instance.getDataPoint(pnt.getKey(), false);
        	if(dpvo != null) {
        		JsonObject point = new JsonObject();
        		point.put("dataPointXid", dpvo.getXid());
        		point.put("contextKey", pnt.getValue());
        		context.add(point);
        	}
        }
        writer.writeEntry("additionalContext", context);
        
        if(scriptPermissions != null) {
            JsonObject permissions = new JsonObject();
            permissions.put(ScriptPermissions.DATA_SOURCE, scriptPermissions.getDataSourcePermissions());
            permissions.put(ScriptPermissions.DATA_POINT_READ, scriptPermissions.getDataPointReadPermissions());
            permissions.put(ScriptPermissions.DATA_POINT_SET, scriptPermissions.getDataPointSetPermissions());
            writer.writeEntry("scriptPermissions", permissions);
        } else {
            writer.writeEntry("scriptPermissions", null);
        }
    }
    
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        
    	DataPointDao dataPointDao = DataPointDao.instance;
        String xid = jsonObject.getString("targetPointId");
        if (xid != null) {
            Integer id = dataPointDao.getIdByXid(xid);
            if (id == null)
                throw new TranslatableJsonException("emport.error.missingPoint", xid);
            targetPointId = id;
        }

        // Active
        String text = jsonObject.getString("activeAction");
        if (text != null) {
            activeAction = SET_ACTION_CODES.getId(text);
            if (!SET_ACTION_CODES.isValidId(activeAction))
                throw new TranslatableJsonException("emport.error.eventHandler.invalid", "activeAction", text,
                        SET_ACTION_CODES.getCodeList());
        }

        if (activeAction == SET_ACTION_POINT_VALUE) {
            xid = jsonObject.getString("activePointId");
            if (xid != null) {
                Integer id = dataPointDao.getIdByXid(xid);
                if (id == null)
                    throw new TranslatableJsonException("emport.error.missingPoint", xid);
                activePointId = id;
            }
        }
        else if (activeAction == SET_ACTION_STATIC_VALUE) {
            text = jsonObject.getString("activeValueToSet");
            if (text != null)
                activeValueToSet = text;
        }
        else if (activeAction == SET_ACTION_SCRIPT_VALUE) {
            text = jsonObject.getString("activeScript");
            if (text == null)
            	throw new TranslatableJsonException("emport.error.eventHandler.invalid", "inactiveScript");
            activeValueToSet = text;
        }

        // Inactive
        text = jsonObject.getString("inactiveAction");
        if (text != null) {
            inactiveAction = SET_ACTION_CODES.getId(text);
            if (!SET_ACTION_CODES.isValidId(inactiveAction))
                throw new TranslatableJsonException("emport.error.eventHandler.invalid", "inactiveAction", text,
                        SET_ACTION_CODES.getCodeList());
        }

        if (inactiveAction == SET_ACTION_POINT_VALUE) {
            xid = jsonObject.getString("inactivePointId");
            if (xid != null) {
                Integer id = dataPointDao.getIdByXid(xid);
                if (id == null)
                    throw new TranslatableJsonException("emport.error.missingPoint", xid);
                inactivePointId = id;
            }
        }
        else if (inactiveAction == SET_ACTION_STATIC_VALUE) {
            text = jsonObject.getString("inactiveValueToSet");
            if (text != null)
                inactiveValueToSet = text;
        }
        else if (inactiveAction == SET_ACTION_SCRIPT_VALUE) {
            text = jsonObject.getString("inactiveScript");
            if (text == null)
            	throw new TranslatableJsonException("emport.error.eventHandler.invalid", "inactiveScript");
            inactiveValueToSet = text;
        }
        
        //Acknowledge
        text = jsonObject.getString("acknowledgeAction");
        if (text != null) {
            acknowledgeAction = SET_ACTION_CODES.getId(text);
            if (!SET_ACTION_CODES.isValidId(acknowledgeAction))
                throw new TranslatableJsonException("emport.error.eventHandler.invalid", "acknowledgeAction", text,
                        SET_ACTION_CODES.getCodeList());
        }
        
        JsonBoolean b = jsonObject.getJsonBoolean("acknowledgeActionEvenIfInactive");
        if (b != null)
            acknowledgeActionEvenIfInactive = b.booleanValue();
        
        if (acknowledgeAction == SET_ACTION_POINT_VALUE) {
            xid = jsonObject.getString("acknowledgePointId");
            if (xid != null) {
                Integer id = dataPointDao.getIdByXid(xid);
                if (id == null)
                    throw new TranslatableJsonException("emport.error.missingPoint", xid);
                acknowledgePointId = id;
            }
        }
        else if (acknowledgeAction == SET_ACTION_STATIC_VALUE) {
            text = jsonObject.getString("acknowledgeValueToSet");
            if (text != null)
                acknowledgeValueToSet = text;
        }
        else if (acknowledgeAction == SET_ACTION_SCRIPT_VALUE) {
            text = jsonObject.getString("acknowledgeScript");
            if (text == null)
                throw new TranslatableJsonException("emport.error.eventHandler.invalid", "acknowledgeScript");
            acknowledgeValueToSet = text;
        }

        JsonArray context = jsonObject.getJsonArray("additionalContext");
        if(context != null) {
        	List<IntStringPair> additionalContext = new ArrayList<>();
        	for(JsonValue jv : context) {
        		JsonObject jo = jv.toJsonObject();
        		String dataPointXid = jo.getString("dataPointXid");
        		if(dataPointXid == null)
        			throw new TranslatableJsonException("emport.error.context.missing", "dataPointXid");
        		
        		Integer id = DataPointDao.instance.getIdByXid(dataPointXid);
        		if(id == null)
        			throw new TranslatableJsonException("emport.error.missingPoint", dataPointXid);
        		
        		String contextKey = jo.getString("contextKey");
        		if(contextKey == null)
        			throw new TranslatableJsonException("emport.error.context.missing", "contextKey");
        		
        		additionalContext.add(new IntStringPair(id, contextKey));
        	}
        	this.additionalContext = additionalContext;
        } else
        	this.additionalContext = new ArrayList<>();
        
        JsonObject permissions = jsonObject.getJsonObject("scriptPermissions");
        ScriptPermissions scriptPermissions = new ScriptPermissions();
        if(permissions != null) {
        	String perm = permissions.getString(ScriptPermissions.DATA_SOURCE);
        	if(perm != null)
        		scriptPermissions.setDataSourcePermissions(perm);
        	perm = permissions.getString(ScriptPermissions.DATA_POINT_READ);
        	if(perm != null)
        		scriptPermissions.setDataPointReadPermissions(perm);
        	perm = permissions.getString(ScriptPermissions.DATA_POINT_SET);
        	if(perm != null)
        		scriptPermissions.setDataPointSetPermissions(perm);
        }
    	this.scriptPermissions = scriptPermissions;
    }
    
    
    @Override
    public EventHandlerRT<SetPointEventHandlerVO> createRuntime(){
    	return new SetPointHandlerRT(this);
    }

    
    public static TranslatableMessage getSetActionMessage(int action) {
        switch (action) {
        case SET_ACTION_NONE:
            return new TranslatableMessage("eventHandlers.action.none");
        case SET_ACTION_POINT_VALUE:
            return new TranslatableMessage("eventHandlers.action.point");
        case SET_ACTION_STATIC_VALUE:
            return new TranslatableMessage("eventHandlers.action.static");
        }
        return new TranslatableMessage("common.unknown");
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.AbstractEventHandlerVO#asModel()
	 */
	@Override
	public AbstractEventHandlerModel<?> asModel() {
		return new SetPointEventHandlerModel(this);
	}
}
