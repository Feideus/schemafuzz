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
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Override
    public void beforeTestClass(TestContext testContext) {

        try {
            String[] args = {
                    "-t", "pgsql",
                    "-db", "sample_database2",
                    "-hostOptionalPort", "127.0.0.1",
                    "-o", "target/integrationtesting/databaseServiceIT",
                    "-dp", "postgresql-42.2.2.jar",
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
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void WeightPropagationTest() throws AssertionException
    {
        Random rand = new Random();
        String query = "SELECT * FROM customer WHERE store_id=2";
        QueryResponse response = null;

        try {
            PreparedStatement stmt = sqlService.prepareStatement(query, database, "customer");
            ResultSet rs = stmt.executeQuery();
            QueryResponseParser parser = new QueryResponseParser();

             response = parser.parse(rs,database.getTablesMap().get("test_table"));

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        GenericTreeNode tmpMutation1 = new GenericTreeNode(response.getRows().get(0),1,sqlService);
        GenericTreeNode tmpMutation2 = new GenericTreeNode(response.getRows().get(1),1,sqlService);
        GenericTreeNode tmpMutation3 = new GenericTreeNode(response.getRows().get(2),1,sqlService);

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



//    @Ignore
//    @Test
//    public void SingleChangeBasedOnWeightShouldNotReturnNull() throws AssertionException
//    {
//        GenericTreeNode gtn1 = new GenericTreeNode(null,1,null,null,false);
//        gtn1.setPotential_changes(new ArrayList<>());
//
//        GenericTreeNode gtn2= new GenericTreeNode(null,2,null,gtn1,false);
//        gtn2.setPotential_changes(new ArrayList<>());
//        gtn2.setWeight(10);
//
//        GenericTreeNode gtn3 = new GenericTreeNode(null,3,null,gtn1,false);
//        gtn3.setPotential_changes(new ArrayList<>());
//        gtn3.setWeight(10);
//
//        SingleChange sg1 = new SingleChange(null,null,"1","2");
//        SingleChange sg2 = new SingleChange(null,null,"1","3");
//        SingleChange sg3 = new SingleChange(null,null,"hello","hella");
//        SingleChange sg4 = new SingleChange(null,null,"f","t");
//
//        gtn1.getPotential_changes().add(sg1);
//        gtn2.getPotential_changes().add(sg2);
//        gtn3.getPotential_changes().add(sg3);
//        gtn3.getPotential_changes().add(sg4);
//
//        Assert.assertNotNull(gtn1.singleChangeBasedOnWeight());
//    }
//
//    @Ignore
//    @Test
//    public void singleChangeAttachedMutationShouldMatch() // Not very Usefull
//    {
//        GenericTreeNode gtn1 = new GenericTreeNode(null,1,null,null,false);
//        String s1 = "1";
//        Object so1 = s1;
//        String s2 = "2";
//        Object so2 = s2;
//        SingleChange sg1 = new SingleChange(null,null,so1,so2);
//
//        gtn1.setChosenChange(sg1);
//
//        Assert.assertEquals("Testing singleChange Attached Mutation consistency",gtn1.getChosenChange().getAttachedToMutation().getId(),gtn1.getId());
//
//    }
//    @Ignore
//    @Test
//    public void NoNullMutationPossibilitiesTest() throws Exception
//    {
//
//        String[] args = {
//                "-t", "pgsql",
//                "-db","sample_database2",
//                "-hostOptionalPort","127.0.0.1",
//                "-o", "target/integrationtesting/databaseServiceIT",
//                "-dp","postgresql-42.2.2.jar",
//                "-u", "feideus",
//                "-p", "feideus"
//        };
//
//        Config config = new Config(args);
//        DatabaseMetaData databaseMetaData = sqlService.connect(config);
//        String schema = sqlService.getConnection().getSchema();
//        String catalog = sqlService.getConnection().getCatalog();
//        database = new Database(
//                databaseMetaData,
//                "DatabaseServiceIT",
//                catalog,
//                schema,
//                null
//        );
//        databaseService.gatheringSchemaDetails(config, database, progressListener);
//
//        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM test_table", database, null);
//        ResultSet rs = stmt.executeQuery();
//        QueryResponseParser parser = new QueryResponseParser();
//
//        QueryResponse response = parser.parse(rs,database.getTablesMap().get("test_table"));
//        GenericTreeNode tmpMutation = new GenericTreeNode(response.getRows().get(0),1);
//        Assert.assertFalse(tmpMutation.discoverMutationPossibilities(tmpMutation).contains("null"));
//
//    }
//    @Ignore
//    @Test
//    public void injectAndUndoConsistencyTest() throws Exception
//    {
//        String[] args = {
//                "-t", "pgsql",
//                "-db","sample_database2",
//                "-hostOptionalPort","127.0.0.1",
//                "-o", "target/integrationtesting/databaseServiceIT",
//                "-dp","postgresql-42.2.2.jar",
//                "-u", "feideus",
//                "-p", "feideus"
//        };
//
//        Config config = new Config(args);
//        DatabaseMetaData databaseMetaData = sqlService.connect(config);
//        String schema = sqlService.getConnection().getSchema();
//        String catalog = sqlService.getConnection().getCatalog();
//        Database database = new Database(
//                databaseMetaData,
//                "DatabaseServiceIT",
//                catalog,
//                schema,
//                null
//        );
//        databaseService.gatheringSchemaDetails(config, database, progressListener);
//
//
//        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM test_table", database, null);
//        ResultSet rs = stmt.executeQuery();
//        QueryResponseParser parser = new QueryResponseParser();
//        QueryResponse response = parser.parse(rs,database.getTablesMap().get("test_table"));
//
//        Row row = response.getRows().get(0);
//        GenericTreeNode tmpMutation = new GenericTreeNode(row,1);
//        tmpMutation.setChosenChange(tmpMutation.getPotential_changes().get(0));
//        tmpMutation.initPostChangeRow();
//
//
//        Assert.assertTrue(tmpMutation.inject(sqlService,database,false)); //Test
//
//        rs = stmt.executeQuery();
//        response = parser.parse(rs,database.getTablesMap().get("test_table"));
//
//        Assert.assertTrue(response.getRows().get(0).compare(tmpMutation.getPost_change_row()));
//
//        Assert.assertTrue(tmpMutation.undo(sqlService,database)); //Test
//
//        rs = stmt.executeQuery();
//        response = parser.parse(rs,database.getTablesMap().get("test_table"));
//
//        Assert.assertTrue(response.getRows().get(0).compare(tmpMutation.getInitial_state_row()));
//
//    }
//    @Ignore
//    @Test
//    public void compareTest() throws Exception
//    {
//        String[] args = {
//                "-t", "pgsql",
//                "-db","sample_database2",
//                "-hostOptionalPort","127.0.0.1",
//                "-o", "target/integrationtesting/databaseServiceIT",
//                "-dp","postgresql-42.2.2.jar",
//                "-u", "feideus",
//                "-p", "feideus"
//        };
//
//        Config config = new Config(args);
//        DatabaseMetaData databaseMetaData = sqlService.connect(config);
//        String schema = sqlService.getConnection().getSchema();
//        String catalog = sqlService.getConnection().getCatalog();
//        Database database = new Database(
//                databaseMetaData,
//                "DatabaseServiceIT",
//                catalog,
//                schema,
//                null
//        );
//        databaseService.gatheringSchemaDetails(config, database, progressListener);
//
//
//        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM test_table", database, null);
//        ResultSet rs = stmt.executeQuery();
//        QueryResponseParser parser = new QueryResponseParser();
//        QueryResponse response = parser.parse(rs,database.getTablesMap().get("test_table"));
//
//        Row row = response.getRows().get(0);
//        Row row2 = row.clone();
//
//        GenericTreeNode tmpMutation = new GenericTreeNode(row,1);
//        tmpMutation.setChosenChange(tmpMutation.getPotential_changes().get(0));
//
//        GenericTreeNode tmpMutation2 = new GenericTreeNode(row2,2);
//        tmpMutation2.setChosenChange(tmpMutation.getPotential_changes().get(0)); // taking potential change fron mut1 just to be sure
//
//
//        Assert.assertTrue(tmpMutation.compare(tmpMutation2));
//
//        tmpMutation.getInitial_state_row().getContent().replace("id","-20");
//
//        Assert.assertFalse(tmpMutation.compare(tmpMutation2));
//
//        tmpMutation.setChosenChange(tmpMutation.getPotential_changes().get(1));
//
//        Assert.assertFalse(tmpMutation.compare(tmpMutation2));
//
//    }
//    @Ignore
//    @Test
//    public void findPathToMutationTest ()
//    {
//        GenericTreeNode rootMutation = new GenericTreeNode(null,0);
//        rootMutation.setParent(null);
//        rootMutation.setDepth(0);
//        GenericTreeNode tmpMutation = new GenericTreeNode(null,1);
//        tmpMutation.setParent(rootMutation);
//        rootMutation.setDepth(1);
//        GenericTreeNode tmpMutation3 = new GenericTreeNode(null,3);
//        tmpMutation3.setParent(rootMutation);
//        rootMutation.setDepth(1);
//        GenericTreeNode tmpMutation2 = new GenericTreeNode(null,2);
//        tmpMutation2.setParent(tmpMutation);
//        rootMutation.setDepth(2);
//        GenericTreeNode tmpMutation4 = new GenericTreeNode(null,4);
//        tmpMutation4.setParent(tmpMutation3);
//        rootMutation.setDepth(2);
//
//        ArrayList<GenericTreeNode> res1 = new ArrayList<>();
//        res1.add(tmpMutation2);
//        res1.add(tmpMutation);
//
//        ArrayList<GenericTreeNode> res2 = new ArrayList<>();
//        res2.add(tmpMutation3);
//        res2.add(tmpMutation4);
//
//        ArrayList<ArrayList<GenericTreeNode>> finalPath = new ArrayList<>();
//        finalPath.add(res1);
//        finalPath.add(res2);
//
//        Assert.assertTrue(tmpMutation2.findPathToMutation(tmpMutation4).equals(finalPath));
//
//    }
//
//    @Ignore
//    @Test
//    public void isSingleChangeOnPathTest ()
//    {
//        TableColumn tmpTableColumn1 = new TableColumn("test_table_column","bool","test_table");
//        TableColumn tmpTableColumn2 = new TableColumn("test_table_column","bool","test_table");
//
//
//        GenericTreeNode rootMutation = new GenericTreeNode(null,0);
//        rootMutation.setParent(null);
//        rootMutation.setChosenChange(new SingleChange(tmpTableColumn1,null,"1","3"));
//        rootMutation.setDepth(0);
//
//
//        GenericTreeNode tmpMutation = new GenericTreeNode(null,1);
//        tmpMutation.setParent(rootMutation);
//        rootMutation.setDepth(1);
//
//        GenericTreeNode tmpMutation2 = new GenericTreeNode(null,2);
//        tmpMutation2.setChosenChange(new SingleChange(tmpTableColumn2,null,"1","2"));
//        tmpMutation2.setParent(tmpMutation);
//        rootMutation.setDepth(2);
//
//        GenericTreeNode tmpMutationInPath = new GenericTreeNode(null,3);
//        tmpMutationInPath.setParent(tmpMutation2);
//        tmpMutationInPath.setChosenChange(new SingleChange(tmpTableColumn1,null,"1","3"));
//        rootMutation.setDepth(3);
//
//        Assert.assertFalse(tmpMutation2.isSingleChangeOnCurrentPath(rootMutation));
//        Assert.assertTrue(tmpMutationInPath.isSingleChangeOnCurrentPath(rootMutation));
//
//    }

}
