/*

import com.sun.rowset.internal.Row;
import java.sql.*;
import java.sql.ResultSet;
import java.util.*;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class Row
{
    private Table parentTable;
    private HashMap<String,Object> content;
    private Integer nbKeys;

	public Row() {
    		this.content = new HashMap<String,Object>();
	}

	public Row(Table parentTable, HashMap<String,Object> content, Integer nbKeys)
    {
        this.parentTable = parentTable;
        this.content = new HashMap<String,Object>();
        this.content = content;
        this.nbKeys = nbKeys;
	}

  public Table getParentTable()
  {
    return this.parentTable;
  }

	public HashMap<String,Object> getContent() {
		return content;
	}

	public void setContent(HashMap<String,Object> content) {
		this.content = content;
	}


	public Integer getNbKeys() {
		return nbKeys;
	}

  public Object getValueOfColumn(String columnName)
  {
    return content.get(columnName);
  }

  public void setValueOfColumn(String columnName, Object newVal)
  {
    this.getContent().replace(columnName, newVal);
  }

	public void setNbKeys(Integer nbKeys) {
		this.nbKeys = nbKeys;
	}

	@Override
	public String toString() {
	  String res= "table : "+parentTable.toString()+"content :"+content.toString();
    return res;
	}

  public boolean compare(Row initial_state_row)
  {

    if(content.size() != initial_state_row.getContent().size())
      return false;

    for(Map.Entry<String,Object> entry : content.entrySet())
    {
      if(!initial_state_row.getContent().containsKey(entry.getKey()))
        return false;

      if(!initial_state_row.getContent().get(entry.getKey()).equals(entry.getValue()))
          return false;
    }
      return true;
  }

  public Row clone()
  {
    HashMap<String,Object> clonedMap = (HashMap<String,Object>) this.content.clone();
    Row res = new Row(this.parentTable,clonedMap,this.content.keySet().size());

    return res;
  }
}
