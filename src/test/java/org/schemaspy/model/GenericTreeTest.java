package org.schemaspy.model;

import nl.jqno.equalsverifier.internal.exceptions.AssertionException;
import org.junit.*;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.schemaspy.Config;
import org.schemaspy.cli.CommandLineArguments;
import org.schemaspy.service.DatabaseService;
import org.schemaspy.service.SqlService;
import org.schemaspy.util.CaseInsensitiveMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


@RunWith(SpringRunner.class)
@SpringBootTest
public class GenericTreeTest {

    @Autowired
    private SqlService sqlService;
    @Autowired
    private DatabaseService databaseService;
    private Database database;
    @Mock
    private ProgressListener progressListener;
    @MockBean
    private CommandLineArguments arguments;
    @MockBean
    private CommandLineRunner commandLineRunner;

    @Ignore
    @Test
    public void checkMaxDepthTest() throws Exception
    {

        String[] args = {
                "-t", "pgsql",
                "-db","sample_database2",
                "-hostOptionalPort","127.0.0.1",
                "-o", "target/integrationtesting/databaseServiceIT",
                "-dp","postgresql-42.2.2.jar",
                "-u", "feideus",
                "-p", "feideus"
        };

        Config config = new Config(args);
        DatabaseMetaData databaseMetaData = sqlService.connect(config);
        String schema = sqlService.getConnection().getSchema();
        String catalog = sqlService.getConnection().getCatalog();
        Database database = new Database(
                databaseMetaData,
                "DatabaseServiceIT",
                catalog,
                schema,
                null
        );
        databaseService.gatheringSchemaDetails(config, database, progressListener);


        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM actual_test_table", database, null);
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();
        QueryResponse response = parser.parse(rs,database.getTablesMap().get("actual_test_table"));

        Row row = response.getRows().get(0);
        Row row2 = row.clone();
        Row row3 = row.clone();
        Row row4 = row.clone();
        Row row5 = row.clone();
        Row row6 = row.clone();

        GenericTree tree = new GenericTree();

        GenericTreeNode tmpMutation = new GenericTreeNode(row,1,sqlService);
        tmpMutation.setChosenChange(tmpMutation.getPotential_changes().get(0));
        tmpMutation.setDepth(0);

        GenericTreeNode tmpMutation2 = new GenericTreeNode(row2,2,sqlService);
        tmpMutation2.setChosenChange(tmpMutation.getPotential_changes().get(0));
        tmpMutation2.setDepth(1);

        GenericTreeNode tmpMutation3 = new GenericTreeNode(row3,3,sqlService);
        tmpMutation3.setChosenChange(tmpMutation.getPotential_changes().get(0));
        tmpMutation3.setDepth(2);

        GenericTreeNode tmpMutation4 = new GenericTreeNode(row4,4,sqlService);
        tmpMutation4.setChosenChange(tmpMutation.getPotential_changes().get(0));
        tmpMutation4.setDepth(1);

        GenericTreeNode tmpMutation5 = new GenericTreeNode(row5,5,sqlService);
        tmpMutation4.setChosenChange(tmpMutation.getPotential_changes().get(0));
        tmpMutation4.setDepth(3);

        GenericTreeNode tmpMutation6 = new GenericTreeNode(row6,6,sqlService);
        tmpMutation6.setChosenChange(tmpMutation.getPotential_changes().get(0));
        tmpMutation6.setDepth(4);


        tmpMutation.addChild(tmpMutation2);
        tmpMutation.addChild(tmpMutation4);
        tmpMutation2.addChild(tmpMutation3);
        tmpMutation3.addChild(tmpMutation4);
        tmpMutation4.addChild(tmpMutation5);
        tmpMutation5.addChild(tmpMutation6);

        tree.setRoot(tmpMutation);

        Assert.assertEquals(tree.checkMaxDepth(tree.getRoot()) , 4);

    }

}

