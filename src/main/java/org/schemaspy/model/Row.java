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
    private HashMap<String,String> content;
    private Integer nbKeys;

	public Row() {
		super();
	}

	public Row(HashMap<String,String> content, Integer nbKeys) {
		super();
		this.content = content;
		this.nbKeys = nbKeys;
	}

	public HashMap<String,String> getRow() {
		return content;
	}

	public void setRow(HashMap<String,String> content) {
		this.content = content;
	}


	public Integer getNbKeys() {
		return nbKeys;
	}


	public void setNbKeys(Integer nbKeys) {
		this.nbKeys = nbKeys;
	}

	@Override
	public String toString() {
	  String res= "";
    for (String name: content.keySet())
    {

            String key =name.toString();
            String value = content.get(name).toString();
            res.concat("Column ="+key+"Value ="+value);
    }
    return res;
	}
}
