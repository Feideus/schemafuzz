
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

    private SchemaAnalyzer analyzer;

    private DatabaseService databaseService;

    private Mutation rootMutation;
    private int nbMutation;

    public DBFuzzer(SchemaAnalyzer analyzer)
    {
      this.sqlService = Objects.requireNonNull(analyzer.getSqlService());
      this.databaseService = Objects.requireNonNull(analyzer.getDatabaseService());
      this.analyzer = analyzer;
      this.nbMutation = 0;
    }

    public boolean fuzz (Config config)
    {
        boolean returnStatus = true;
        int mark = 0;
        //adding CASCADE to all foreign key tableColumns.
        settingTemporaryCascade(false); // need to drop and recreate database

        LOGGER.info("Starting Database Fuzzing");

        Row randomRow = pickRandomRow();
        Mutation currentMutation = new Mutation(randomRow,nextId());
        currentMutation.initPotential_changes(currentMutation.discoverMutationPossibilities(analyzer.getDb()));
        currentMutation.setChosenChange(currentMutation.getPotential_changes().get(0));
        rootMutation = currentMutation;
        boolean resQuery = false;

        while(mark != -1)
        {
          try
          {
            if(currentMutation.getChosenChange() != null)
            {
              resQuery = currentMutation.inject(analyzer,false);
              if(resQuery)
              {
                nbMutation = getLastId(rootMutation);
                LOGGER.info("mutation was sucessfull");
              }
              else
                LOGGER.info("QueryError");

              //currentMutation.undo(analyzer);
              //LOGGER.info("backwards mutation was successfull");
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
          }
          catch(Exception e)
          {
            returnStatus = false;
            System.out.println("error while recovering marking"+e);
          }

          currentMutation = chooseNextMutation();
          while(!this.isNewMutation(currentMutation))
          {
            System.out.println("this Mutation has already been tried ");
            currentMutation = chooseNextMutation();
          }
          System.out.println(currentMutation.toString());
      }
      printMutationTree(rootMutation);
      removeTemporaryCascade();
      return returnStatus;
    }


    //extract Random row from the db specified in sqlService
    public Row pickRandomRow()
    {
      Table randomTable = pickRandomTable();


      //String theQuery = "SELECT * FROM "+randomTable.getName()+" ORDER BY RANDOM() LIMIT 1";
      String theQuery = "SELECT * FROM test_table ORDER BY RANDOM() LIMIT 1";
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
      Mutation previousMutation = getLastMutation(rootMutation);
      int markingDiff = previousMutation.getInterest_mark();
      Random rand = new Random();

      if(nbMutation > 1)
      {
        markingDiff = previousMutation.getInterest_mark()-getMutation(rootMutation,getLastId(rootMutation)-1).getInterest_mark();
      }

      if(rootMutation != null)
      {
        if(markingDiff > 0)
        {
            previousMutation.initPotential_changes(previousMutation.discoverMutationPossibilities(analyzer.getDb()));
            int randNumber = rand.nextInt(previousMutation.getPotential_changes().size());
            nextMut = new Mutation(previousMutation.getPost_change_row(),nextId());
            nextMut.setChosenChange(previousMutation.getPotential_changes().get(randNumber));

            previousMutation.addChild(nextMut);
            nextMut.setParent(previousMutation);
        }
        else if(markingDiff == 0 || markingDiff < 0)
        {
            int randNumber = rand.nextInt(nbMutation);
            while(getMutation(rootMutation,randNumber).getPotential_changes().size() == 0)
            {
              randNumber = rand.nextInt(nbMutation);
            }
            int randMutation = rand.nextInt(getMutation(rootMutation,randNumber).getPotential_changes().size());
            nextMut = new Mutation(getMutation(rootMutation,randNumber).getPost_change_row(),nextId());
            nextMut.initPotential_changes(nextMut.discoverMutationPossibilities(analyzer.getDb()));
            nextMut.setChosenChange(getMutation(rootMutation,randNumber).getPotential_changes().get(randMutation));
        }
        else
        {
            System.out.println("I mean What Da Heck");
        }

      }

      return nextMut;
    }

    public boolean isNewMutation(Mutation newMut)
    {
      boolean res = true;
      for(int i = 0; i < nbMutation; i++)
      {
        if(getMutation(rootMutation,i).compare(newMut))
          res = false;
      }

      return res;
    }

    public int getLastId(Mutation rootMutation)
    {
      if(rootMutation == null)
        return 1;

      int maxId = rootMutation.getId();
      for(int i = 0; i < rootMutation.getChilds().size();i++)
      {
        if(rootMutation.getChilds().size()!= 0 && maxId < getLastId(rootMutation.getChilds().get(i)))
          maxId = getLastId(rootMutation.getChilds().get(i));
      }
      return maxId;
    }

    public Mutation getLastMutation(Mutation mutation)
    {
      Mutation res = mutation;
      for(int i = 0; i < res.getChilds().size();i++)
      {
        if(getLastMutation(res.getChilds().get(i)).getId() == getLastId(rootMutation))
          res = res.getChilds().get(i);
      }
      return res;
    }

    public Mutation getMutation(Mutation mutation ,int id)
    {

        Mutation res = mutation;

          for(int i = 0; i < res.getChilds().size();i++)
          {
            if(getMutation(res.getChilds().get(i), res.getChilds().get(i).getId()).getId() == id)
              res = res.getChilds().get(i);
          }
          return res;
    }

    public void printMutationTree(Mutation rootMutation)
    {
      String res = "";

      for(int i = 0; i < rootMutation.getChilds().size();i++)
      {
        res = res+" [ ID : "+rootMutation.getId()+" OV : "+rootMutation.getChosenChange().getOldValue()+" NV : "+rootMutation.getChosenChange().getNewValue()+" ]\n";
      }

      System.out.println(res);
    }

    public int nextId()
    {
      return getLastId(rootMutation)+1;
    }
}
