/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.infiniteautomation.mango.db.query.SQLConstants;
import com.serotonin.db.DaoUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.TranslatableMessageParseException;

public class BaseDao extends DaoUtils implements SQLConstants{

    /**
     * Public constructor for code that needs to get stuff from the database.
     */
    public BaseDao() {
        super(Common.databaseProxy.getDataSource(), Common.databaseProxy.getTransactionManager());
    }

    //
    // Convenience methods for storage of booleans.
    //
    public static String boolToChar(boolean b) {
        return b ? Y : N;
    }

    public static boolean charToBool(String s) {
        return Y.equals(s);
    }

    //
    // XID convenience methods
    //
    protected String generateUniqueXid(String prefix, String tableName) {
        return Common.generateXid(prefix);
    }

    protected boolean isXidUnique(String xid, int excludeId, String tableName) {
        return ejt.queryForInt("select count(*) from " + tableName + " where xid=? and id<>?", new Object[] { xid,
                excludeId }, 0) == 0;
    }

    //
    // Convenience methods for translatable messages
    //
    public static String writeTranslatableMessage(TranslatableMessage tm) {
        if (tm == null)
            return null;
        return tm.serialize();
    }

    public static TranslatableMessage readTranslatableMessage(ResultSet rs, int columnIndex) throws SQLException {
        String s = rs.getString(columnIndex);
        if (s == null)
            return null;

        try {
            return TranslatableMessage.deserialize(s);
        }
        catch (TranslatableMessageParseException e) {
            return new TranslatableMessage("common.default", rs.getString(columnIndex));
        }
    }
}
