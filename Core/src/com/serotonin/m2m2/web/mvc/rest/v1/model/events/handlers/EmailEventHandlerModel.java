/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;
import com.serotonin.m2m2.web.mvc.rest.v1.model.email.EmailRecipientModel;

/**
 * 
 * @author Terry Packer
 */
@Deprecated
public class EmailEventHandlerModel extends AbstractEventHandlerModel<EmailEventHandlerVO>{

	/**
	 * @param data
	 */
	public EmailEventHandlerModel(EmailEventHandlerVO data) {
		super(data);
	}

	public EmailEventHandlerModel() {
		super(new EmailEventHandlerVO());
	}

    public List<EmailRecipientModel<?>> getActiveRecipients() {
        if(this.data.getActiveRecipients() == null)
            return null;
    	List<EmailRecipientModel<?>> models = new ArrayList<EmailRecipientModel<?>>();
        for(RecipientListEntryBean b : this.data.getActiveRecipients()) {
            EmailRecipientModel<?> model = EmailRecipientModel.createModel(b);
            if(model != null)
                models.add(model);
        }
    	return models;
    }

    public void setActiveRecipients(List<EmailRecipientModel<?>> activeRecipients) {
        List<RecipientListEntryBean> list = new ArrayList<RecipientListEntryBean>(activeRecipients.size());
        for(EmailRecipientModel<?> m : activeRecipients)
        	list.add(EmailRecipientModel.createBean(m));
        this.data.setActiveRecipients(list);
    }

    public int getEscalationDelay() {
        return this.data.getEscalationDelay();
    }

    public void setEscalationDelay(int escalationDelay) {
        this.data.setEscalationDelay(escalationDelay);
    }

    public String getEscalationDelayType() {
        return Common.TIME_PERIOD_CODES.getCode(this.data.getEscalationDelayType());
    }

    public void setEscalationDelayType(String escalationDelayType) {
        this.data.setEscalationDelayType(Common.TIME_PERIOD_CODES.getId(escalationDelayType));
    }


    public List<EmailRecipientModel<?>> getEscalationRecipients() {
        if(this.data.getEscalationRecipients() == null)
            return null;
    	List<EmailRecipientModel<?>> models = new ArrayList<EmailRecipientModel<?>>();
        for(RecipientListEntryBean b : this.data.getEscalationRecipients()) {
            EmailRecipientModel<?> model = EmailRecipientModel.createModel(b);
            if(model != null)
                models.add(model);
        }
    	return models;
    }

    public void setEscalationRecipients(List<EmailRecipientModel<?>> escalationRecipients) {
        List<RecipientListEntryBean> list = new ArrayList<RecipientListEntryBean>(escalationRecipients.size());
        for(EmailRecipientModel<?> m : escalationRecipients)
        	list.add(EmailRecipientModel.createBean(m));
        this.data.setEscalationRecipients(list);
    }

    public boolean isSendEscalation() {
        return this.data.isSendEscalation();
    }

    public void setSendEscalation(boolean sendEscalation) {
        this.data.setSendEscalation(sendEscalation);
    }
    
    public boolean isRepeatEscalations() {
        return this.data.isRepeatEscalations();
    }
    
    public void setRepeatEscalations(boolean repeatEscalations) {
        this.data.setRepeatEscalations(repeatEscalations);
    }

    public boolean isSendInactive() {
        return this.data.isSendInactive();
    }

    public void setSendInactive(boolean sendInactive) {
        this.data.setSendInactive(sendInactive);
    }

    public boolean isInactiveOverride() {
        return this.data.isInactiveOverride();
    }

    public void setInactiveOverride(boolean inactiveOverride) {
        this.data.setInactiveOverride(inactiveOverride);
    }

    public List<EmailRecipientModel<?>> getInactiveRecipients() {
        if(this.data.getInactiveRecipients() == null)
            return null;
    	List<EmailRecipientModel<?>> models = new ArrayList<EmailRecipientModel<?>>();
        for(RecipientListEntryBean b : this.data.getInactiveRecipients()) {
            EmailRecipientModel<?> model = EmailRecipientModel.createModel(b);
            if(model != null)
                models.add(model);
        }
    	return models;
    }

    public void setInactiveRecipients(List<EmailRecipientModel<?>> inactiveRecipients) {
        List<RecipientListEntryBean> list = new ArrayList<RecipientListEntryBean>(inactiveRecipients.size());
        for(EmailRecipientModel<?> m : inactiveRecipients)
        	list.add(EmailRecipientModel.createBean(m));
        this.data.setInactiveRecipients(list);
    }
    

    public boolean isIncludeSystemInfo(){
    	return this.data.isIncludeSystemInfo();
    }
    public void setIncludeSystemInfo(boolean includeSystemInfo){
    	this.data.setIncludeSystemInfo(includeSystemInfo);
    }
    
    public int getIncludePointValueCount() {
		return this.data.getIncludePointValueCount();
	}

	public void setIncludePointValueCount(int includePointValueCount) {
		this.data.setIncludePointValueCount(includePointValueCount);
	}
	
	public boolean isIncludeLogfile() {
		return this.data.isIncludeLogfile();
	}

	public void setIncludeLogfile(boolean includeLogfile) {
		this.data.setIncludeLogfile(includeLogfile);
	}
	
	public String getCustomTemplate() {
		return this.data.getCustomTemplate();
	}
	
	public void setCustomTemplate(String customTemplate) {
		this.data.setCustomTemplate(customTemplate);
	}
	
	public List<IntStringPair> getAdditionalContext() {
		return this.data.getAdditionalContext();
	}
	
	public void setAdditionalContext(List<IntStringPair> additionalContext) {
		this.data.setAdditionalContext(additionalContext);
	}
	
	public Set<String> getScriptPermissions() {
	    if(this.data.getScriptPermissions() != null)
	        return this.data.getScriptPermissions().getPermissionsSet();
	    else
	        return null;
	}
	
	public void setScriptPermissions(Set<String> scriptPermissions) {
	    if(scriptPermissions != null)
	        this.data.setScriptPermissions(new ScriptPermissions(scriptPermissions));
	}
	
	public String getScript() {
	    return this.data.getScript();
	}
	
	public void setScript(String script) {
	    this.data.setScript(script);
	}
}
