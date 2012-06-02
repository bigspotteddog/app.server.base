package com.maintainer.data.provider.hibernate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.hibernate.cfg.Configuration;
import org.hibernate.service.jdbc.connections.internal.C3P0ConnectionProvider;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import com.maintainer.util.Utils;

public class ExportDatabase {

    private static final Logger LOG = Logger.getLogger(ExportDatabase.class.getName());
    public static final Long VERSION_CANT_CONNECT = null;
    public static final long VERSION_EMPTY = 0;
    public static final long VERSION_1_0_0_0 = parseVersion("1.0.0.0");
    public static final long CURRENT_VERSION = VERSION_1_0_0_0;

    public C3P0ConnectionProvider pool;

    public static void main(String[] args) throws Exception {
        new ExportDatabase().initializeOrUpgrade();
    }

    public ExportDatabase() {}

    public void initializeOrUpgrade() throws Exception {
        // Determine what version is in the database, if there is a database.
        Long previousVersion;
        Long version = determineVersion();
        if (version.equals(VERSION_CANT_CONNECT)) {
            LOG.info("Please verify settings in etc/database.properties and run the script again.");
        } else if (version == VERSION_EMPTY) {
            createDatabase();
        } else {
            LOG.info("Database is at version: " + formatVersion(version));
            previousVersion = version;
            HibernateUtil.beginTransaction();
            try {
                setVersion(version);
                LOG.info("Database is at version: " + formatVersion(version));
                HibernateUtil.commitTransaction(true);
            } catch(Exception e) {
                LOG.severe("Upgrade failed");
                HibernateUtil.rollbackTransaction();
                setVersion(previousVersion);
            }
        }
        if (pool != null) {
            try {
                pool.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void createHelperProcedures(Connection conn) throws SQLException {
        StringBuilder builder = new StringBuilder();
        builder.append("DROP PROCEDURE IF EXISTS AddForeignKeyIfNotExists;\n");
        Statement spStmt = conn.createStatement();
        spStmt.executeUpdate(builder.toString());

        builder = new StringBuilder();
        builder.append("CREATE PROCEDURE AddForeignKeyIfNotExists(IN dbName tinytext, IN tableName tinytext,\n");
        builder.append("IN referencedTable tinytext, IN keyName tinytext, IN keyField tinyText)\n");
        builder.append("BEGIN\n");
        builder.append("DECLARE  constraintName varchar(255);\n");
        builder.append("SET constraintName = 'NA';\n");
        builder.append("SELECT CONSTRAINT_NAME into constraintName\n");
        builder.append("FROM information_schema.REFERENTIAL_CONSTRAINTS\n");
        builder.append("WHERE UNIQUE_CONSTRAINT_SCHEMA = dbName\n");
        builder.append("and TABLE_NAME = tableName\n");
        builder.append("and REFERENCED_TABLE_NAME = referencedTable;\n");
        builder.append("IF (constraintName = 'NA') THEN\n");
		builder.append("set @ddl=CONCAT('ALTER TABLE ',dbName,'.',tableName,\n");
	    builder.append("' ADD FOREIGN KEY (', keyName, ') REFERENCES ', referencedTable, '(', keyField, ')');\n");
		builder.append("prepare stmt from @ddl;\n");
		builder.append("execute stmt;\n");
        builder.append("END IF;\n");
        builder.append("END\n");

        spStmt = conn.createStatement();
        spStmt.executeUpdate(builder.toString());

        builder = new StringBuilder();
        builder.append("DROP PROCEDURE IF EXISTS DropForeignKeyIfExists;\n");
        spStmt = conn.createStatement();
        spStmt.executeUpdate(builder.toString());

        builder = new StringBuilder();
        builder.append("CREATE PROCEDURE DropForeignKeyIfExists(IN dbName tinytext, \n");
        builder.append("IN tableName tinytext,\n");
        builder.append("IN referencedTable tinytext)\n");
        builder.append("BEGIN\n");
        builder.append("DECLARE  keyName varchar(255);\n");
        builder.append("SELECT CONSTRAINT_NAME into keyName\n");
        builder.append("FROM information_schema.REFERENTIAL_CONSTRAINTS\n");
        builder.append("WHERE UNIQUE_CONSTRAINT_SCHEMA = dbName\n");
        builder.append("and TABLE_NAME = tableName\n");
        builder.append("and REFERENCED_TABLE_NAME = referencedTable;\n");
        builder.append("IF EXISTS(SELECT * FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_NAME = keyName) THEN \n");
        builder.append("set @ddl=CONCAT('ALTER TABLE ',dbName,'.',tableName,\n");
        builder.append("' DROP FOREIGN KEY ',keyName); \n");
        builder.append("prepare stmt from @ddl; \n");
        builder.append("execute stmt; \n");
        builder.append("END IF; \n");
        builder.append("END");

        spStmt = conn.createStatement();
        spStmt.executeUpdate(builder.toString());

        builder = new StringBuilder();
        builder.append("drop procedure if exists AddColumnUnlessExists;");
        spStmt = conn.createStatement();
        spStmt.executeUpdate(builder.toString());

        builder = new StringBuilder();
        builder.append("create procedure AddColumnUnlessExists( \n");
        builder.append("IN dbName tinytext, IN tableName tinytext,\n ");
        builder.append("IN fieldName tinytext, IN fieldDef text)\n ");
        builder.append("begin \n");
        builder.append("IF NOT EXISTS ( \n");
        builder.append("SELECT * FROM information_schema.COLUMNS \n");
        builder.append("WHERE column_name=fieldName \n");
        builder.append("and table_name=tableName \n");
        builder.append("and table_schema=dbName) \n");
        builder.append("THEN \n");
        builder.append("set @ddl=CONCAT('ALTER TABLE ',dbName,'.',tableName, \n");
        builder.append("' ADD COLUMN ',fieldName,' ',fieldDef);\n");
        builder.append("prepare stmt from @ddl;\n");
        builder.append("execute stmt;\n");
        builder.append("END IF;\n");
        builder.append("end");

        spStmt = conn.createStatement();
        spStmt.executeUpdate(builder.toString());

        builder = new StringBuilder();
        builder.append("drop procedure if exists DropColumnIfExists; ");
        spStmt = conn.createStatement();
        spStmt.executeUpdate(builder.toString());

        builder = new StringBuilder();
        builder.append("create procedure DropColumnIfExists(IN dbName tinytext, \n");
        builder.append("IN tableName tinytext, IN fieldName tinytext) \n");
        builder.append("begin \n");
        builder.append("IF EXISTS ( \n");
        builder.append("SELECT * FROM information_schema.COLUMNS \n");
        builder.append("WHERE column_name=fieldName \n");
        builder.append("and table_name=tableName \n");
        builder.append("and table_schema=dbName) \n");
        builder.append("THEN \n");
        builder.append("set @ddl=CONCAT('ALTER TABLE ',dbName,'.',tableName, \n");
        builder.append("' DROP COLUMN ',fieldName); \n");
        builder.append("prepare stmt from @ddl; \n");
        builder.append("execute stmt; \n");
        builder.append("END IF; \n");
        builder.append("end ");

        spStmt = conn.createStatement();
        spStmt.executeUpdate(builder.toString());
    }

    private static String formatVersion(long version) {
        String s = Long.toString(version);
        if (s.length() < 10) {
            s = "0" + s;
        }
        if (s.length() != 10) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }
        return Integer.parseInt(s.substring(0, 2)) + "." +
                Integer.parseInt(s.substring(2, 4)) + "." +
                Integer.parseInt(s.substring(4, 6)) + "." +
                s.substring(6);
    }

    @SuppressWarnings("unused")
    private long executeSqlUpgrade(String scriptPath, long newVersion, long previousVersion) throws IOException, SQLException {
        LOG.info("Upgrading to " + formatVersion(newVersion));

        StringBuffer buff = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(scriptPath));
        String line;
        while ((line = reader.readLine()) != null) {
            buff.append(line);
            buff.append("\n");
        }
        String sql = buff.toString();

        try {
            Connection db = pool.getConnection();
            createHelperProcedures(db);
            Statement stmt = db.createStatement();
            for (String statement : sql.split(";")) {
                if (statement != null && statement.trim().length() > 0) {
                    LOG.fine(statement);
                    stmt.execute(statement);
                }
            }
            String versionString = formatVersion(newVersion);
            int count = stmt.executeUpdate ("update ApplicationVersion set Version = '" + versionString + "' where Id = 1");
            if (count == 0) {
                stmt.executeUpdate ("insert into ApplicationVersion (Id, Version) values (1, '" + versionString + "')");
            }
            stmt.close();
            if (!db.getAutoCommit()) {
                db.commit();
            }
            db.close();
        } catch (SQLException e) {
            LOG.severe("UPGRADE SCRIPT FAILED: " + e);
            return previousVersion;
        }
        setVersion(newVersion);
        return newVersion;
    }

    private void setVersion(long newVersion) throws SQLException {
        try {
            Connection db = pool.getConnection();
            Statement stmt = db.createStatement();
            String versionString = formatVersion(newVersion);
            int count = stmt.executeUpdate("update ApplicationVersion set Version = '" + versionString + "' where Id = 1");
            if (count == 0) {
                stmt.executeUpdate("insert into ApplicationVersion (Id, Version) values (1, '" + versionString + "')");
            }
            stmt.close();
            if (!db.getAutoCommit()) {
                db.commit();
            }
            db.close();
        } catch (SQLException e) {
            LOG.severe("UPGRADE SCRIPT FAILED: " + e);
        }
    }

    private void createDatabase() throws Exception {
        // Load the onelink hibernate configuration.
        LOG.info("Exporting database schema.");
        Configuration config = HibernateUtil.createConfiguration();

        // Invoke the hibernate schema export utility.
        SchemaExport export = new SchemaExport(config);

        String exportDatabaseScriptProp = System.getProperty("export.database.script", "false");
        boolean exportDatabaseScript = Boolean.parseBoolean(exportDatabaseScriptProp);

        if (!exportDatabaseScript) {
            export.setFormat(false);
        } else {
            String exportDatabaseFilename = System.getProperty("export.database.filename");
            if (exportDatabaseFilename != null) {
                export.setOutputFile(exportDatabaseFilename);
            }
        }

        String exportDatabaseProp = System.getProperty("export.database", "false");
        boolean exportDatabase = Boolean.parseBoolean(exportDatabaseProp);

        String createDatabaseProp = System.getProperty("create.database", "false");
        boolean createDatabase = Boolean.parseBoolean(createDatabaseProp);

        export.execute(exportDatabaseScript, exportDatabase, false, createDatabase);

        // Load initial data.
        LOG.info("Loading initial data.");
        createDefaultData();
        //setVersion(CURRENT_VERSION);
        LOG.info("Database initialization successful.");
    }

    private void createDefaultData() throws Exception {
        Transaction.execute("system", new Callable<Object>() {
            @Override
            public Object call() throws Exception {
//                new InitialDataLoader().init();
                return null;
            }
        });
    }

    private Long determineVersion() {

        // Return null version for "no database", zero for "empty database", else version number.
        Long version = null;

        // Configuration set?
        Connection db = null;
        while (true) {
            LOG.info("Verifying database parameters.");
            Properties properties = Utils.getDatabaseConfigurationProperties();

            try {
                pool = new C3P0ConnectionProvider();
                pool.configure(properties);
            } catch (Exception e) {
                LOG.severe("Failed to establish database connection pool: " + e);
                break;
            }

            // Is there even a database?
            try {
                db = pool.getConnection();
            } catch (SQLException e) {
                LOG.severe("Failed to connect to database: " + e + "\n\nVerify etc/database.properties (does your database exist?).");
                break;
            }

            // There's a database, does it have Device table?  If not, its an empty database.
            Statement stmt;
            ResultSet rs;
            try {
                stmt = db.createStatement();
                rs = stmt.executeQuery("select count(*) from Device");
                rs.next();
                int count = rs.getInt(1);
                LOG.info("Database contains " + count + " existing devices.");
            } catch (SQLException e) {
                // no device table? empty database
                version = VERSION_EMPTY;
                break;
            }

            // Does it have the ApplicationVersion table? If not, its 3.1.0.2246
            try {
                stmt = db.createStatement();
                rs = stmt.executeQuery("select Version from ApplicationVersion where Id = 1");
                rs.next();
                String s = rs.getString(1).trim();
                version = parseVersion(s);
                break;
            } catch (SQLException e) {
                // no application version table
                version = VERSION_1_0_0_0;
                break;
            }
        }

        if (db != null) {
            try {
                db.close();
            } catch (SQLException e) {
                // ignore
            }
        }

        return version;
    }

    // Major.minor.revision.build
    // -> M,Mmm,rrb,bbb
    private static long parseVersion(String s) {
        String[] parts = s.split("\\.");
        return (long) Integer.parseInt(parts[0]) * 100000000
                + Integer.parseInt(parts[1]) * 1000000
                + Integer.parseInt(parts[2]) * 10000
                + Integer.parseInt(parts[3]);
    }

    public void createApplicationVersion() throws Exception {
    }
}
