
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
  private SingleChange chosenChange;
  private Mutation child;
  private Mutation parent;

	/**
	* Default Mutation constructor
	*/
	public Mutation(Row initial_state_row) {
		this.id = count.incrementAndGet();
		this.initial_state_row = initial_state_row;

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

  public boolean inject(SingleChange chosenChange,SchemaAnalyzer analyzer) throws Exception
  {

    this.chosenChange = chosenChange;

    String theQuery = updateQueryBuilder();
    try
    {
             PreparedStatement stmt = analyzer.getSqlService().prepareStatement(theQuery, analyzer.getDb(),null);
             stmt.execute();
             System.out.println("Mutation succesfull !!");
             return true;
    }
    catch(Exception e)
    {
      throw new Exception(e);
    }
  }

  public String updateQueryBuilder()
  {
    String theQuery ;

    if(chosenChange.getParentTableColumn().getTypeName().equals("varchar"))
      theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+"='"+chosenChange.getNewValue()+"', ";
    else
      theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+" = "+chosenChange.getNewValue()+", ";

    for(Map.Entry<String,String> entry : initial_state_row.getContent().entrySet())
    {
      if(!entry.getKey().equals(chosenChange.getParentTableColumn().getName()))
      {
        if(chosenChange.getParentTableColumn().getTypeName().equals("varchar"))
          theQuery = theQuery+(entry.getKey()+"='"+entry.getValue()+"', ");
        else
          theQuery = theQuery+(entry.getKey()+"="+entry.getValue()+", ");
      }

    }

    theQuery = theQuery.substring(0,theQuery.lastIndexOf(","));
    theQuery = theQuery+"WHERE ";
    // PLACE CODE HERE TO HANDLE NO PRIMARY KEY CASE
    theQuery = theQuery+(" "+initial_state_row.getParentTable().getPrimaryColumns().get(0).getName()+"="+initial_state_row.getValueOfColumn(initial_state_row.getParentTable().getPrimaryColumns().get(0).getName()));

    System.out.println("build query ! "+theQuery);
    return theQuery;
  }

}
