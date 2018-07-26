/*
 * Copyright (C) 2004-2011 John Currier
 * Copyright (C) 2017 Nils Petzaell
 *
 * This file is a part of the SchemaSpy project (http://schemaspy.org).
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.schemaspy;


import org.schemaspy.cli.CommandLineArguments;
import org.schemaspy.model.*;
import org.schemaspy.model.xml.SchemaMeta;
import org.schemaspy.service.DatabaseService;
import org.schemaspy.service.SqlService;
import org.schemaspy.util.ConnectionURLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author John Currier
 * @author Nils Petzaell
 */
public class SchemaAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SqlService sqlService;

    private final DatabaseService databaseService;

    private Database db;



    private final CommandLineArguments commandLineArguments;

    public SchemaAnalyzer(SqlService sqlService, DatabaseService databaseService, CommandLineArguments commandLineArguments) {
        this.sqlService = Objects.requireNonNull(sqlService);
        this.databaseService = Objects.requireNonNull(databaseService);
        this.commandLineArguments = Objects.requireNonNull(commandLineArguments);
    }

    public Database analyze(Config config) throws SQLException, IOException {
        // don't render console-based detail unless we're generating HTML (those probably don't have a user watching)
        // and not already logging fine details (to keep from obfuscating those)

        ProgressListener progressListener = new ConsoleProgressListener(false,commandLineArguments);
        // if -all(evaluteAll) or -schemas given then analyzeMultipleSchemas
        List<String> schemas = config.getSchemas();


        if (schemas != null || config.isEvaluateAllEnabled()) {
            return this.analyzeMultipleSchemas(config, progressListener);
        } else {
            String schema = commandLineArguments.getSchema();
            return analyze(schema, config, progressListener);
        }
    }

    public Database analyzeMultipleSchemas(Config config, ProgressListener progressListener) throws SQLException, IOException {
        try {
            // following params will be replaced by something appropriate
            List<String> args = config.asList();
            args.remove("-schemas");
            args.remove("-schemata");

            List<String> schemas = config.getSchemas();
            String schemaSpec = config.getSchemaSpec();
            Connection connection = this.getConnection(config);
            DatabaseMetaData meta = connection.getMetaData();
            //-all(evaluteAll) given then get list of the database schemas
            if (schemas == null || config.isEvaluateAllEnabled()) {
                if (schemaSpec == null)
                    schemaSpec = ".*";
                LOGGER.info(
                        "Analyzing schemas that match regular expression '{}'. " +
                        "(use -schemaSpec on command line or in .properties to exclude other schemas)",
                        schemaSpec);
                schemas = DbAnalyzer.getPopulatedSchemas(meta, schemaSpec, false);
                if (schemas.isEmpty())
                    schemas = DbAnalyzer.getPopulatedSchemas(meta, schemaSpec, true);
                if (schemas.isEmpty())
                    schemas.add(config.getUser());
            }

            LOGGER.info("Analyzing schemas: " + System.lineSeparator() + "{}",
                    schemas.stream().collect(Collectors.joining(System.lineSeparator())));

            String dbName = config.getDb();
            // set flag which later on used for generation rootPathtoHome link.
            config.setOneOfMultipleSchemas(true);
            for (String schema : schemas) {
                // reset -all(evaluteAll) and -schemas parameter to avoid infinite loop! now we are analyzing single schema
                config.setSchemas(null);
                config.setEvaluateAllEnabled(false);
                if (dbName == null)
                    config.setDb(schema);
                else
                    config.setSchema(schema);

                LOGGER.info("Analyzing {}", schema);
                db = this.analyze(schema, config, progressListener);
                if (db == null) //if any of analysed schema returns null
                    return null;
            }
            return db;
        } catch (Config.MissingRequiredParameterException missingParam) {
            config.dumpUsage(missingParam.getMessage(), missingParam.isDbTypeSpecific());
            return null;
        }
    }

    public Database analyze(String schema, Config config, ProgressListener progressListener) throws SQLException, IOException {
        try {
            LOGGER.info("Starting schema analysis");

            String dbName = config.getDb();

            String catalog = commandLineArguments.getCatalog();

            DatabaseMetaData meta = sqlService.connect(config);

            LOGGER.debug("supportsSchemasInTableDefinitions: {}", meta.supportsSchemasInTableDefinitions());
            LOGGER.debug("supportsCatalogsInTableDefinitions: {}", meta.supportsCatalogsInTableDefinitions());

            // set default Catalog and Schema of the connection
            if (schema == null)
                schema = meta.getConnection().getSchema();
            if (catalog == null)
                catalog = meta.getConnection().getCatalog();

            SchemaMeta schemaMeta = config.getMeta() == null ? null : new SchemaMeta(config.getMeta(), dbName, schema);
            if (config.isHtmlGenerationEnabled()) {
                LOGGER.info("Connected to {} - {}", meta.getDatabaseProductName(), meta.getDatabaseProductVersion());

                if (schemaMeta != null && schemaMeta.getFile() != null) {
                    LOGGER.info("Using additional metadata from {}", schemaMeta.getFile());
                }
            }

            //
            // create our representation of the database
            //
            db = new Database(meta, dbName, catalog, schema, schemaMeta);

            databaseService.gatheringSchemaDetails(config, db, progressListener);

            long duration = progressListener.startedGraphingSummaries();

            Collection<Table> tables = new ArrayList<>(db.getTables());
            tables.addAll(db.getViews());

            if (tables.isEmpty()) {
                dumpNoTablesMessage(schema, config.getUser(), meta, config.getTableInclusions() != null);
                if (!config.isOneOfMultipleSchemas()) // don't bail if we're doing the whole enchilada
                    throw new EmptySchemaException();
            }

            duration = progressListener.finishedGatheringDetails();
            long overallDuration = progressListener.finished(tables, config);

            if (config.isHtmlGenerationEnabled())
                LOGGER.info("Wrote relationship details of {} tables/views in {} seconds.", tables.size(), overallDuration / 1000);


            db.initMeta(config,db);

            ///-------------- TEST ZONE
            LOGGER.info("Done initializing Meta");

            //System.out.println("lesColumns"+db.getLesColumns().toString());
            //System.out.println("lesForeignKeys = "+db.getLesForeignKeys().toString()+"\n");
            //System.out.println("lesCheckConstraints= "+db.getLesCheckConstraints().toString()+"\n");


            /// ----------- END OF TEST ZONE

            return db;
        } catch (Config.MissingRequiredParameterException missingParam) {
            config.dumpUsage(missingParam.getMessage(), missingParam.isDbTypeSpecific());
            return null;
        }
    }

    public CommandLineArguments getCommandLineArguments() {
        return commandLineArguments;
    }

    private Connection getConnection(Config config) throws IOException {

        Properties properties = config.getDbProperties();

        ConnectionURLBuilder urlBuilder = new ConnectionURLBuilder(config, properties);
        if (config.getDb() == null)
            config.setDb(urlBuilder.build());

        String driverClass = properties.getProperty("driver");
        String driverPath = properties.getProperty("driverPath");
        if (Objects.isNull(driverPath))
            driverPath = "";

        if (Objects.nonNull(config.getDriverPath()))
            driverPath = config.getDriverPath();

        DbDriverLoader driverLoader = new DbDriverLoader();
        return driverLoader.getConnection(config, urlBuilder.build(), driverClass, driverPath);
    }

    /**
     * dumpNoDataMessage
     *
     * @param schema String
     * @param user   String
     * @param meta   DatabaseMetaData
     */
    private static void dumpNoTablesMessage(String schema, String user, DatabaseMetaData meta, boolean specifiedInclusions) throws SQLException {
        LOGGER.warn("No tables or views were found in schema '{}'.", schema);
        List<String> schemas;
        try {
            schemas = DbAnalyzer.getSchemas(meta);
        } catch (SQLException | RuntimeException exc) {
            LOGGER.error("The user you specified '{}' might not have rights to read the database metadata.", user, exc);
            return;
        }

        if (Objects.isNull(schemas)) {
            LOGGER.error("Failed to retrieve any schemas");
            return;
        } else if (schemas.contains(schema)) {
            LOGGER.error(
                    "The schema exists in the database, but the user you specified '{}'" +
                    "might not have rights to read its contents.",
                    user);
            if (specifiedInclusions) {
                LOGGER.error(
                        "Another possibility is that the regular expression that you specified " +
                        "for what to include (via -i) didn't match any tables.");
            }
        } else {
            LOGGER.error(
                    "The schema '{}' could not be read/found, schema is specified using the -s option." +
                    "Make sure user '{}' has the correct privileges to read the schema." +
                    "Also not that schema names are usually case sensitive.",
                    schema, user);
            LOGGER.info(
                    "Available schemas(Some of these may be user or system schemas):" +
                    System.lineSeparator() + "{}",
                    schemas.stream().collect(Collectors.joining(System.lineSeparator())));
            List<String> populatedSchemas = DbAnalyzer.getPopulatedSchemas(meta);
            if (populatedSchemas.isEmpty()) {
                LOGGER.error("Unable to determine if any of the schemas contain tables/views");
            } else {
                LOGGER.info("Schemas with tables/views visible to '{}':" + System.lineSeparator() + "{}",
                        populatedSchemas.stream().collect(Collectors.joining(System.lineSeparator())));
            }
        }
    }



	/**
	* Returns value of LOGGER
	* @return
	*/
	public static Logger getLOGGER() {
		return LOGGER;
	}

	/**
	* Returns value of sqlService
	* @return
	*/
	public SqlService getSqlService() {
		return sqlService;
	}

	/**
	* Returns value of databaseService
	* @return
	*/
	public DatabaseService getDatabaseService() {
		return databaseService;
	}

	/**
	* Returns value of db
	* @return
	*/
	public Database getDb() {
		return db;
	}

}
