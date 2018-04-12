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

 public class NotNullConstraint
 {
   private String name;
   private Table parentTable;
   private TableColumn notNullColumn;
   private boolean notNUll;

	/**
	* Default empty NotNullConstraint constructor
	*/
	public NotNullConstraint(String name) {
    this.name = name;
	}

	/**
	* Default NotNullConstraint constructor
	*/
	public NotNullConstraint(String name, Table parentTable, TableColumn notNullColumn, boolean notNUll) {
		this.name = name;
		this.parentTable = parentTable;
		this.notNullColumn = notNullColumn;
		this.notNUll = notNUll;
	}

	/**
	* Returns value of name
	* @return
	*/
	public String getName() {
		return name;
	}

	/**
	* Sets new value of name
	* @param
	*/
	public void setName(String name) {
		this.name = name;
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
	* Returns value of notNullColumn
	* @return
	*/
	public TableColumn getNotNullColumn() {
		return notNullColumn;
	}

	/**
	* Sets new value of notNullColumn
	* @param
	*/
	public void setNotNullColumn(TableColumn notNullColumn) {
		this.notNullColumn = notNullColumn;
	}

	/**
	* Returns value of notNUll
	* @return
	*/
	public boolean isNotNUll() {
		return notNUll;
	}

	/**
	* Sets new value of notNUll
	* @param
	*/
	public void setNotNUll(boolean notNUll) {
		this.notNUll = notNUll;
	}
}
