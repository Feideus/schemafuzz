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
import java.util.*;

/**
 * Represents a <a href='http://en.wikipedia.org/wiki/Foreign_key'>
 * Foreign Key Constraint</a> that "ties" a child table to a parent table
 * via foreign and primary keys.
 */
public class QueryResponseParser
{
    private ResultSetMetaData resultMeta;
    private QueryResponse formatedResponse;


	public QueryResponseParser() {
    this.formatedResponse = new QueryResponse();
	}

	public QueryResponse getFormatedResponse() {
		return formatedResponse;
	}

	public void setFormatedResponse(QueryResponse formatedResponse) {
		this.formatedResponse = formatedResponse;
	}

  public QueryResponse parse(ResultSet resultOfQuery,Table parentTable) throws Exception
  {
    int i = 0;
      try
      {
        QueryResponse queryResponse = new QueryResponse();
        if(!resultOfQuery.isClosed())
        {
          this.resultMeta = resultOfQuery.getMetaData();

          while(resultOfQuery.next())
          {
            HashMap<String,String> mapOfTheRow = new HashMap<String,String>();

            for(i = 1; i <= resultMeta.getColumnCount();i++)
            {
              mapOfTheRow.put(resultMeta.getColumnName(i), resultOfQuery.getString(i));
            }

            Row currentRow = new Row(parentTable,mapOfTheRow,resultMeta.getColumnCount());
            queryResponse.getRows().add(currentRow);
          }
          return queryResponse;
        }

      }
      catch(SQLException e)
      {
        e.printStackTrace();
      }

      throw new Exception();
    }

}
