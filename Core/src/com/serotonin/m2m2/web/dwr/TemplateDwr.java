/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DuplicateKeyException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateDefinition;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateVO;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * DWR To Facilitate Template Interface
 * 
 * @author Terry Packer
 *
 */
public class TemplateDwr extends BaseDwr{

	 private final static Log LOG = LogFactory.getLog(DataPointDwr.class);
	 private TemplateDao dao;
	 
	 public TemplateDwr(){
		 this.dao = TemplateDao.getInstance();
	 }
	 
	/**
	 * Save a template
	 * @return
	 */
    @DwrPermission(user = true)
    public ProcessResult getDataPointTemplates(int dataTypeId) {
    	ProcessResult result = new ProcessResult();
    	List<DataPointPropertiesTemplateVO> templates = dao.getDataPointTemplatesByDataTypeId(dataTypeId);
    	result.addData(TemplateDao.getInstance().tableName, templates);
    	
    	return result;
    }
	
	/**
	 * Save a template
	 * @return
	 */
    @DwrPermission(user = true)
    public ProcessResult getDataPointTemplate(int id) {
    	ProcessResult result = new ProcessResult();
    	result.addData("template",dao.get(id));
    	return result;
    }
    
    /**
     * Get a new Data Point Template
     * @return
     */
    @DwrPermission(custom = SystemSettingsDao.PERMISSION_DATASOURCE)
    public ProcessResult getNewDataPointTemplate(){
    	ProcessResult response = new ProcessResult();
    	DataPointPropertiesTemplateVO vo = new DataPointPropertiesTemplateVO();
    	vo.setDefinition(new DataPointPropertiesTemplateDefinition());
    	response.addData("vo", vo);
    	return response;
    }
    
    @DwrPermission(custom = SystemSettingsDao.PERMISSION_DATASOURCE)
    public ProcessResult findPointsWithTemplate(DataPointPropertiesTemplateVO vo){
    	ProcessResult response = new ProcessResult();
    	List<DataPointVO> dataPoints = DataPointDao.getInstance().getByTemplate(vo.getId(), false);
    	List<DataPointSummary> summaries = new ArrayList<DataPointSummary>(dataPoints.size());
    	for(DataPointVO dp: dataPoints)
    		summaries.add(new DataPointSummary(dp));
    	response.addData(DataPointDao.getInstance().tableName, summaries);
    	return response;
    }
    
	/**
	 * Save a Data Point template
	 * @return
	 */
    @DwrPermission(custom = SystemSettingsDao.PERMISSION_DATASOURCE)
    public ProcessResult saveDataPointTemplate(DataPointPropertiesTemplateVO vo) {
    	
        ProcessResult response = new ProcessResult();

        if (vo.getXid() == null) {
            vo.setXid(dao.generateUniqueXid());
        }
        //We are using XID as only viewable indicator of the template, but 
        // we need a name for validation so set XID and name to the same thing.
        if(StringUtils.isEmpty(vo.getName())){
        	vo.setName(vo.getXid());
        }
        vo.validate(response);

        if (!response.getHasMessages()) {
            try {
                dao.save(vo);
                updateDataPointsUsingTemplate(vo, response);
            }
            catch (Exception e) {
                // Handle the exceptions.
                LOG.error(e);

                String context = vo.getName();
                if (context == null) {
                    context = vo.getXid();
                }
                if (context == null) {
                    context = vo.getXid();
                }
                if (context == null) {
                    context = Integer.toString(vo.getId());
                }

                if (e instanceof DuplicateKeyException)
                    response.addContextualMessage(context, "table.edit.alreadyExists");
                else
                    response.addContextualMessage(context, "table.edit.unableToSave", e.getMessage());
            }
        }
        response.addData("vo", vo);
        response.addData("id", vo.getId()); //Add in case it fails
        return response;
    }
	
    /**
     * Update all data points tied to this template.
     * 
     * PRE-The Template must have been validated
     * 
     * @param vo
     * @param response
     */
	protected void updateDataPointsUsingTemplate(DataPointPropertiesTemplateVO vo, ProcessResult response){
		//Update all data points and let the user know which ones were updated
		List<DataPointVO> templatedPoints = DataPointDao.getInstance().getByTemplate(vo.getId());
		List<String> xidsUpdated = new ArrayList<String>();
		Map<String,String> failedXidMap = new HashMap<String,String>(); //Map of XID to why
		for(DataPointVO templatedPoint : templatedPoints){
			try{
				vo.updateDataPointVO(templatedPoint);
				Common.runtimeManager.saveDataPoint(templatedPoint);
				xidsUpdated.add(templatedPoint.getXid());
			}catch(Exception e){
				LOG.error(e.getMessage(), e);
				failedXidMap.put(templatedPoint.getXid(), e.getMessage());
				response.addMessage(new TranslatableMessage("common.default", e.getMessage()));
			}
		}
		
		response.addData("updatedXids", xidsUpdated);
		response.addData("failedXids", failedXidMap);
	}
}
