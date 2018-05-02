
package org.schemaspy.model;

import org.schemaspy.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.PreparedStatement;


public class Mutation
{

  private Integer id;
  private Integer interest_mark;
  private Row initial_state_row;
  private Row post_change_row;
  private ArrayList<SingleChange> potential_changes = new ArrayList<SingleChange>();
  private ArrayList<SingleChange> cascadeFK = new ArrayList<SingleChange>(); // a integrer
  private SingleChange chosenChange;
  private ArrayList<Mutation> childs = new ArrayList<Mutation>();
  private Mutation parent;
  private boolean cascadingFK;
	/**
	* Default Mutation constructor
	*/
	public Mutation(Row initial_state_row,int id) {
		this.id = id;
		this.initial_state_row = initial_state_row;
    this.cascadingFK = false;
	}

	public Integer getId() {
		return id;
	}

  public Row getPost_change_row()
  {
    return this.post_change_row;
  }

  public void setPost_change_row(Row postChangeRow)
  {
    this.post_change_row = postChangeRow;
  }

  public SingleChange getChosenChange() {
    return chosenChange;
  }


	public Row getInitial_state_row() {
		return initial_state_row;
	}

  public int getInterest_mark()
  {
      return this.interest_mark;
  }

  public void setInterest_mark(int mark)
  {
    this.interest_mark = mark;
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
	* Returns value of childs
	* @return
	*/
	public ArrayList<Mutation> getChilds() {
		return childs;
	}

  public void setChosenChange(SingleChange sc)
  {
    this.chosenChange = sc;
  }

  public void addChild(Mutation childMut)
  {
      this.childs.add(childMut);
  }


	/**
	* Sets new value of childs
	* @param
	*/
	public void setChilds(ArrayList<Mutation> childs) {
		this.childs = childs;
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
      if(!possibilities.get(i).respectsConstraints())
        possibilities.remove(possibilities.get(i));
    }
    return possibilities;
  }

  public ArrayList<SingleChange> discoverFieldPossibilities (TableColumn tableColumn, String column_value) throws Exception
  {
      System.out.println(tableColumn.getTypeName());
      ArrayList<SingleChange> oneChange = new ArrayList<SingleChange>();
      String typeName = tableColumn.getTypeName();
      switch (typeName) {
            case "int2":
                          oneChange.add(new SingleChange(tableColumn,this,column_value,Integer.toString(Integer.parseInt(column_value)+1)));
                          oneChange.add(new SingleChange(tableColumn,this,column_value,Integer.toString(32767)));
                          oneChange.add(new SingleChange(tableColumn,this,column_value,Integer.toString(1)));
                     break;
            case "varchar":
                          char tmp = column_value.charAt(0);
                          oneChange.add(new SingleChange(tableColumn,this,column_value,(Character.toString(tmp++)+column_value.substring(1))));
                          oneChange.add(new SingleChange(tableColumn,this,column_value,(Character.toString(tmp--)+column_value.substring(1))));

                     break;
            case "bool":
                          if(column_value.equals("f"))
                            oneChange.add(new SingleChange(tableColumn,this,column_value,"t"));
                          if(column_value.equals("t"))
                            oneChange.add(new SingleChange(tableColumn,this,column_value,"f"));
                     break;
            /*  case 5:  typeName = "May";
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

  public boolean inject(SchemaAnalyzer analyzer, boolean undo) throws Exception
  {

    String theQuery = updateQueryBuilder(undo);
    try
    {
             PreparedStatement stmt = analyzer.getSqlService().prepareStatement(theQuery, analyzer.getDb(),null);
             stmt.execute();
             this.post_change_row = this.initial_state_row;
             this.post_change_row.setValueOfColumn(chosenChange.getParentTableColumn().getName(), chosenChange.getNewValue());
             return true;
    }
    catch(Exception e)
    {
      throw new Exception(e);
    }
  }

  public boolean undo(SchemaAnalyzer analyzer)
  {
    try
    {
      return this.inject(analyzer, true);
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
      if(chosenChange.getParentTableColumn().getTypeName().equals("varchar") || chosenChange.getParentTableColumn().getTypeName().equals("bool"))
        theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+"='"+chosenChange.getOldValue()+"', ";
      else
        theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+" = "+chosenChange.getOldValue()+", ";
    }
    else
    {
      if(chosenChange.getParentTableColumn().getTypeName().equals("varchar") || chosenChange.getParentTableColumn().getTypeName().equals("bool"))
        theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+"='"+chosenChange.getNewValue()+"', ";
      else
        theQuery = "UPDATE "+initial_state_row.getParentTable().getName()+" SET "+chosenChange.getParentTableColumn().getName()+"="+chosenChange.getNewValue()+", ";
    }
    for(Map.Entry<String,String> entry : initial_state_row.getContent().entrySet())
    {
      if(!entry.getKey().equals(chosenChange.getParentTableColumn().getName()))
      {
        if(chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("varchar") || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("bool"))
          theQuery = theQuery+(entry.getKey()+"='"+entry.getValue()+"', ");
        else
          theQuery = theQuery+(entry.getKey()+"="+entry.getValue()+", ");
      }

    }

    theQuery = theQuery.substring(0,theQuery.lastIndexOf(","));
    theQuery = theQuery+" WHERE ";

    // USING ALL VALUES TO TRIANGULATE THE ROW TO UPDATE (no primary key)
    if(initial_state_row.getParentTable().getPrimaryColumns().isEmpty())
    {
      for(Map.Entry<String,String> entry : initial_state_row.getContent().entrySet())
      {
        if(!entry.getKey().equals(chosenChange.getParentTableColumn().getName()))
        {
          if(chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("varchar") || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("bool"))
            theQuery = theQuery+(entry.getKey()+"='"+entry.getValue()+"' AND ");
          else
            theQuery = theQuery+(entry.getKey()+"="+entry.getValue()+" AND ");
        }
        else
        {
          if(undo)
            theQuery = theQuery+(entry.getKey()+"='"+chosenChange.getNewValue()+"' AND ");
          else
            theQuery = theQuery+(entry.getKey()+"='"+chosenChange.getOldValue()+"' AND ");
        }
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

  @Override
  public String toString()
  {
    return "Mutation[ id : "+id+" ChosenChange : "+chosenChange+" ]";
  }

  public boolean compare(Mutation mutation)
  {
    boolean res = false;
    if(this.getId() == mutation.getId())
      res=true;

    if(this.initial_state_row.compare(mutation.getInitial_state_row()) && this.chosenChange.compare(mutation.getChosenChange()))
      res = true;

    return res;
  }

}
