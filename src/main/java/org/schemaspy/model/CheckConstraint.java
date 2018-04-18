/*
 * This file is a part of the SchemaSpy project (http://schemaspy.org).
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 John Currier
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.schemaspy.model;
import java.util.*;



/**
 * Represents a <a href='http://en.wikipedia.org/wiki/Foreign_key'>
 * Foreign Key Constraint</a> that "ties" a child table to a parent table
 * via foreign and primary keys.
 */
public class CheckConstraint {
    private String name;
    private Table parentTable;
    private TableColumn checkedColumn;
    private TableColumn targetColumn;
    private String constantValue;
    private String operation;
    private ArrayList<CheckConstraint> orClauses;

    public CheckConstraint()
    {
      super();
  	}



  	/**
  	* Default CheckConstraint constructor
  	*/
  	public CheckConstraint(Table parentTable, TableColumn checkedColumn, TableColumn targetColumn, String constantValue,String name, String operation) {
  		this.parentTable = parentTable;
  		this.checkedColumn = checkedColumn;
  		this.targetColumn = targetColumn;
  		this.constantValue = constantValue;
      this.operation = operation;
      this.name = name;
  	}

    public ArrayList<CheckConstraint> getOrClauses()
    {
      return orClauses;
    }

    public void setOrClauses(ArrayList<CheckConstraint> orClauses)
    {
      this.orClauses = orClauses;
    }

    public boolean equals(final CheckConstraint ckConstraint) {
          Boolean res = false;

            if(this.getName() == ckConstraint.getName() && this.getOperation() == ckConstraint.getOperation() && this.getTargetColumn() == ckConstraint.getTargetColumn() && this.getCheckedColumn() == ckConstraint.getCheckedColumn())
                res = true;
            return res;
    }

	/**
	* Returns value of name
	* @return
	*/
	public String getName() {
		return name;
	}

	/**
	* Returns value of parentTable
	* @return
	*/
	public Table getParentTable() {
		return parentTable;
	}

	/**
	* Sets new value of parentTable
	* @param
	*/
	public void setParentTable(Table parentTable) {
		this.parentTable = parentTable;
	}

	/**
	* Returns value of checkedColumn
	* @return
	*/
	public TableColumn getCheckedColumn() {
		return checkedColumn;
	}

	/**
	* Sets new value of checkedColumn
	* @param
	*/
	public void setCheckedColumn(TableColumn checkedColumn) {
		this.checkedColumn = checkedColumn;
	}

	/**
	* Returns value of targetColumn
	* @return
	*/
	public TableColumn getTargetColumn() {
		return targetColumn;
	}

	/**
	* Sets new value of targetColumn
	* @param
	*/
	public void setTargetColumn(TableColumn targetColumn) {
		this.targetColumn = targetColumn;
	}

	/**
	* Returns value of constantValue
	* @return
	*/
	public Object getConstantValue() {
		return constantValue;
	}

	/**
	* Sets new value of constantValue
	* @param
	*/
	public void setConstantValue(String constantValue) {
		this.constantValue = constantValue;
	}

	/**
	* Returns value of operation
	* @return
	*/
	public String getOperation() {
		return operation;
	}

	/**
	* Sets new value of operation
	* @param
	*/
	public void setOperation(String operation) {
		this.operation = operation;
	}

	/**
	* Returns value of Logger
	* @return
  */

	/**
	* Create string representation of CheckConstraint for printing
	* @return
	*/
	@Override
	public String toString() {
		return "CheckConstraint [name=" + name + "\n parentTable=" + parentTable + "\n checkedColumn=" + checkedColumn + "\n targetColumn=" + targetColumn + "\n constantValue=" + constantValue + "\n operation=" + operation + "]\n\n";
	}

 public static CheckConstraint parse (String tableName ,String ccName, String stringCC, Database db)
 {

      CheckConstraint cc = new CheckConstraint();

      cc.name = ccName;
      cc.parentTable = db.getTablesByName().get(tableName);

      if(stringCC.contains("OR"))
      {
        cc.orClauses.add(CheckConstraint.parse(cc.parentTable.getName(),ccName,stringCC.substring(stringCC.indexOf("OR")+2),db));
        stringCC = stringCC.substring(0,stringCC.indexOf("OR"));
      }

      if(stringCC.contains("AND"))
      {
        CheckConstraint.parse(cc.parentTable.getName(),ccName,stringCC.substring(stringCC.indexOf("AND")+3),db);
        stringCC = stringCC.substring(0,stringCC.indexOf("AND"));
      }

      if(stringCC.contains("<") && !stringCC.contains(">"))
      {
        if(db.columnExists(stringCC.substring(stringCC.lastIndexOf("(")+1,stringCC.indexOf("<")),cc.parentTable))
        {
          cc.checkedColumn =  db.findColumn(stringCC.substring(stringCC.lastIndexOf("(")+1,stringCC.indexOf("<")),cc.parentTable);
        }

        if(db.columnExists(stringCC.substring(stringCC.indexOf("<"),stringCC.indexOf(")")),cc.parentTable))
        {
          cc.targetColumn =  db.findColumn(stringCC.substring(stringCC.indexOf("<"),stringCC.indexOf(")")),cc.parentTable);
        }
        else
        {
          if(stringCC.substring(stringCC.indexOf("<")+1,stringCC.indexOf("<")+2).equals("="))
            cc.constantValue = stringCC.substring(stringCC.indexOf("<")+2,stringCC.indexOf(")"));
          else
            cc.constantValue = stringCC.substring(stringCC.indexOf("<")+1,stringCC.indexOf(")"));
        }


        if(stringCC.substring(stringCC.indexOf("<"),stringCC.indexOf("<")+1) == "=")
        {
          cc.operation = "<=";
        }
        else
          cc.operation = "<";
      }
      else if(stringCC.contains(">") && !stringCC.contains("<"))
      {
                      if(db.columnExists(stringCC.substring(stringCC.lastIndexOf("(")+1,stringCC.indexOf(">")),cc.parentTable))
                      {
                        cc.checkedColumn =  db.findColumn(stringCC.substring(stringCC.lastIndexOf("(")+1,stringCC.indexOf(">")),cc.parentTable);
                      }
                      if(db.columnExists(stringCC.substring(stringCC.indexOf(">"),stringCC.indexOf(")")),cc.parentTable))
                      {
                        cc.targetColumn =  db.findColumn(stringCC.substring(stringCC.indexOf(">"),stringCC.indexOf(")")),cc.parentTable);
                      }
                      else
                      {
                        if(stringCC.substring(stringCC.indexOf(">")+1,stringCC.indexOf(">")+2).equals("="))
                          cc.constantValue = stringCC.substring(stringCC.indexOf(">")+2,stringCC.indexOf(")"));

                        else
                          cc.constantValue = stringCC.substring(stringCC.indexOf(">")+1,stringCC.indexOf(")"));
                      }

                      if(stringCC.substring(stringCC.indexOf(">"),stringCC.indexOf(">")+1) == "=")
                          cc.operation = ">=";
                      else
                          cc.operation = ">";
      }
      else if(stringCC.contains("=") && !stringCC.contains(">") && !stringCC.contains("<"))
      {
        if(db.columnExists(stringCC.substring(stringCC.lastIndexOf("(")+1,stringCC.indexOf("=")),cc.parentTable))
        {
          cc.checkedColumn =  db.findColumn(stringCC.substring(stringCC.lastIndexOf("(")+1,stringCC.indexOf("=")),cc.parentTable);
        }
        if(db.columnExists(stringCC.substring(stringCC.indexOf("="),stringCC.indexOf(")")),cc.parentTable))
        {
          cc.targetColumn =  db.findColumn(stringCC.substring(stringCC.lastIndexOf("="),stringCC.indexOf(")")),cc.parentTable);
        }
        else
          cc.constantValue = stringCC.substring(stringCC.indexOf("=")+1,stringCC.indexOf(")"));

          cc.operation = "=";
      }

    return cc;

 }

 public static ArrayList<CheckConstraint> parseAll(String tableName, Map<String,String> stringCCMap, Database db)
 {

   ArrayList<CheckConstraint> res = new ArrayList<CheckConstraint>();

   for (Map.Entry<String, String> entry : stringCCMap.entrySet())
   {
     if(entry != null)
     {
        res.add(CheckConstraint.parse(tableName, entry.getKey(), entry.getValue(), db));
     }

   }

   return res;
 }

}
