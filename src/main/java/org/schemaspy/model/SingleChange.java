
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
     return "\nSingleChange {\nparentTableColumn : "+this.parentTableColumn.getName()+"\n oldValue :"+oldValue+"\n newValue :"+newValue+"\n}";
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
}
