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
import java.util.ArrayList;


public class QueryResponse
{
    private ArrayList<Row> rows;
    private Integer nbRows;

    /**
  	* Default empty QueryResponse constructor
  	*/
  	public QueryResponse()
    {
      this.rows = new ArrayList<Row>();
  	}

  	/**
  	* Default QueryResponse constructor
  	*/
  	public QueryResponse(ArrayList<Row> rows, Integer nbRows)
    {
  		this.rows = new ArrayList<Row>();
      	this.rows = rows;
  		this.nbRows = nbRows;
  	}
	/**
	* Returns value of rows
	* @return
	*/
	public ArrayList<Row> getRows() {
		return rows;
	}

  	public void addRow(Row row)
  {
    rows.add(row);
  }

	/**
	* Sets new value of rows
	* @param
	*/
	public void setRows(ArrayList<Row> rows) {
		this.rows = rows;
	}

	/**
	* Returns value of nbRows
	* @return
	*/
	public Integer getNbRows() {
		return nbRows;
	}

	/**
	* Sets new value of nbRows
	* @param
	*/
	public void setNbRows(Integer nbRows) {
		this.nbRows = nbRows;
	}

  	public String toString()
  {
    return "Rows = "+rows.toString();
  }


}
