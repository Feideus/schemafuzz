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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class GenericTreeNodeTest extends AbstractTestExecutionListener {

    @Autowired
    private SqlService sqlService;
    @Autowired
    private DatabaseService databaseService;
    @Mock
    private ProgressListener progressListener;
    @MockBean
    private CommandLineArguments arguments;
    @MockBean
    private CommandLineRunner commandLineRunner;

    private Database database;

    private boolean setupDone;

    @Before public void setupOnce() throws Exception
    {
        if(!setupDone)
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
            database = new Database(
                    databaseMetaData,
                    "DatabaseServiceIT",
                    catalog,
                    schema,
                    null
            );
            databaseService.gatheringSchemaDetails(config, database, progressListener);
            database.initMeta(config,database);
            setupDone = true;
        }
    }

    @Test
    public void WeightPropagationTest() throws Exception
    {
        System.out.println("DBBBBB = "+database);
        System.out.println(sqlService);
        Random rand = new Random();
        String query = "SELECT * FROM actual_test_table";
        QueryResponse response = null;

        PreparedStatement stmt = sqlService.prepareStatement(query, database, "actual_test_table");
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();
        response = parser.parse(rs,database.getTablesMap().get("actual_test_table"));

        GenericTreeNode tmpMutation1 = new GenericTreeNode(response.getRows().get(0),1,sqlService);
        GenericTreeNode tmpMutation2 = new GenericTreeNode(response.getRows().get(1),2,tmpMutation1,tmpMutation1,true,sqlService);
        GenericTreeNode tmpMutation3 = new GenericTreeNode(response.getRows().get(2),3,tmpMutation1,tmpMutation1,true,sqlService);

        tmpMutation1.setWeight(rand.nextInt(Integer.MAX_VALUE));
        tmpMutation2.setWeight(rand.nextInt(Integer.MAX_VALUE));
        tmpMutation3.setWeight(Integer.MAX_VALUE);

        tmpMutation1.addChild(tmpMutation2);
        tmpMutation1.addChild(tmpMutation3);

        tmpMutation2.propagateWeight();
        tmpMutation3.propagateWeight();

        Assert.assertEquals("Testing Weight propagation ",tmpMutation1.getSubTreeWeight(),tmpMutation2.getWeight()+tmpMutation3.getWeight());

        tmpMutation2.setWeight(10);
        tmpMutation3.setWeight(10);

        tmpMutation2.propagateWeight();
        tmpMutation3.propagateWeight();

        tmpMutation1.setSubTreeWeight(0);

        Assert.assertFalse(tmpMutation1.getSubTreeWeight() == tmpMutation2.getWeight()+tmpMutation3.getWeight());

    }

    @Test
    public void SingleChangeBasedOnWeightShouldNotReturnNull() throws Exception
    {
        Random rand = new Random();
        String query = "SELECT * FROM actual_test_table";
        QueryResponse response = null;

        PreparedStatement stmt = sqlService.prepareStatement(query, database, "actual_test_table");
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();
        response = parser.parse(rs,database.getTablesMap().get("actual_test_table"));

        GenericTreeNode tmpMutation1 = new GenericTreeNode(response.getRows().get(0),1,sqlService);
        GenericTreeNode tmpMutation2 = new GenericTreeNode(response.getRows().get(1),2,tmpMutation1,tmpMutation1,true,sqlService);
        GenericTreeNode tmpMutation3 = new GenericTreeNode(response.getRows().get(2),3,tmpMutation1,tmpMutation1,true,sqlService);

        tmpMutation1.addChild(tmpMutation2);
        tmpMutation1.addChild(tmpMutation3);


        Assert.assertNotNull(tmpMutation1.singleChangeBasedOnWeight());
        Assert.assertNotNull(tmpMutation2.singleChangeBasedOnWeight());
        Assert.assertNotNull(tmpMutation3.singleChangeBasedOnWeight());
    }

    @Test
    public void singleChangeAttachedMutationShouldMatch() throws Exception// Not very Usefull
    {
        Random rand = new Random();
        String query = "SELECT * FROM actual_test_table";
        QueryResponse response = null;

        PreparedStatement stmt = sqlService.prepareStatement(query, database, "actual_test_table");
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();
        response = parser.parse(rs,database.getTablesMap().get("actual_test_table"));

        GenericTreeNode tmpMutation1 = new GenericTreeNode(response.getRows().get(0),1,sqlService);
        tmpMutation1.setChosenChange(tmpMutation1.getPotential_changes().get(0));

        Assert.assertEquals("Testing singleChange Attached Mutation consistency",tmpMutation1.getChosenChange().getAttachedToMutation().getId(),tmpMutation1.getId());

    }

    @Test
    public void NoNullMutationPossibilitiesTest() throws Exception
    {


        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM actual_test_table", database, null);
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();

        QueryResponse response = parser.parse(rs,database.getTablesMap().get("actual_test_table"));
        GenericTreeNode tmpMutation1 = new GenericTreeNode(response.getRows().get(0),1,sqlService);
        Assert.assertFalse(tmpMutation1.getPotential_changes().contains("null"));

    }

    @Test
    public void injectAndUndoConsistencyTest() throws Exception
    {

        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM actual_test_table WHERE address_id=0", database, null);
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();
        QueryResponse response = parser.parse(rs,database.getTablesMap().get("actual_test_table"));

        GenericTreeNode tmpMutation1 = new GenericTreeNode(response.getRows().get(0),1,sqlService);

        GenericTree mutationTree = new GenericTree();

        tmpMutation1.setChosenChange(tmpMutation1.getPotential_changes().get(0));
        tmpMutation1.initPostChangeRow();
        mutationTree.setRoot(tmpMutation1);

        Assert.assertTrue(tmpMutation1.inject(sqlService,database,mutationTree,false) > 0 ); //Test

        rs = stmt.executeQuery();
        response = parser.parse(rs,database.getTablesMap().get("actual_test_table"));

        System.out.println(response.getRows().get(0));
        System.out.println(tmpMutation1.getPost_change_row());
        Assert.assertTrue(response.getRows().get(0).compare(tmpMutation1.getPost_change_row()));

        Assert.assertTrue(tmpMutation1.undo(sqlService,database,mutationTree) > 0); //Test

        rs = stmt.executeQuery();
        response = parser.parse(rs,database.getTablesMap().get("actual_test_table"));

        Assert.assertTrue(response.getRows().get(0).compare(tmpMutation1.getInitial_state_row()));

    }

    @Test
    public void compareTest() throws Exception
    {

        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM actual_test_table", database, null);
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();
        QueryResponse response = parser.parse(rs,database.getTablesMap().get("actual_test_table"));

        Row row = response.getRows().get(0);
        Row row2 = row.myClone();

        GenericTreeNode tmpMutation1 = new GenericTreeNode(response.getRows().get(0),1,sqlService);
        tmpMutation1.setChosenChange(tmpMutation1.getPotential_changes().get(0));

        GenericTreeNode tmpMutation2 = new GenericTreeNode(response.getRows().get(1),2,tmpMutation1,tmpMutation1,true,sqlService);

        tmpMutation2.setChosenChange(tmpMutation2.getPotential_changes().get(0));

        Assert.assertFalse(tmpMutation1.compare(tmpMutation2));

        tmpMutation1.setChosenChange(tmpMutation1.getPotential_changes().get(1));

        Assert.assertFalse(tmpMutation1.compare(tmpMutation2));

    }

}
