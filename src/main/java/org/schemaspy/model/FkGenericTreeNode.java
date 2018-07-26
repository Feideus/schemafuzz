package org.schemaspy.model;

import org.schemaspy.service.SqlService;

import java.util.Map;

public class FkGenericTreeNode {

    private GenericTreeNode parent;
    private Row initial_state_row;
    private Row post_change_row;
    private SingleChange fkChange;
    private boolean targetsMultipleRows;

    public FkGenericTreeNode(Row initial_state_row,GenericTreeNode parent, SingleChange sg,boolean multipleRows)
    {
        this.targetsMultipleRows = multipleRows;
        this.parent = parent;
        this.initial_state_row = initial_state_row;
        this.fkChange = sg;
        initPostChangeRow();
    }


    public void initPostChangeRow()
    {
        this.post_change_row = this.initial_state_row.myClone();
        this.post_change_row.setValueOfColumn(fkChange.getParentTableColumn().getName(), fkChange.getNewValue());
    }

    public String updateQueryBuilder(boolean undo,Database db, SqlService sqlService) //undo variable tells if the function should build Inject string or Undo string
    {
        String theQuery;

        if (undo)
        {
            if (fkChange.getParentTableColumn().getTypeName().equals("varchar")
                    || fkChange.getParentTableColumn().getTypeName().equals("bool")
                    || fkChange.getParentTableColumn().getTypeName().equals("timestamp")
                    || fkChange.getParentTableColumn().getTypeName().equals("date")
                    || fkChange.getParentTableColumn().getTypeName().equals("_text")
                    || fkChange.getParentTableColumn().getTypeName().equals("text")
                    || fkChange.getParentTableColumn().getTypeName().equals("fulltext"))
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + fkChange.getParentTableColumn().getName() + "='" + fkChange.getOldValue().toString() + "', ";
            else
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + fkChange.getParentTableColumn().getName() + " = " + fkChange.getOldValue().toString() + ", ";
        }
        else
        {
            if (fkChange.getParentTableColumn().getTypeName().equals("varchar")
                    || fkChange.getParentTableColumn().getTypeName().equals("bool")
                    || fkChange.getParentTableColumn().getTypeName().equals("timestamp")
                    || fkChange.getParentTableColumn().getTypeName().equals("date")
                    || fkChange.getParentTableColumn().getTypeName().equals("_text")
                    || fkChange.getParentTableColumn().getTypeName().equals("text")
                    || fkChange.getParentTableColumn().getTypeName().equals("fulltext"))
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + fkChange.getParentTableColumn().getName() + "='" + fkChange.getNewValue().toString() + "', ";
            else
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + fkChange.getParentTableColumn().getName() + "=" + fkChange.getNewValue().toString() + ", ";
        }
        theQuery = theQuery.substring(0, theQuery.lastIndexOf(","));
        theQuery = theQuery + " WHERE ";


        for (Map.Entry<String, Object> entry : initial_state_row.getContent().entrySet())
        {
            if (!entry.getKey().equals(fkChange.getParentTableColumn().getName()))
            {
                if (fkChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("varchar")
                        || fkChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("bool")
                        || fkChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("timestamp")
                        || fkChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("date")
                        || fkChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("_text")
                        || fkChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("text")
                        || fkChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("fulltext"))
                    theQuery = theQuery + (entry.getKey() + "='" + entry.getValue().toString() + "' AND ");
            }
            else
            {
                if (undo)
                    theQuery = theQuery + (entry.getKey() + "='" + fkChange.getNewValue().toString() + "' AND ");
                else
                    theQuery = theQuery + (entry.getKey() + "='" + fkChange.getOldValue().toString() + "' AND ");
            }
        }
        theQuery = theQuery.substring(0, theQuery.lastIndexOf(" AND "));

        System.out.println(theQuery);

        return theQuery;
    }
}
