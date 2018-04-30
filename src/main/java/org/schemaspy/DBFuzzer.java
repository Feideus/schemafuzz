
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
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DBFuzzer
{

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public  SqlService sqlService;

    private  DatabaseService databaseService;

    private SchemaAnalyzer analyzer;

    private ArrayList<Mutation> mutationTree;

    public DBFuzzer(SchemaAnalyzer analyzer)
    {
      this.mutationTree = new ArrayList<Mutation>();
      this.sqlService = Objects.requireNonNull(analyzer.getSqlService());
      this.databaseService = Objects.requireNonNull(analyzer.getDatabaseService());
      this.analyzer = analyzer;
    }

    public boolean fuzz (Config config)
    {
        boolean returnStatus = true;
        int mark = 0;
        //adding CASCADE to all foreign key tableColumns.
        settingTemporaryCascade(false); // need to drop and recreate database

        LOGGER.info("Starting Database Fuzzing");

        Row randomRow = pickRandomRow();
        Mutation currentMutation = new Mutation(randomRow);

        //while(evaluation != -1)
        //{


          currentMutation.initPotential_changes(currentMutation.discoverMutationPossibilities(analyzer.getDb()));
          LOGGER.info(currentMutation.getPotential_changes().toString());
          try
          {
            if(!currentMutation.getPotential_changes().isEmpty())
            {
              currentMutation.setChosenChange(currentMutation.getPotential_changes().get(0));
              currentMutation.inject(analyzer,false);
              LOGGER.info("mutation was sucessfull");
              mutationTree.add(currentMutation);
              System.out.println(mutationTree);
              currentMutation.undo(analyzer);
              LOGGER.info("backwards mutation was successfull");
            }

          }
          catch(Exception e)
          {
              LOGGER.error(e.toString());
              returnStatus = false;
          }

          try
          {
            Process evaluatorProcess = new ProcessBuilder("/bin/bash", "./evaluator.sh").start();
            mark = Integer.parseInt(getEvaluatorResponse(evaluatorProcess));
            currentMutation.setInterest_mark(mark);
            System.out.println(currentMutation.getInterest_mark());

            currentMutation = chooseNextMutation();
            System.out.println(currentMutation.toString());
          }
          catch(Exception e)
          {
            System.out.println("error while recovering marking"+e);
          }
      //}

      removeTemporaryCascade();
      return returnStatus;
    }


    //extract Random row from the db specified in sqlService
    public Row pickRandomRow()
    {
      Table randomTable = pickRandomTable();


      //String theQuery = "SELECT * FROM "+randomTable.getName()+" ORDER BY RANDOM() LIMIT 1";
      String theQuery = "SELECT * FROM test_table WHERE (id=1) ORDER BY RANDOM() LIMIT 1";
      QueryResponseParser qrp = new QueryResponseParser();
      ResultSet rs = null;
      Row res = null ;
      PreparedStatement stmt;

        try
        {
             stmt = sqlService.prepareStatement(theQuery);
             rs = stmt.executeQuery();
             res = qrp.parse(rs,analyzer.getDb().getTablesMap().get("test_table")).getRows().get(0); //randomTable should be set there
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


    public String getEvaluatorResponse(Process p)
    {
        String response = "";
        try
        {

          BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
          String line;
          while ((line = r.readLine())!=null) {
            response = response+line;
          }
          r.close();
        }
        catch(Exception e)
        {
          System.out.println("error while reading process output"+e);
        }
        return response;
    }

    public Mutation chooseNextMutation()
    {
      Mutation nextMut = null;
      Mutation lastMutation = mutationTree.get(mutationTree.size()-1);
      int markingDiff = 0;
      Random rand = new Random();

      if(mutationTree.size() > 1)
        markingDiff = mutationTree.get(lastMutation.getId()).getInterest_mark()-mutationTree.get(lastMutation.getId()-2).getInterest_mark();


      if(!mutationTree.isEmpty())
      {
        if(markingDiff < 0)
        {
          System.out.println("should not happen right now");
        }
        else if(markingDiff > 0)
        {

            lastMutation.initPotential_changes(lastMutation.discoverMutationPossibilities(analyzer.getDb()));
            int randNumber = rand.nextInt(lastMutation.getPotential_changes().size());
            nextMut = new Mutation(lastMutation.getPost_change_row());
            nextMut.setChosenChange(lastMutation.getPotential_changes().get(randNumber));
        }
        else if(markingDiff == 0)
        {
            int randNumber = rand.nextInt(mutationTree.size());
            int randMutation = rand.nextInt(mutationTree.get(randNumber).getPotential_changes().size());
            nextMut = new Mutation(mutationTree.get(randNumber).getPost_change_row());
            nextMut.setChosenChange(mutationTree.get(randNumber).getPotential_changes().get(randMutation));
        }
        else
        {
            System.out.println("I mean What Da Heck");
        }

      }

      return nextMut;
    }
}
