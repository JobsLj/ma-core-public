/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.SystemActionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SqlBackupActionPermissionDefinition;
import com.serotonin.m2m2.rt.maint.work.DatabaseBackupWorkItem;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.timer.OneTimeTrigger;

/**
 * 
 * @author Terry Packer
 */
public class SqlBackupActionDefinition extends SystemActionDefinition{

	private final String KEY = "sqlBackup";
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#getKey()
	 */
	@Override
	public String getKey() {
		return KEY;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#getWorkItem(com.fasterxml.jackson.databind.JsonNode)
	 */
	@Override
	public SystemActionTask getTaskImpl(final JsonNode input) {
		return new Action();
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#getPermissionTypeName()
	 */
	@Override
	protected String getPermissionTypeName() {
		return SqlBackupActionPermissionDefinition.PERMISSION;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#validate(com.fasterxml.jackson.databind.JsonNode)
	 */
	@Override
	protected void validate(JsonNode input) throws ValidationException {

	}
	
	/**
	 * Class to allow purging data in ordered tasks with a queue 
	 * of up to 5 waiting purges
	 * 
	 * @author Terry Packer
	 */
	class Action extends SystemActionTask{
		
		private final DatabaseBackupWorkItem item;
		private volatile boolean cancelled;
		
		public Action(){
			super(new OneTimeTrigger(0l), "SQL Database Backup Action", "DATABASE_BACKUP_ACTION", 5);
			this.item = new DatabaseBackupWorkItem();
		}

		/* (non-Javadoc)
		 * @see com.serotonin.timer.Task#run(long)
		 */
		@Override
		public void runImpl(long runtime) {
        	String backupLocation = SystemSettingsDao.instance.getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION);
			item.setBackupLocation(backupLocation);
			Common.backgroundProcessing.addWorkItem(item);
			
			//As a medium priority task we can just wait here until we are done
			while(true){
				if(item.isFinished())
					break;
				else
					try {
						Thread.sleep(800);
					} catch (InterruptedException e) { 	}
			}
			
			//Did it work?
			if(item.isFailed()){
				this.results.put("failed", item.isFailed());
			}else{
				//Add the filename of the backup
				this.results.put("backupFile", item.getFilename());
			}
		}
		
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.util.timeout.SystemActionTask#cancel()
		 */
		@Override
		public boolean cancel() {
			boolean result = super.cancel();
			this.cancelled = true;
			this.item.cancel();
			return result;
		}
		
		/* (non-Javadoc)
		 * @see com.serotonin.timer.Task#isCancelled()
		 */
		@Override
		public boolean isCancelled() {
			return this.cancelled;
		}
		
	}
}
