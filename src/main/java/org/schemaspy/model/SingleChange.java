
package org.schemaspy.model;


public class SingleChange
{
  private GenericTreeNode attachedToMutation;
  private TableColumn parentTableColumn;
  private String oldValue;
  private String newValue;


	public SingleChange(TableColumn parentColumn ,GenericTreeNode attachedToMutation, String oldValue, String newValue) {
    this.parentTableColumn = parentColumn;
		this.attachedToMutation = attachedToMutation;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

  public boolean respectsConstraints()
  {
    switch (parentTableColumn.getTypeName())
    {
          case "serial":
                        if(Integer.parseInt(newValue) < Math.pow(2,parentTableColumn.getLength()))
                          return true;
                        return false;
          case "numeric":
                        if(Integer.parseInt(newValue) < Math.pow(2,parentTableColumn.getLength()))
                          return true;
                        return false;
          case "int2":
                      if(Integer.parseInt(newValue) <= 32767)
                      {
                        return true;
                      }
                      return false;
          default:
            return true;

    }
  }


   @Override
   public String toString()
   {
     return "\n[SG - attachedToMutation : "+this.getattachedToMutation().getId()+"| OV :"+oldValue+" | NV :"+newValue+" ]\n";
   }

   public String getOldValue()
    {
      return oldValue;
    }

    public String getNewValue()
    {
      return newValue;
    }

    public TableColumn getParentTableColumn()
    {
      return parentTableColumn;
    }

    public boolean compare(SingleChange chosenChange)
    {
      if(!chosenChange.getParentTableColumn().getTable().getName().equals(this.getParentTableColumn().getTable().getName()))
        return false;

      if(!chosenChange.getParentTableColumn().getName().equals(this.getParentTableColumn().getName()))
        return false;

      if(!chosenChange.getNewValue().equals(this.getNewValue()) || !chosenChange.getOldValue().equals(this.getOldValue()))
        return false;

      return true;
    }

    public GenericTreeNode getattachedToMutation()
    {
      return this.attachedToMutation;
    }

    public void setAttachedToMutation(GenericTreeNode attachedToMutation)
    {
      this.attachedToMutation = attachedToMutation;
    }
}
