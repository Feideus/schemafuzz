
package org.schemaspy.model;


public class SingleChange
{
  private Mutation parentMutation;
  private TableColumn parentTableColumn;
  private String oldValue;
  private String newValue;


	public SingleChange(TableColumn parentColumn ,Mutation parentMutation, String oldValue, String newValue) {
    this.parentTableColumn = parentColumn;
		this.parentMutation = parentMutation;
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
     return "\n[SG - ParentMutation : "+this.getParentMutation().getId()+"| OV :"+oldValue+" | NV :"+newValue+" ]\n";
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
      if(chosenChange.getNewValue() != this.getNewValue() || chosenChange.getOldValue() != this.getOldValue())
        return false;

      return true;
    }

    public Mutation getParentMutation()
    {
      return this.parentMutation;
    }
}
