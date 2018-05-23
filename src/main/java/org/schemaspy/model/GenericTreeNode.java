package org.schemaspy.model;

import java.sql.PreparedStatement;

import java.util.*;
import java.util.ArrayList;
import java.util.List;

import org.schemaspy.*;

public class GenericTreeNode {


    private final Integer id;
    private Integer interest_mark;
    private Integer weight;
    private Integer subTreeWeight;
    private int depth;
    private final Row initial_state_row;
    private Row post_change_row;
    private ArrayList<SingleChange> potential_changes = new ArrayList<SingleChange>();
    private ArrayList<SingleChange> cascadeFK = new ArrayList<SingleChange>(); // a integrer
    private boolean cascadingFK;
    private GenericTreeNode parent;
    private ArrayList<GenericTreeNode> children = new ArrayList<GenericTreeNode>();
    private SingleChange chosenChange;

    /**
     * Default GenericTreeNode constructor
     */
    public GenericTreeNode(Row initial_state_row, int id) { // used only for rootMutation
        this.cascadingFK = false;
        this.subTreeWeight = 0;
        this.parent = null;
        this.weight = 1;
        this.depth = 0;
        this.id = id;
        this.initial_state_row = initial_state_row;
        this.potential_changes = discoverMutationPossibilities(this);
    }

    public GenericTreeNode(Row initial_state_row, int id, GenericTreeNode rootMutation, GenericTreeNode parentMutation) {
        this.parent = parentMutation;
        this.cascadingFK = false;
        this.subTreeWeight = 0;
        this.weight = 1;
        this.id = id;
        initDepth();
        initDepth();
        this.initial_state_row = initial_state_row;
        this.potential_changes = discoverMutationPossibilities(rootMutation);
    }

    public Integer getId() {
        return id;
    }

    public void setSubTreeWeight(int subTreeWeight)
    {
        this.subTreeWeight = subTreeWeight;
    }

    public void setPotential_changes(ArrayList<SingleChange> potCh) //used in tests
    {
        this.potential_changes = potCh;
    }

    public Integer getWeight() {
        return this.weight;
    }

    public void initDepth() {
        GenericTreeNode tmp = this;
        int cpt = 0;
        while (tmp.getParent() != null) {
            tmp = tmp.getParent();
            cpt++;
        }

        this.depth = cpt;
    }

    public boolean checkWeightConsistency() {
        int tmp = 0;
        for (GenericTreeNode child : this.getChildren()) {
            tmp += child.getWeight();
        }

        if (tmp != this.getSubTreeWeight() && !this.getChildren().isEmpty()) {
            System.out.println("Weight inconstistent " + this.getWeight() + "   " + this.getSubTreeWeight());
            System.out.println("Mutation concernee = " + this);
            return false;
        }

        return true;
    }

    public void updateSubTreeWeight() {
        int tmp = 0;
        for (GenericTreeNode child : this.getChildren()) {
            tmp += child.getWeight();
        }
        this.subTreeWeight = tmp;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     *
     */
    public SingleChange singleChangeBasedOnWeight()
    {
        final Random r = new Random();

        checkWeightConsistency();
        if (this.potential_changes.isEmpty() && (0 == subTreeWeight))
            System.out.println("ERROR PICKING : no potential_changes AND subtreeweight = 0");

        int rnd = r.nextInt(subTreeWeight + potential_changes.size());
        assert (rnd >= 0);

        if (rnd < potential_changes.size()) // checking if currentNode is the pick
           return potential_changes.remove(rnd);

        rnd -= potential_changes.size(); // removing the potential changes "weight" of the current node to match subtree Weight
        for (GenericTreeNode n : children) // launching on every child if current node wasnt picked.
        {
            int w = n.getWeight();
            if (rnd < w) {
                return n.singleChangeBasedOnWeight();
            }
            rnd -= w;
        }

        System.out.println("ici2");
        throw new Error("This should be impossible to reach");
    }

    public Row getPost_change_row() {
        return this.post_change_row;
    }

    public int getDepth() {
        return this.depth;
    }


    public SingleChange getChosenChange() {
        return chosenChange;
    }

    public int getSubTreeWeight() {
        return this.subTreeWeight;
    }


    public Row getInitial_state_row() {
        return initial_state_row;
    }

    public int getInterest_mark() {
        return this.interest_mark;
    }

    public void setInterest_mark(int mark) {
        this.interest_mark = mark;
    }

    /**
     * Returns value of potential_changes
     *
     * @return
     */
    public ArrayList<SingleChange> getPotential_changes() {
        return potential_changes;
    }

    public void setChosenChange(SingleChange sc) {
        this.chosenChange = sc;
        this.chosenChange.setAttachedToMutation(this);
    }

    /**
     * Sets new value of children
     *
     * @param
     */

    public void setParent(GenericTreeNode parent) {
        this.parent = parent;
    }

    public ArrayList<SingleChange> discoverMutationPossibilities(GenericTreeNode rootMutation) {

        if(initial_state_row == null)
            return null ;

        ArrayList<SingleChange> possibilities = new ArrayList<SingleChange>();

        //TRYING TO DISCOVER RAW POSSIBILITIES
        for (Map.Entry<String, String> content : initial_state_row.getContent().entrySet())
        {
            try
            {
                TableColumn parentColumn = initial_state_row.getParentTable().findTableColumn(content.getKey());
                possibilities.addAll(discoverFieldPossibilities(parentColumn, content.getValue(),rootMutation));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        //REMOVING POSSIBILITIES THAT DONT MATCH CONSTRAINTS
        for(SingleChange singleChange : possibilities)
        {
            if (!singleChange.respectsConstraints())
                possibilities.remove(singleChange);
        }
        return possibilities;
    }

    public ArrayList<SingleChange> discoverFieldPossibilities(TableColumn tableColumn, String column_value,GenericTreeNode rootMutation) throws Exception //listing of the mutation possibilities on the specified row
    {

        ArrayList<SingleChange> oneChange = new ArrayList<SingleChange>();
        String typeName = tableColumn.getTypeName();

        switch (typeName) {
            case "int2":
                int tmp = Integer.parseInt(rootMutation.getInitial_state_row().getContent().get(tableColumn.getName()));
                oneChange.add(new SingleChange(tableColumn, this, column_value, Integer.toString(tmp++)));
                oneChange.add(new SingleChange(tableColumn, this, column_value, Integer.toString(32767)));
                oneChange.add(new SingleChange(tableColumn, this, column_value, Integer.toString(1)));
                break;
            case "varchar":
                if (rootMutation == null) {
                    char tmp2 = column_value.charAt(0);
                    oneChange.add(new SingleChange(tableColumn, this, column_value, (Character.toString(tmp2++) + column_value.substring(1))));
                    oneChange.add(new SingleChange(tableColumn, this, column_value, (Character.toString(tmp2--) + column_value.substring(1))));
                } else {
                    char tmp2 = (char) rootMutation.getInitial_state_row().getContent().get(tableColumn.getName()).charAt(0);
                    char nextChar = (char) (tmp2 + 1);
                    char prevChar = (char) (tmp2 - 1);
                    oneChange.add(new SingleChange(tableColumn, this, column_value, (Character.toString(nextChar) + column_value.substring(1))));
                    oneChange.add(new SingleChange(tableColumn, this, column_value, (Character.toString(prevChar) + column_value.substring(1))));
                }

                break;
            case "bool":
                if (column_value.equals("f"))
                    oneChange.add(new SingleChange(tableColumn, this, column_value, "t"));
                if (column_value.equals("t"))
                    oneChange.add(new SingleChange(tableColumn, this, column_value, "f"));
                break;
              /*  case 5:  typeName = "May";
                       break;
              case 6:  typeName = "June";
                       break;
              case 7:  typeName = "July";
                       break;
              case 8:  typeName = "August";
                       break;
              case 9:  typeName = "September";
                       break;
              case 10: typeName = "October";
                       break;
              case 11: typeName = "November";
                       break;
              case 12: typeName = "December";
                       break;*/
            default:
                throw new Exception("No raw GenericTreeNode possibilities could be found");
        }


        return oneChange;
    }

    public boolean inject(SchemaAnalyzer analyzer, boolean undo)
    {

        if (undo)
            System.out.println("UNDOING");
        else
            System.out.println("INJECT");

        String theQuery = updateQueryBuilder(undo);
        try
        {
            PreparedStatement stmt = analyzer.getSqlService().prepareStatement(theQuery, analyzer.getDb(), null);
            stmt.execute();
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public void initPostChangeRow()
    {
        this.post_change_row = this.initial_state_row.clone();
        this.post_change_row.setValueOfColumn(chosenChange.getParentTableColumn().getName(), chosenChange.getNewValue());
    }

    public boolean undo(SchemaAnalyzer analyzer)
    {
        try
        {
           return this.inject(analyzer, true);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public String updateQueryBuilder(boolean undo) //undo variable tells if the function should build Inject string or Undo string
    {
        String theQuery;

        if (undo)
        {
            if (chosenChange.getParentTableColumn().getTypeName().equals("varchar") || chosenChange.getParentTableColumn().getTypeName().equals("bool"))
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + "='" + chosenChange.getOldValue() + "', ";
            else
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + " = " + chosenChange.getOldValue() + ", ";
        }
        else
        {
            if (chosenChange.getParentTableColumn().getTypeName().equals("varchar") || chosenChange.getParentTableColumn().getTypeName().equals("bool"))
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + "='" + chosenChange.getNewValue() + "', ";
            else
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + "=" + chosenChange.getNewValue() + ", ";
        }
        for (Map.Entry<String, String> entry : initial_state_row.getContent().entrySet())
        {
            if (!entry.getKey().equals(chosenChange.getParentTableColumn().getName()))
            {
                if (chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("varchar") || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("bool"))
                    theQuery = theQuery + (entry.getKey() + "='" + entry.getValue() + "', ");
                else
                    theQuery = theQuery + (entry.getKey() + "=" + entry.getValue() + ", ");
            }
        }

        theQuery = theQuery.substring(0, theQuery.lastIndexOf(","));
        theQuery = theQuery + " WHERE ";

        // USING ALL VALUES TO TRIANGULATE THE ROW TO UPDATE (no primary key)
        if (initial_state_row.getParentTable().getPrimaryColumns().isEmpty())
        {
            for (Map.Entry<String, String> entry : initial_state_row.getContent().entrySet())
            {
                if (!entry.getKey().equals(chosenChange.getParentTableColumn().getName()))
                {
                    if (chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("varchar") || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("bool"))
                        theQuery = theQuery + (entry.getKey() + "='" + entry.getValue() + "' AND ");
                    else
                        theQuery = theQuery + (entry.getKey() + "=" + entry.getValue() + " AND ");
                }
                else
                {
                    if (undo)
                        theQuery = theQuery + (entry.getKey() + "='" + chosenChange.getNewValue() + "' AND ");
                    else
                        theQuery = theQuery + (entry.getKey() + "='" + chosenChange.getOldValue() + "' AND ");
                }
            }
            theQuery = theQuery.substring(0, theQuery.lastIndexOf(" AND "));
        }
        else
            theQuery = theQuery + (" " + initial_state_row.getParentTable().getPrimaryColumns().get(0).getName() + "=" + initial_state_row.getValueOfColumn(initial_state_row.getParentTable().getPrimaryColumns().get(0).getName()));


        //System.out.println("build query ! "+theQuery); uncomment to see built request;
        return theQuery;
    }


    //NOT FUNCTIONNAL
//    public ArrayList<SingleChange> checkCascadeFK(SingleChange chosenChange, SchemaAnalyzer analyzer)
//    {
//      ArrayList<SingleChange> res = new ArrayList<SingleChange>();
//      int i,j;
//
//      for(Map.Entry<String,Collection<ForeignKeyConstraint>> entry : analyzer.getDb().getLesForeignKeys().entrySet())
//      {
//        Iterator<ForeignKeyConstraint> iter = entry.getValue().iterator();
//        while (iter.hasNext()) {
//          ForeignKeyConstraint elem = iter.next();
//          for(i = 0; i < elem.getParentColumns().size();i++)
//          {
//                if(elem.getParentColumns().get(i).getName().equals(chosenChange.getParentTableColumn().getName()))
//                  res.add(new SingleChange(elem.getParentColumns().get(i),this,chosenChange.getOldValue(),chosenChange.getNewValue()));
//                if(elem.getChildColumns().get(i).getName().equals(chosenChange.getParentTableColumn().getName()))
//                  res.add(new SingleChange(elem.getChildColumns().get(i),this,chosenChange.getOldValue(),chosenChange.getNewValue()));
//          }
//        }
//      }
//
//      for( i = 0; i < res.size();i++)
//      {
//        for( j = 0; j < res.size();j++)
//        {
//          if(res.get(i).equals(res.get(j)))
//            res.remove(res.get(j));
//        }
//      }
//
//      System.out.println("LA PRESENCE DE FOREIGN KEY EST"+ res);
//      return res;
//    }


    public boolean compare(GenericTreeNode genericTreeNode)
    {
        boolean res = false;
        if (this.getId() == genericTreeNode.getId())
            res = true;

        if (this.initial_state_row.compare(genericTreeNode.getInitial_state_row()) && this.chosenChange.compare(genericTreeNode.getChosenChange()))
            res = true;

        return res;
    }

    public boolean undoToMutation(GenericTreeNode target, SchemaAnalyzer analyzer)
    {
        ArrayList<GenericTreeNode> goingUp = findPathToMutation(target).get(0);
        ArrayList<GenericTreeNode> goingDown = findPathToMutation(target).get(1);

        for(GenericTreeNode node : goingUp )
        {
            node.undo(analyzer);
        }

        for(GenericTreeNode node : goingDown )
        {
            node.inject(analyzer, false);
        }

        return true;
    }

    public GenericTreeNode getParent() {
        return this.parent;
    }

    public List<GenericTreeNode> getChildren() {
        return this.children;
    }

    public int getNumberOfChildren() {
        return getChildren().size();
    }

    public boolean hasChildren() {
        return (getNumberOfChildren() > 0);
    }

    public void setChildren(ArrayList<GenericTreeNode> children)
    {
        for (GenericTreeNode child : children)
        {
            child.parent = this;
        }

        this.children = children;
    }

    public void addChild(GenericTreeNode child)
    {
        child.parent = this;
        children.add(child);
    }

    public String toString()
    {
        return "[ MUT ID " + this.getId() + " Depth = " + this.getDepth() + " SG " + this.chosenChange + "]";
    }

    public ArrayList<ArrayList<GenericTreeNode>> findPathToMutation(GenericTreeNode target)
    {
        ArrayList<ArrayList<GenericTreeNode>> finalPath = new ArrayList<ArrayList<GenericTreeNode>>();
        ArrayList<GenericTreeNode> thisPath = new ArrayList<GenericTreeNode>();
        ArrayList<GenericTreeNode> targetPath = new ArrayList<GenericTreeNode>();

        GenericTreeNode tmpTarget = target;
        GenericTreeNode tmpThis = this;

        int depthOffset = -1;

        while (depthOffset != 0)
        {
            depthOffset = tmpThis.getDepth() - tmpTarget.getDepth();
            if (depthOffset > 0)
            {
                thisPath.add(tmpThis);
                tmpThis = tmpThis.getParent();
            }
            else if (depthOffset < 0)
            {
                targetPath.add(tmpTarget);
                tmpTarget = tmpTarget.getParent();
            }
        }

        while (!tmpThis.compare(tmpTarget))
        {
            thisPath.add(tmpThis);
            targetPath.add(tmpTarget);

            tmpThis = tmpThis.getParent();
            tmpTarget = tmpTarget.getParent();
        }

        Collections.reverse(targetPath);
        finalPath.add(thisPath); //way up
        finalPath.add(targetPath); // way down

        return finalPath; // returns the way up to first commun ancestor as index 0 and the way down to the target from the FCA as index 1

    }

    public void initWeight() // Modify euristic here when refining the choosing patern
    {
        setWeight(this.interest_mark); // eventually consider depth?
    }

    public boolean isSingleChangeOnCurrentPath(GenericTreeNode rootMutation)
    {
        ArrayList<GenericTreeNode> finalPath = new ArrayList<GenericTreeNode>();
        finalPath.addAll(this.findPathToMutation(rootMutation).get(0));
        finalPath.addAll(this.findPathToMutation(rootMutation).get(1));

        for (GenericTreeNode mutOnPath : finalPath)
        {
            if (mutOnPath.getChosenChange().compare(this.getChosenChange()))
                return false;
        }
        return true;
    }


    public void propagateWeight()
    {
        this.updateSubTreeWeight();

        if (this.getParent() != null)
            this.getParent().propagateWeight();
    }

}
