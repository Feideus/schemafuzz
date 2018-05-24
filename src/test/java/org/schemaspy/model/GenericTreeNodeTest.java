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
public class GenericTreeNodeTest {

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



    @Test
    public void WeightPropagationTest() throws AssertionException
    {
        Random rand = new Random();

        GenericTreeNode gtn1 = new GenericTreeNode(null,1,null,null);
        gtn1.setWeight(rand.nextInt(Integer.MAX_VALUE));
        GenericTreeNode gtn2 = new GenericTreeNode(null,2,null,gtn1);
        gtn2.setWeight(rand.nextInt(Integer.MAX_VALUE));
        GenericTreeNode gtn3 = new GenericTreeNode(null,3,null,gtn1);
        gtn3.setWeight(Integer.MAX_VALUE);

        gtn1.addChild(gtn2);
        gtn1.addChild(gtn3);

        gtn2.propagateWeight();
        gtn3.propagateWeight();

        Assert.assertEquals("Testing Weight propagation ",gtn1.getSubTreeWeight(),gtn2.getWeight()+gtn3.getWeight());

        gtn2.setWeight(10);
        gtn3.setWeight(10);

        gtn2.propagateWeight();
        gtn3.propagateWeight();

        gtn1.setSubTreeWeight(0);

        Assert.assertFalse(gtn1.getSubTreeWeight() == gtn2.getWeight()+gtn3.getWeight());

    }

    @Test
    public void SingleChangeBasedOnWeightShouldNotReturnNull() throws AssertionException
    {
        GenericTreeNode gtn1 = new GenericTreeNode(null,1,null,null);
        gtn1.setPotential_changes(new ArrayList<>());

        GenericTreeNode gtn2= new GenericTreeNode(null,2,null,gtn1);
        gtn2.setPotential_changes(new ArrayList<>());
        gtn2.setWeight(10);

        GenericTreeNode gtn3 = new GenericTreeNode(null,3,null,gtn1);
        gtn3.setPotential_changes(new ArrayList<>());
        gtn3.setWeight(10);

        SingleChange sg1 = new SingleChange(null,null,"1","2");
        SingleChange sg2 = new SingleChange(null,null,"1","3");
        SingleChange sg3 = new SingleChange(null,null,"hello","hella");
        SingleChange sg4 = new SingleChange(null,null,"f","t");

        gtn1.getPotential_changes().add(sg1);
        gtn2.getPotential_changes().add(sg2);
        gtn3.getPotential_changes().add(sg3);
        gtn3.getPotential_changes().add(sg4);

        Assert.assertNotNull(gtn1.singleChangeBasedOnWeight());
    }

    @Test
    public void singleChangeAttachedMutatationShouldMatch() // Not very Usefull
    {
        GenericTreeNode gtn1 = new GenericTreeNode(null,1,null,null);
        SingleChange sg1 = new SingleChange(null,null,"1","2");

        gtn1.setChosenChange(sg1);

        Assert.assertEquals("Testing singleChange Attached Mutation consistency",gtn1.getChosenChange().getAttachedToMutation().getId(),gtn1.getId());

    }

//    @Test
//    public void discoverMutationPossibilitiesTest()
//    {
//        HashMap<String,String> mapOfTheRow= new HashMap<String,String>();
//        mapOfTheRow.put("id","1");
//        mapOfTheRow.put("string","Loy");
//        mapOfTheRow.put("bool","f");
//
//        CaseInsensitiveMap<TableColumn> tableColumns= new CaseInsensitiveMap<TableColumn>();
//
//        TableColumn testTableColumn1 = new TableColumn("id","int2");
//        TableColumn testTableColumn2 = new TableColumn("string","varchar");
//        TableColumn testTableColumn3 = new TableColumn("bool","bool");
//
//        tableColumns.put("",testTableColumn1);
//        tableColumns.put("",testTableColumn2);
//        tableColumns.put("",testTableColumn3);
//
//        Table testTable = new Table("test_table",tableColumns);
//        testTable.setColumns(tableColumns);
//
//        Row row = new Row(testTable,mapOfTheRow,3);
//
//        GenericTreeNode gtn1 = new GenericTreeNode(row,1,null,null);
//
//        Assert.assertFalse("No null in a node possibilities",gtn1.getPotential_changes().contains("null"));
//    }

    @Test
    public void NoNullMutationPossibilitiesTest() throws Exception
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

        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM test_table", database, null);
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();

        QueryResponse response = parser.parse(rs,database.getTablesMap().get("test_table"));
        GenericTreeNode tmpMutation = new GenericTreeNode(response.getRows().get(0),1);
        Assert.assertFalse(tmpMutation.discoverMutationPossibilities(tmpMutation).contains("null"));

    }

    @Test
    public void injectAndUndoConsistencyTest() throws Exception
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


        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM test_table", database, null);
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();
        QueryResponse response = parser.parse(rs,database.getTablesMap().get("test_table"));

        Row row = response.getRows().get(0);
        GenericTreeNode tmpMutation = new GenericTreeNode(row,1);
        tmpMutation.setChosenChange(tmpMutation.getPotential_changes().get(0));
        tmpMutation.initPostChangeRow();


        Assert.assertTrue(tmpMutation.inject(sqlService,database,false)); //Test

        rs = stmt.executeQuery();
        response = parser.parse(rs,database.getTablesMap().get("test_table"));

        Assert.assertTrue(response.getRows().get(0).compare(tmpMutation.getPost_change_row()));

        Assert.assertTrue(tmpMutation.undo(sqlService,database)); //Test

        rs = stmt.executeQuery();
        response = parser.parse(rs,database.getTablesMap().get("test_table"));

        Assert.assertTrue(response.getRows().get(0).compare(tmpMutation.getInitial_state_row()));

    }

    @Test
    public void compareTest() throws Exception
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


        PreparedStatement stmt = sqlService.prepareStatement("SELECT * FROM test_table", database, null);
        ResultSet rs = stmt.executeQuery();
        QueryResponseParser parser = new QueryResponseParser();
        QueryResponse response = parser.parse(rs,database.getTablesMap().get("test_table"));

        Row row = response.getRows().get(0);
        Row row2 = row.clone();

        GenericTreeNode tmpMutation = new GenericTreeNode(row,1);
        tmpMutation.setChosenChange(tmpMutation.getPotential_changes().get(0));

        GenericTreeNode tmpMutation2 = new GenericTreeNode(row2,2);
        tmpMutation2.setChosenChange(tmpMutation.getPotential_changes().get(0)); // taking potential change fron mut1 just to be sure


        Assert.assertTrue(tmpMutation.compare(tmpMutation2));

        tmpMutation.getInitial_state_row().getContent().replace("id","-20");

        Assert.assertFalse(tmpMutation.compare(tmpMutation2));

        tmpMutation.setChosenChange(tmpMutation.getPotential_changes().get(1));

        Assert.assertFalse(tmpMutation.compare(tmpMutation2));

    }

}
