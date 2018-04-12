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
import java.sql.ResultSet;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.lang.invoke.MethodHandles;
/**
 * Represents a <a href='http://en.wikipedia.org/wiki/Foreign_key'>
 * Foreign Key Constraint</a> that "ties" a child table to a parent table
 * via foreign and primary keys.
 */
public class QueryResponseParser
{
    private ResultSetMetaData resultMeta;
    private QueryResponse formatedResponse;
    private final static Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


	public QueryResponseParser() {
	}

	public QueryResponse getFormatedResponse() {
		return formatedResponse;
	}


	public void setFormatedResponse(QueryResponse formatedResponse) {
		this.formatedResponse = formatedResponse;
	}

  public QueryResponse parse(ResultSet resultOfQuery)
  {

    QueryResponse queryResponse = new QueryResponse();
    int i = 0;

    if(resultOfQuery != null)
    {
      try
      {
        this.resultMeta = resultOfQuery.getMetaData();
        while(resultOfQuery.next())
        {
          HashMap<String,String> mapOfTheRow = new HashMap();

          for(i = 0; i < resultMeta.getColumnCount();i++)
          {
            mapOfTheRow.put(resultMeta.getColumnName(i), resultOfQuery.getBlob(i).toString());
          }

          Row currentRow = new Row(mapOfTheRow,resultMeta.getColumnCount());
          queryResponse.addRow(currentRow);
        }
      }
      catch(SQLException e)
      {
        LOGGER.debug("Parsing the reponse Threw an error : "+e);
      }
    }

    return queryResponse;
  }

}
