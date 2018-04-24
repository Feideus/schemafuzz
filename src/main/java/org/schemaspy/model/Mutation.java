
package org.schemaspy.model;

import org.schemaspy.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.PreparedStatement;


public class Mutation
{

  private static final AtomicInteger count = new AtomicInteger(0);
  private Integer id;
  private Integer interest_mark;
  private Row initial_state_row;
  private ArrayList<SingleChange> potential_changes = new ArrayList<SingleChange>();
  private ArrayList<SingleChange> cascadeFK = new ArrayList<SingleChange>(); // a integrer
  private SingleChange chosenChange;
  private Mutation child;
  private Mutation parent;
  private boolean cascadingFK;
	/**
	* Default Mutation constructor
	*/
	public Mutation(Row initial_state_row) {
		this.id = count.incrementAndGet();
		this.initial_state_row = initial_state_row;
    this.cascadingFK = false;
	}

	public Integer getId() {
		return id;
	}

  public SingleChange getChosenChange() {
    return chosenChange;
  }


	public Row getInitial_state_row() {
		return initial_state_row;
	}

	/**
	* Returns value of potential_changes
	* @return
	*/
	public ArrayList<SingleChange> getPotential_changes() {
		return potential_changes;
	}

	/**
	* Sets new value of potential_changes
	* @param
	*/
	public void initPotential_changes(ArrayList<SingleChange> potential_changes) {
		this.potential_changes = potential_changes;
	}

	/**
	* Returns value of child
	* @return
	*/
	public Mutation getChild() {
		return child;
	}

public void setChosenChange(SingleChange sc)
{
  this.chosenChange = sc;
}

	/**
	* Sets new value of child
	* @param
	*/
	public void setChild(Mutation child) {
		this.child = child;
	}

	/**
	* Returns value of parent
	* @return
	*/
	public Mutation getParent() {
		return parent;
	}

	/**
	* Sets new value of parent
	* @param
	*/
	public void setParent(Mutation parent) {
		this.parent = parent;
	}

  public ArrayList<SingleChange> discoverMutationPossibilities(Database db)
  {

    int i;
    ArrayList<SingleChange> possibilities = new ArrayList<SingleChange>();


    //TRYING TO DISCOVER RAW POSSIBILITIES
    for(Map.Entry<String,String> content : initial_state_row.getContent().entrySet())
    {
            try
            {
              TableColumn parentColumn = initial_state_row.getParentTable().findTableColumn(content.getKey());
              possibilities.addAll(discoverFieldPossibilities(parentColumn,content.getValue()));
            }
            catch(Exception e)
            {
            }
    }

    //REMOVING POSSIBILITIES THAT DONT MATCH CONSTRAINTS
    for(i = 0; i < possibilities.size(); i++)
    {
      if(possibilities.get(i).respectsConstraints())
        possibilities.remove(possibilities.get(i));
    }

    return possibilities;
  }

  public ArrayList<SingleChange> discoverFieldPossibilities (TableColumn tableColumn, String column_value) throws Exception
  {
      ArrayList<SingleChange> oneChange = new ArrayList<SingleChange>();
      String typeName = tableColumn.getTypeName();
      switch (typeName) {
            case "serial":
                          System.out.println("serial");
                          oneChange.add(new SingleChange(tableColumn,this,column_value,Integer.toString(Integer.parseInt(column_value)+1)));
                          oneChange.add(new SingleChange(tableColumn,this,column_value,Integer.toString(32767)));
                          oneChange.add(new SingleChange(tableColumn,this,column_value,Integer.toString(1)));
                     break;
            case "varchar":
                          System.out.println("varchar");
                          oneChange.add(new SingleChange(tableColumn,this,column_value,"a"));
                          char tmp = column_value.charAt(0);
                          tmp++;
                          oneChange.add(new SingleChange(tableColumn,this,column_value,(Character.toString(tmp)+column_value.substring(1))));
                     break;
            /*case "varchar":
                          oneChange.add(new SingleChange(this,column_value,Integer.toString(Integer.parseInt(column_value)+1)));
                          oneChange.add(new SingleChange(this,column_value,Integer.toString(2147483647)));
                          oneChange.add(new SingleChange(this,column_value,Integer.toString(1)));
                     break;
            case 4:  typeName = "April";//OTHER TYPES COMMING IN LATER
                     break;
            case 5:  typeName = "May";
                     break;
            case 6:  typeName = "June";
                     break;
            case 7:  typeName = "July";
                     break;
            case 8:  typeName = "August";
                     break;
            case 9:  typeName = "September";
                     break;
            case 10: typeName = "October";
                     break;
            case 11: typeName = "November";
                     break;
            case 12: typeName = "December";
                     break;*/
            default: throw new Exception("No raw mutation possibilities could be found");
        }


      return oneChange;
  }

  public boolean inject(SingleChange chosenChange,SchemaAnalyzer analyzer, boolean undo) throws Exception
  {
    int i;

    //trying to iumplement cascade on fk
    /*if(cascadeFK.isEmpty())
    {
      System.out.println("Checking FKs");
      cascadeFK = checkCascadeFK(chosenChange,analyzer);
    }

    if(!cascadingFK)
    {
      cascadingFK = true;
      if(!cascadeFK.isEmpty())
      {

        for( i = 0; i < cascadeFK.size();injecti++)
        {
          System.out.println("cascading found FKs");
          inject(cascadeFK.get(i),analyzer,false);
        }
          System.out.println("Done cascading found FKs");
      }
      cascadingFK = false;
    }*/

    String theQuery = updateQueryBuilder (undo);
    try
    {
             PreparedStatement stmt = analyzer.getSqlService().prepareStatement(theQuery, analyzer.getDb(),null);
             stmt.execute();
             return true;
    }
    catch(Exception e)
    {
      throw new Exception(e);
    }
  }

  public boolean undo(SingleChange chosenChange, SchemaAnalyzer analyzer)
  {
    try
    {
      return this.inject(chosenChange, analyzer, true);
    }
    catch(Exception e)
    {
      System.out.println("Undo failed with error :"+e);
      return false;
    }
  }

  public String updateQueryBuilder(boolean undo)
  {
    String theQuery ;

    if(undo)
    {
      if(chosenChange.getParentTableColumn().getTypeName().equals("varchar"))
        theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+"='"+chosenChange.getNewValue()+"', ";
      else
        theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+" = "+chosenChange.getNewValue()+", ";
    }
    else
    {
      if(chosenChange.getParentTableColumn().getTypeName().equals("varchar"))
        theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+"='"+chosenChange.getOldValue()+"', ";
      else
        theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+"="+chosenChange.getOldValue()+", ";
    }
    for(Map.Entry<String,String> entry : initial_state_row.getContent().entrySet())
    {
      if(!entry.getKey().equals(chosenChange.getParentTableColumn().getName()))
      {
        if(chosenChange.getParentTableColumn().getTypeName().equals("varchar"))
          theQuery = theQuery+(entry.getKey()+" = '"+entry.getValue()+"', ");
        else
          theQuery = theQuery+(entry.getKey()+" = "+entry.getValue()+", ");
      }

    }

    theQuery = theQuery.substring(0,theQuery.lastIndexOf(","));
    theQuery = theQuery+" WHERE ";

    // USING ALL VALUES TO TRIANGULATE THE ROW TO UPDATE (no primary key)
    if(initial_state_row.getParentTable().getPrimaryColumns().isEmpty())
    {
      for(Map.Entry<String,String> entry : initial_state_row.getContent().entrySet())
      {
          if(chosenChange.getParentTableColumn().getTypeName().equals("varchar"))
            theQuery = theQuery+(entry.getKey()+"='"+entry.getValue()+"' AND ");
          else
            theQuery = theQuery+(entry.getKey()+"="+entry.getValue()+" AND ");
      }
      theQuery = theQuery.substring(0,theQuery.lastIndexOf(" AND "));
    }
    else
      theQuery = theQuery+(" "+initial_state_row.getParentTable().getPrimaryColumns().get(0).getName()+"="+initial_state_row.getValueOfColumn(initial_state_row.getParentTable().getPrimaryColumns().get(0).getName()));


    System.out.println("build query ! "+theQuery);
    return theQuery;
  }


  //NOT FUNCTIONNAL
  public ArrayList<SingleChange> checkCascadeFK(SingleChange chosenChange, SchemaAnalyzer analyzer)
  {
    ArrayList<SingleChange> res = new ArrayList<SingleChange>();
    int i,j;

    for(Map.Entry<String,Collection<ForeignKeyConstraint>> entry : analyzer.getDb().getLesForeignKeys().entrySet())
    {
      Iterator<ForeignKeyConstraint> iter = entry.getValue().iterator();
      while (iter.hasNext()) {
        ForeignKeyConstraint elem = iter.next();
        for(i = 0; i < elem.getParentColumns().size();i++)
        {
              if(elem.getParentColumns().get(i).getName().equals(chosenChange.getParentTableColumn().getName()))
                res.add(new SingleChange(elem.getParentColumns().get(i),this,chosenChange.getOldValue(),chosenChange.getNewValue()));
              if(elem.getChildColumns().get(i).getName().equals(chosenChange.getParentTableColumn().getName()))
                res.add(new SingleChange(elem.getChildColumns().get(i),this,chosenChange.getOldValue(),chosenChange.getNewValue()));
        }
      }
    }

    for( i = 0; i < res.size();i++)
    {
      for( j = 0; j < res.size();j++)
      {
        if(res.get(i).equals(res.get(j)))
          res.remove(res.get(j));
      }
    }

    System.out.println("LA PRESENCE DE FOREIGN KEY EST"+ res);
    return res;
  }

}
