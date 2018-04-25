
package org.schemaspy;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import org.schemaspy.model.*;
import org.schemaspy.model.Table;
import org.schemaspy.service.DatabaseService;
import org.schemaspy.service.SqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DBFuzzer
{

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public  SqlService sqlService;

    private  DatabaseService databaseService;

    private SchemaAnalyzer analyzer;

    public DBFuzzer(SchemaAnalyzer analyzer)
    {
      this.sqlService = Objects.requireNonNull(analyzer.getSqlService());
      this.databaseService = Objects.requireNonNull(analyzer.getDatabaseService());
      this.analyzer = analyzer;
    }

    public boolean fuzz (Config config)
    {
        boolean returnStatus = true;

        //adding CASCADE to all foreign key tableColumns.
        settingTemporaryCascade(false); // need to drop and recreate database

        LOGGER.info("Starting Database Fuzzing");
        Row randomRow = pickRandomRow();
        Mutation firstMutation = new Mutation(randomRow);
        firstMutation.initPotential_changes(firstMutation.discoverMutationPossibilities(analyzer.getDb()));
        LOGGER.info(firstMutation.getPotential_changes().toString());
        try
        {
          if(!firstMutation.getPotential_changes().isEmpty())
          {
            firstMutation.setChosenChange(firstMutation.getPotential_changes().get(0));
            firstMutation.inject(firstMutation.getChosenChange(),analyzer,false);
          }

            LOGGER.info("mutation was sucessfull");

            firstMutation.undo(firstMutation.getChosenChange(),analyzer);

            LOGGER.info("backwards mutation was successfull");
        }
        catch(Exception e)
        {
            LOGGER.error(e.toString());
            returnStatus = false;
        }

        removeTemporaryCascade();

      return returnStatus;
    }


    //extract Random row from the db specified in sqlService
    public Row pickRandomRow()
    {

      Table randomTable = pickRandomTable();

      String theQuery = "SELECT * FROM "+randomTable.getName()+" ORDER BY RANDOM() LIMIT 1";
      QueryResponseParser qrp = new QueryResponseParser();
      ResultSet rs = null;
      Row res = null ;
      PreparedStatement stmt;

        try
        {
             stmt = sqlService.prepareStatement(theQuery);
             rs = stmt.executeQuery();
             res = qrp.parse(rs,randomTable).getRows().get(0);
        }
        catch (Exception e)
        {
          LOGGER.info("This query threw an error"+e);
        }

        return res;
    }

    public Table pickRandomTable()
    {
        Random rand = new Random();

        int i = 0, n = rand.nextInt(analyzer.getDb().getTablesMap().entrySet().size());

          for (Map.Entry<String, Table> entry : analyzer.getDb().getTablesMap().entrySet())
          {
            if(n == i)
              return entry.getValue();
            i++;
          }
          throw new RuntimeException("Random table wasn't found"); // should never be reached
    }

    public boolean settingTemporaryCascade(Boolean undo)
    {
      Iterator i;
      ForeignKeyConstraint currentFK;
      String dropSetCascade = null;
      for(Map.Entry<String,Collection<ForeignKeyConstraint>> entry : analyzer.getDb().getLesForeignKeys().entrySet())
      {
              i = entry.getValue().iterator();
              while(i.hasNext())
              {
                currentFK = (ForeignKeyConstraint) i.next();
                dropSetCascade = "ALTER TABLE "+currentFK.getChildTable().getName()+" DROP CONSTRAINT "+currentFK.getName()+ " CASCADE";
                try
                {
                         PreparedStatement stmt = analyzer.getSqlService().prepareStatement(dropSetCascade, analyzer.getDb(),null);
                         stmt.execute();
                }
                catch(Exception e)
                {
                  System.out.println("Dans le catch erreur :"+e);
                }
              }

      }

        for(Map.Entry<String,Collection<ForeignKeyConstraint>> entry : analyzer.getDb().getLesForeignKeys().entrySet())
        {
                i = entry.getValue().iterator();
                while(i.hasNext())
                {
                  currentFK = (ForeignKeyConstraint) i.next();
                  if(!undo)
                    dropSetCascade = "ALTER TABLE "+currentFK.getChildTable().getName()+" ADD CONSTRAINT "+currentFK.getName()+" FOREIGN KEY ("+currentFK.getParentColumns().get(0).getName()+" ) REFERENCES "+currentFK.getParentTable().getName()+"("+currentFK.getChildColumns().get(0).getName()+") ON UPDATE CASCADE";
                  else
                  {
                    dropSetCascade = "ALTER TABLE "+currentFK.getChildTable().getName()+" ADD CONSTRAINT "+currentFK.getName()+" FOREIGN KEY ("+currentFK.getParentColumns().get(0).getName()+" ) REFERENCES "+currentFK.getParentTable().getName()+"("+currentFK.getChildColumns().get(0).getName()+")";
                  }
                  try
                  {
                           PreparedStatement stmt = analyzer.getSqlService().prepareStatement(dropSetCascade, analyzer.getDb(),null);
                           stmt.execute();
                  }
                  catch(Exception e)
                  {
                    System.out.println("Dans le catch 2 erreur :"+e);
                  }
                }

          }

      if(!undo)
        LOGGER.info("temporary set all fk constraints to cascade");
      else
        LOGGER.info("set all the constraints back to original");

      return true;
    }

    public boolean removeTemporaryCascade()
    {
      return settingTemporaryCascade(true);
    }
}
