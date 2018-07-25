/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigDao;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.SchemaDefinition;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.module.EmportDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * Common repo for export data
 *
 * @author Terry Packer
 */
public class ConfigurationExportData {
    
    //When adding to this list make sure you update getAllExportNames();
    public static final String DATA_SOURCES = SchemaDefinition.DATASOURCES_TABLE;
    public static final String DATA_POINTS = SchemaDefinition.DATAPOINTS_TABLE;
    public static final String EVENT_HANDLERS = SchemaDefinition.EVENT_HANDLER_TABLE;
    public static final String EVENT_DETECTORS = SchemaDefinition.EVENT_DETECTOR_TABLE;
    public static final String JSON_DATA = SchemaDefinition.JSON_DATA_TABLE;
    public static final String MAILING_LISTS = SchemaDefinition.MAILING_LISTS_TABLE;
    public static final String POINT_HIERARCHY = "pointHierarchy";
    public static final String PUBLISHERS = SchemaDefinition.PUBLISHERS_TABLE;
    public static final String SYSTEM_SETTINGS = SchemaDefinition.SYSTEM_SETTINGS_TABLE;
    public static final String TEMPLATES = SchemaDefinition.TEMPLATES_TABLE;
    public static final String USERS = SchemaDefinition.USERS_TABLE;
    public static final String VIRTUAL_SERIAL_PORTS = "virtualSerialPorts";

    /**
     * Get a list of all available export elements
     * @return
     */
    public static String[] getAllExportNames() {
        List<String> names = new ArrayList<>();
        names.add(DATA_SOURCES);
        names.add(DATA_POINTS);
        names.add(EVENT_HANDLERS);
        //TODO reinstate event detectors once there is a non-data-point event detector
        //names.add(EVENT_DETECTORS);
        names.add(JSON_DATA);
        names.add(MAILING_LISTS);
        names.add(PUBLISHERS);
        names.add(POINT_HIERARCHY);
        names.add(SYSTEM_SETTINGS);
        names.add(TEMPLATES);
        names.add(USERS);
        names.add(VIRTUAL_SERIAL_PORTS);
        
        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class))
            if(def.getInView())
                names.add(def.getElementId());
        
        return names.toArray(new String[names.size()]);
    }
    
    /**
     * Get a list of pairs of i18n property and export names for all export items
     * @return
     */
    public static List<StringStringPair> getAllExportDescriptions(){
        List<StringStringPair> elements = new ArrayList<StringStringPair>();
        elements.add(new StringStringPair("header.dataSources", DATA_SOURCES));
        elements.add(new StringStringPair("header.dataPoints", DATA_POINTS));
        elements.add(new StringStringPair("header.eventHandlers", EVENT_HANDLERS));
        //TODO reinstate event detectors once there is a non-data-point event detector
        //elements.add(new StringStringPair("header.eventDetectors", EVENT_DETECTORS));
        elements.add(new StringStringPair("header.jsonData", JSON_DATA));
        elements.add(new StringStringPair("header.mailingLists", MAILING_LISTS));
        elements.add(new StringStringPair("header.publishers", PUBLISHERS));
        elements.add(new StringStringPair("header.pointHierarchy", POINT_HIERARCHY));
        elements.add(new StringStringPair("header.systemSettings", SYSTEM_SETTINGS));
        elements.add(new StringStringPair("header.pointPropertyTemplates", TEMPLATES));
        elements.add(new StringStringPair("header.users", USERS));
        elements.add(new StringStringPair("header.virtualSerialPorts", VIRTUAL_SERIAL_PORTS));

        
        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class)) {
            if(def.getInView())
                elements.add(new StringStringPair(def.getDescriptionKey(), def.getElementId()));
        }
        
        return elements;
    }
    
    /**
     * Get a map of desired export data.
     * 
     * @param exportElements if null full export is returned
     * @return
     */
    public static Map<String, Object> createExportDataMap(String[] exportElements){
        if(exportElements == null)
            exportElements = getAllExportNames();
        
        Map<String, Object> data = new LinkedHashMap<>();
        
        if (ArrayUtils.contains(exportElements, DATA_SOURCES))
            data.put(DATA_SOURCES, DataSourceDao.instance.getDataSources());
        if (ArrayUtils.contains(exportElements, DATA_POINTS))
            data.put(DATA_POINTS, DataPointDao.instance.getDataPoints(null, true));
        if (ArrayUtils.contains(exportElements, USERS))
            data.put(USERS, UserDao.instance.getUsers());
        if (ArrayUtils.contains(exportElements, MAILING_LISTS))
            data.put(MAILING_LISTS, MailingListDao.instance.getMailingLists());
        if (ArrayUtils.contains(exportElements, PUBLISHERS))
            data.put(PUBLISHERS, PublisherDao.instance.getPublishers());
        if (ArrayUtils.contains(exportElements, EVENT_HANDLERS))
            data.put(EVENT_HANDLERS, EventHandlerDao.instance.getEventHandlers());
        if (ArrayUtils.contains(exportElements, POINT_HIERARCHY))
            data.put(POINT_HIERARCHY, DataPointDao.instance.getPointHierarchy(true).getRoot().getSubfolders());
        if (ArrayUtils.contains(exportElements, SYSTEM_SETTINGS))
            data.put(SYSTEM_SETTINGS, SystemSettingsDao.instance.getAllSystemSettingsAsCodes());
        if (ArrayUtils.contains(exportElements, TEMPLATES))
            data.put(TEMPLATES, TemplateDao.instance.getAll());
        if (ArrayUtils.contains(exportElements, VIRTUAL_SERIAL_PORTS))
            data.put(VIRTUAL_SERIAL_PORTS, VirtualSerialPortConfigDao.instance.getAll());
        if (ArrayUtils.contains(exportElements, JSON_DATA))
            data.put(JSON_DATA, JsonDataDao.instance.getAll());
        
        //TODO Add EVENT_DETECTORS
        //TODO Write the ImportTask properly for EventDetectors...
        
        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class)) {
            if (ArrayUtils.contains(exportElements, def.getElementId()))
                data.put(def.getElementId(), def.getExportData());
        }
        
        return data;
    }
    
}
