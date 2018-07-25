/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers;

import java.util.List;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.rt.script.ScriptPermissions;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;

/**
 * 
 * @author Terry Packer
 */
public class SetPointEventHandlerModel extends AbstractEventHandlerModel<SetPointEventHandlerVO>{
	
	/**
	 * @param data
	 */
	public SetPointEventHandlerModel(SetPointEventHandlerVO data) {
		super(data);
	}
	
	public SetPointEventHandlerModel(){
		super(new SetPointEventHandlerVO());
	}
	
    public int getTargetPointId() {
        return this.data.getTargetPointId();
    }

    public void setTargetPointId(int targetPointId) {
        this.data.setTargetPointId(targetPointId);;
    }

    public String getActiveAction() {
        return SetPointEventHandlerVO.SET_ACTION_CODES.getCode(this.data.getActiveAction());
    }

    public void setActiveAction(String activeAction) {
        this.data.setActiveAction(SetPointEventHandlerVO.SET_ACTION_CODES.getId(activeAction));
    }

    public String getInactiveAction() {
        return SetPointEventHandlerVO.SET_ACTION_CODES.getCode(this.data.getInactiveAction());
    }

    public void setInactiveAction(String inactiveAction) {
        this.data.setInactiveAction(SetPointEventHandlerVO.SET_ACTION_CODES.getId(inactiveAction));
    }
    
    public String getAcknowledgeAction() {
        return SetPointEventHandlerVO.SET_ACTION_CODES.getCode(this.data.getAcknowledgeAction());
    }

    public void setAcknowledgeAction(String acknowledgeAction) {
        this.data.setAcknowledgeAction(SetPointEventHandlerVO.SET_ACTION_CODES.getId(acknowledgeAction));
    }
    
    public String getActiveValueToSet() {
        return this.data.getActiveValueToSet();
    }

    public void setActiveValueToSet(String activeValueToSet) {
        this.data.setActiveValueToSet(activeValueToSet);
    }

    public int getActivePointId() {
        return this.data.getActivePointId();
    }

    public void setActivePointId(int activePointId) {
        this.data.setActivePointId(activePointId);
    }

    public String getInactiveValueToSet() {
        return this.data.getInactiveValueToSet();
    }

    public void setInactiveValueToSet(String inactiveValueToSet) {
        this.data.setInactiveValueToSet(inactiveValueToSet);
    }

    public int getInactivePointId() {
        return this.data.getInactivePointId();
    }

    public void setInactivePointId(int inactivePointId) {
        this.data.setInactivePointId(inactivePointId);
    }
    
    public String getAcknowledgeValueToSet() {
        return this.data.getAcknowledgeValueToSet();
    }

    public void setAcknowledgeValueToSet(String acknowledgeValueToSet) {
        this.data.setAcknowledgeValueToSet(acknowledgeValueToSet);
    }

    public int getAcknowledgePointId() {
        return this.data.getAcknowledgePointId();
    }

    public void setAcknowledgePointId(int acknowledgePointId) {
        this.data.setAcknowledgePointId(acknowledgePointId);
    }
    
    public boolean isAcknowledgeActionEvenIfInactive() {
        return this.data.isAcknowledgeActionEvenIfInactive();
    }
    
    public void setAcknowledgeActionEvenIfInactive(boolean acknowledgeActionEvenIfInactive) {
        this.data.setAcknowledgeActionEvenIfInactive(acknowledgeActionEvenIfInactive);
    }
    
    public String getActiveScript(){
    	return this.data.getActiveScript();
    }
    
    public void setActiveScript(String activeScript){
    	this.data.setActiveScript(activeScript);
    }
    
    public String getInactiveScript(){
    	return this.data.getInactiveScript();
    }
    
    public void setInactiveScript(String inactiveScript){
    	this.data.setInactiveScript(inactiveScript);
    }
    
    public String getAcknowledgeScript(){
        return this.data.getAcknowledgeScript();
    }
    
    public void setAcknowledgeScript(String acknowledgeScript){
        this.data.setAcknowledgeScript(acknowledgeScript);
    }
    
    public List<IntStringPair> getAdditionalContext() {
    	return this.data.getAdditionalContext();
    }
    
    public void setAdditionalContext(List<IntStringPair> additionalContext) {
    	this.data.setAdditionalContext(additionalContext);
    }
    
    public ScriptPermissions getScriptPermissions() {
    	return this.data.getScriptPermissions();
    }
    
    public void setScriptPermissions(ScriptPermissions scriptPermissions) {
    	this.data.setScriptPermissions(scriptPermissions);
    }
}
