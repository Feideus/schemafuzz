
package org.schemaspy.model;


import org.schemaspy.SchemaAnalyzer;

import java.util.ArrayList;

public class SingleChange
{
  private GenericTreeNode attachedToMutation;
  private TableColumn parentTableColumn;
  private Object oldValue;
  private Object newValue;


	public SingleChange(TableColumn parentColumn ,GenericTreeNode attachedToMutation, Object oldValue, Object newValue)
    {
        this.attachedToMutation = attachedToMutation;
        this.parentTableColumn = parentColumn;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}


    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public boolean respectsConstraints()
  {
    switch (parentTableColumn.getTypeName())
    {
          case "serial":
                        if(Integer.parseInt(newValue.toString()) < Math.pow(2,parentTableColumn.getLength()))
                            return true;
                        return false;
          case "numeric":
                        if(Integer.parseInt(newValue.toString()) < Math.pow(2,parentTableColumn.getLength()))
                            return true;
                        return false;
          case "int2":
                        if(Integer.parseInt(newValue.toString()) <= 32767)
                            return true;
                        return false;
          default:
            return true;
    }
  }

    public void setParentTableColumn(TableColumn parentTableColumn) {
        this.parentTableColumn = parentTableColumn;
    }

    @Override
   public String toString()
   {
     return "\n[SG - attachedToMutation : "+this.getAttachedToMutation().getId()+" | parentTable : "+this.getParentTableColumn().getTable()+" | parentTableColumn : "+this.getParentTableColumn().getName().toString()+" | OV : "+oldValue+" | NV : "+newValue+" ]\n";
   }

   public Object getOldValue()
    {
      return oldValue;
    }

    public Object getNewValue()
    {
      return newValue;
    }

    public TableColumn getParentTableColumn()
    {
      return parentTableColumn;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public boolean compare(SingleChange chosenChange)
    {
        if(chosenChange == null || this == null )
            return false;

        if(!chosenChange.getNewValue().equals(this.getNewValue()) || !chosenChange.getOldValue().equals(this.getOldValue()))
            return false;

        if(!chosenChange.getParentTableColumn().getTable().getName().equals(this.getParentTableColumn().getTable().getName()))
          return false;

        if(!chosenChange.getParentTableColumn().getName().equals(this.getParentTableColumn().getName()))
          return false;


      return true;
    }

    public GenericTreeNode getAttachedToMutation()
    {
      return this.attachedToMutation;
    }

    public void setAttachedToMutation(GenericTreeNode attachedToMutation)
    {
      this.attachedToMutation = attachedToMutation;
    }

    public boolean compareValues()
    {
        if(this.getNewValue().toString().equals(this.getOldValue().toString()))
            return true;

        return false;
    }

}
