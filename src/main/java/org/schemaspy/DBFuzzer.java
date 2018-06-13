
package org.schemaspy;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import org.schemaspy.model.*;
import org.schemaspy.model.Table;
import org.schemaspy.model.GenericTree;
import org.schemaspy.model.GenericTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class DBFuzzer
{

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SchemaAnalyzer analyzer; // Passed from the schemaAnalyser object from the runAnalyzer part. Contains sqlService object used to perform sql Queries

    private GenericTree mutationTree = new GenericTree();

    public DBFuzzer(SchemaAnalyzer analyzer)
    {
      this.analyzer = analyzer;
    }

    public boolean processFirstMutation(GenericTreeNode rootMutation)
    {
        boolean resQuery,returnStatus=true;
        try
        {
            if(rootMutation.getChosenChange() != null)
            {
                resQuery = rootMutation.inject(analyzer.getSqlService(),analyzer.getDb(),false);
                if(resQuery)
                {
                    LOGGER.info("GenericTreeNode was sucessfull");
                }
                else
                    LOGGER.info("QueryError");
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            returnStatus = false;
        }


        //Evaluation
        try
        {
            int mark;
            Process evaluatorProcess = new ProcessBuilder("/bin/bash", "./evaluator.sh").start();
            mark = Integer.parseInt(getEvaluatorResponse(evaluatorProcess));
            rootMutation.setInterest_mark(mark);
            rootMutation.setWeight(mark);
            rootMutation.propagateWeight();
            System.out.println("marking : "+mark);
            System.out.println("Weight : "+rootMutation.getWeight());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            returnStatus = false;
        }

        return returnStatus;
    }

    public boolean fuzz (Config config)
    {
        boolean returnStatus = true;
        boolean resQuery;
        int TreeDepth = 0;
        int maxDepth = Integer.parseInt(analyzer.getCommandLineArguments().getMaxDepth());
        int mark = 0;
        //adding CASCADE to all foreign key tableColumns.
        settingTemporaryCascade(false); // need to drop and recreate database

        LOGGER.info("Starting Database Fuzzing");

        GenericTreeNode currentMutation;
        // Building root Mutation. Could be extended by looking for a relevant first SingleChange as rootMutation
        do {
            Row randomRow = pickRandomRow();
            currentMutation = new GenericTreeNode(randomRow, nextId(),analyzer.getSqlService());
        } while(currentMutation.getPotential_changes().isEmpty());
        currentMutation.setChosenChange(currentMutation.getPotential_changes().get(0));
        currentMutation.initPostChangeRow();
        mutationTree.setRoot(currentMutation);

        if(!processFirstMutation(currentMutation))
            return false;
        /*
        * Main loop. Picks and inject a mutation chosen based on its weight (currently equal to its mark)
        * After injecting and retrieving the marking for the evaluator,
        * undoes necessary mutations from the tree to setup for next mutation
        */
        while(TreeDepth != maxDepth)
        {
          //Choosing next mutation
          currentMutation = chooseNextMutation();
          while(!this.isNewMutation(currentMutation))
          {
            System.out.println("this GenericTreeNode has already been tried ");
            currentMutation = chooseNextMutation();
          }

          System.out.println("chosen mutation "+currentMutation);

            if(!currentMutation.getParent().compare(mutationTree.getLastMutation()))
            {
              try
              {
                mutationTree.getLastMutation().undoToMutation(currentMutation.getParent(),analyzer);

              }
              catch(Exception e)
              {
                e.printStackTrace();
              }
            }
            //Injection
            try
            {
                if(currentMutation.getChosenChange() != null)
                {
                    resQuery = currentMutation.inject(analyzer.getSqlService(),analyzer.getDb(),false);
                    if(resQuery)
                    {
                        LOGGER.info("GenericTreeNode was sucessfull");
                        mutationTree.addToTree(currentMutation);
                    }
                    else
                        LOGGER.info("QueryError");
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
                returnStatus = false;
            }

            //Evalutation
            try
            {
                // the evaluator sets a mark for representing how interesting the mutation was
                Process evaluatorProcess = new ProcessBuilder("/bin/bash", "./aLittleBitLessDumbEvaluator.sh").start();
                mark = Integer.parseInt(getEvaluatorResponse(evaluatorProcess));
                currentMutation.setInterest_mark(mark);
                currentMutation.setWeight(mark);
                currentMutation.propagateWeight(); //update parents weight according to this node new weight
                System.out.println("marking : "+mark);
                System.out.println("Weight : "+currentMutation.getWeight());
            }
            catch(Exception e)
            {
                e.printStackTrace();
                returnStatus = false;
            }
            TreeDepth = mutationTree.checkMaxDepth(mutationTree.getRoot());
      }

      System.out.println("success");
      printMutationTree();
      removeTemporaryCascade();
      return returnStatus;
    }


    //Extract Random row from the db specified in sqlService
    public Row pickRandomRow()
    {
        Row res = null;

      do {
          //Table randomTable = pickRandomTable();

          //String theQuery = "SELECT * FROM " + randomTable.getName() + " ORDER BY RANDOM() LIMIT 1";
          String theQuery = "SELECT * FROM test_table3 ORDER BY RANDOM() LIMIT 1"; // Change test_table2 to test_table here to swap back to line finding
          QueryResponseParser qrp = new QueryResponseParser();
          ResultSet rs = null;
          PreparedStatement stmt;

          try {
              stmt = analyzer.getSqlService().prepareStatement(theQuery);
              rs = stmt.executeQuery();
              res = qrp.parse(rs, analyzer.getDb().getTablesMap().get("test_table3")).getRows().get(0); // randomTable should be in the get()
          } catch (Exception e) {
              LOGGER.info("This query threw an error" + e);
          }
      }
      while(res == null);
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
                    e.printStackTrace();
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
                      e.printStackTrace();
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


    /*
    * Pick a mutation for next iteration of the main loop.
    *
    */
    public GenericTreeNode chooseNextMutation()
    {
        GenericTreeNode nextMut = null;
        GenericTreeNode previousMutation = mutationTree.getLastMutation();
        int markingDiff = previousMutation.getInterest_mark();
        Random rand = new Random();

        if (mutationTree.getNumberOfNodes() > 1) // first mutation doesnt have a predecessor
        {
            markingDiff = previousMutation.getInterest_mark() - mutationTree.find(mutationTree.getLastId()).getInterest_mark();
        }

        if (mutationTree.getRoot() != null)
        {
            if (markingDiff > 0) //
            {
                int randNumber = rand.nextInt(previousMutation.getPotential_changes().size());
                nextMut = new GenericTreeNode(previousMutation.getPost_change_row(), nextId(), mutationTree.getRoot(), previousMutation,false,analyzer.getSqlService());
                nextMut.setChosenChange(previousMutation.getPotential_changes().get(randNumber));
                nextMut.initPostChangeRow();
            }
            else if (markingDiff == 0 || markingDiff < 0)
            {
                Random changeOrDepthen = new Random();

                if(changeOrDepthen.nextInt(2) == 1)
                {
                    SingleChange tmp = mutationTree.getRoot().singleChangeBasedOnWeight();
                    nextMut = new GenericTreeNode(tmp.getAttachedToMutation().getPost_change_row(), nextId(), mutationTree.getRoot(), tmp.getAttachedToMutation(),false,analyzer.getSqlService());
                    nextMut.setChosenChange(tmp);
                    nextMut.initPostChangeRow();
                }
                else
                {
                    Row nextRow;
                    do
                    {
                        nextRow = pickRandomRow();
                        nextMut = new GenericTreeNode(nextRow, nextId(), mutationTree.getRoot(), previousMutation, true, analyzer.getSqlService());
                    }while(nextMut.getPotential_changes().isEmpty());

                    Random nextSingleChangeId = new Random();
                    nextMut.setChosenChange(nextMut.getPotential_changes().get(nextSingleChangeId.nextInt(nextMut.getPotential_changes().size())));
                    nextMut.initPostChangeRow();
                }
            }
            else
                System.out.println("I mean What Da Heck");
        }
        return nextMut;
    }

    public boolean isNewMutation(GenericTreeNode newMut)
    {
        if(mutationTree.getRoot().compare(newMut) || newMut.isSingleChangeOnCurrentPath(mutationTree.getRoot()))
            return false;

        SingleChange chosenChange = newMut.getChosenChange();
        return !chosenChange.compareValues();
    }

    public void printMutationTree()
    {
      String displayer = null ;
      for(int i = 1; i <= mutationTree.getNumberOfNodes();i++)
      {
          for(int j = 0; j < mutationTree.find(i).getDepth();j++)
          {
              displayer = displayer+("--");
          }
          displayer = displayer+(mutationTree.find(i).toString()+"\n");
      }
        System.out.println(displayer);
    }

    public int nextId()
    {
      int res = 0;
      res =  mutationTree.getLastId()+1;
      return res;
    }

    
}
