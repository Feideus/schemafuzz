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




/**
 * Represents a <a href='http://en.wikipedia.org/wiki/Foreign_key'>
 * Foreign Key Constraint</a> that "ties" a child table to a parent table
 * via foreign and primary keys.
 */
public class CheckConstraint {
    private final String name;
    private Table parentTable;
    private TableColumn checkedColumn;
    private TableColumn targetColumn;
    private Object constantValue;
    private String operation;



    public CheckConstraint(String name) {
      this.name = name;
  	}

  	/**
  	* Default CheckConstraint constructor
  	*/
  	public CheckConstraint(Table parentTable, TableColumn checkedColumn, TableColumn targetColumn, Object constantValue,String name, String operation) {
  		this.parentTable = parentTable;
  		this.checkedColumn = checkedColumn;
  		this.targetColumn = targetColumn;
  		this.constantValue = constantValue;
      this.operation = operation;
      this.name = name;
  	}

    /**
     * Returns a string representation of this foreign key constraint.
     *
     * @return
     */

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
	public void setConstantValue(Object constantValue) {
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
}
