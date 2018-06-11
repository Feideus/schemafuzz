package org.schemaspy.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.schemaspy.*;
import org.schemaspy.service.SqlService;

public class GenericTreeNode {


    private final Integer id;
    private Integer interest_mark;
    private Integer weight;
    private Integer subTreeWeight;
    private int depth;
    private Row initial_state_row;
    private Row post_change_row;
    private ArrayList<SingleChange> potential_changes = new ArrayList<SingleChange>();
    //private ArrayList<SingleChange> cascadeFK = new ArrayList<SingleChange>(); // a integrer
    //private boolean cascadingFK;
    private GenericTreeNode parent;
    private ArrayList<GenericTreeNode> children = new ArrayList<GenericTreeNode>();
    private SingleChange chosenChange;
    private boolean isFirstApperance;
    private HashMap<TableColumn,FkGenericTreeNode> fkMutations = new HashMap<TableColumn,FkGenericTreeNode>();

    /**
     * Default GenericTreeNode constructor
     */
    public GenericTreeNode(Row initial_state_row, int id,SqlService sqlService) { // used only for rootMutation and Tests
        //this.cascadingFK = false;
        this.subTreeWeight = 0;
        this.parent = null;
        this.weight = 1;
        this.depth = 0;
        this.id = id;
        this.isFirstApperance = true;
        this.initial_state_row = initial_state_row;
        this.potential_changes = discoverMutationPossibilities(this,sqlService);
    }


    public GenericTreeNode(Row initial_state_row, int id, GenericTreeNode rootMutation, GenericTreeNode parentMutation,boolean isFirstApperance,SqlService sqlService) {
        //this.cascadingFK = false;
        this.parent = parentMutation;
        this.subTreeWeight = 0;
        this.weight = 1;
        this.id = id;
        initDepth();
        this.isFirstApperance = isFirstApperance;
        this.initial_state_row = initial_state_row;
        this.potential_changes = discoverMutationPossibilities(rootMutation,sqlService);
    }


    public boolean getIsFirstApperance() {
        return isFirstApperance;
    }

    public void setDepth(int depth) {
        this.depth = depth;
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

    public ArrayList<SingleChange> discoverMutationPossibilities(GenericTreeNode rootMutation,SqlService sqlService) {

        if(initial_state_row == null)
        {
            System.out.println("NO INITIAL STATE");
            return null ;
        }

        ArrayList<SingleChange> possibilities = new ArrayList<SingleChange>();

        //TRYING TO DISCOVER RAW POSSIBILITIES
        for (Map.Entry<String, Object> content : initial_state_row.getContent().entrySet())
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
        possibilities = removePotentialChangesThatDontMatchConstraints(possibilities,sqlService);
        if(possibilities.isEmpty())
            System.out.println("No raw Mutation could be found for this row");

        return possibilities;
    }

    public ArrayList<SingleChange> discoverFieldPossibilities(TableColumn tableColumn, Object column_value,GenericTreeNode rootMutation) throws Exception //listing of the mutation possibilities on the specified row
    {

        ArrayList<SingleChange> oneChange = new ArrayList<SingleChange>();

        String typeName = tableColumn.getTypeName();
        GenericTreeNode rootForThisMutation = FirstApperanceOf(this);

        System.out.println(typeName);


        switch (typeName) {
            case "smallint":
            case "integer":
            case "int2":

                Object tmp3 = rootForThisMutation.getInitial_state_row().getContent().get(tableColumn.getName());
                if( tmp3 != null && tmp3.toString() != "" )
                {
                    int tmp = Integer.parseInt(rootForThisMutation.getInitial_state_row().getContent().get(tableColumn.getName()).toString());
                    oneChange.add(new SingleChange(tableColumn, this, column_value, Integer.toString(tmp++)));
                    oneChange.add(new SingleChange(tableColumn, this, column_value, Integer.toString(32767)));
                    oneChange.add(new SingleChange(tableColumn, this, column_value, Integer.toString(0)));
                    break;
                }
            case "character":
            case "character varying": // MIXED CHARACTERS/NUMBERS STRINGS MAKE CHARAT CRASH AT 0 IF FIRST CHAR IS NUMBER. USE REGEX TO FIND FIRST ACTUAL LETTER ?
            case "varchar":


                Object tmp4 = rootForThisMutation.getInitial_state_row().getContent().get(tableColumn.getName());
                if(tmp4 != null && tmp4.toString() != "" )
                {

                    String tmp2 = tmp4.toString().replaceAll("\\d", "");
                    if (!tmp2.isEmpty())
                    {
                        char nextChar = (char) (tmp2.charAt(0) + 1);
                        char prevChar = (char) (tmp2.charAt(0) - 1);
                        oneChange.add(new SingleChange(tableColumn, this, column_value, (Character.toString(nextChar) + column_value.toString().substring(1))));
                        oneChange.add(new SingleChange(tableColumn, this, column_value, (Character.toString(prevChar) + column_value.toString().substring(1))));
                    }
                }
                break;
            case "bool":
                if (column_value.equals("f"))
                    oneChange.add(new SingleChange(tableColumn, this, column_value, "t"));
                if (column_value.equals("t"))
                    oneChange.add(new SingleChange(tableColumn, this, column_value, "f"));
                break;

            /*case "timestamp":
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Calendar cal = Calendar.getInstance();

                Date parsedDate = dateFormat.parse(column_value);
                cal.setTime(parsedDate);
                cal.add(Calendar.DAY_OF_WEEK,1);

                Timestamp tsInc = new java.sql.Timestamp(cal.getTime().getTime());
                oneChange.add(new SingleChange(tableColumn, this, column_value, tsInc.toString()));

                parsedDate = new Date(Long.MIN_VALUE);
                cal.setTime(parsedDate);

                Timestamp tsDinausors = new java.sql.Timestamp(cal.getTime().getTime());
                oneChange.add(new SingleChange(tableColumn, this, column_value, tsDinausors.toString()));

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
                System.out.println("Unsupported dataType = "+typeName);
        }

        return oneChange;
    }

    public boolean inject(SqlService sqlService,Database db, boolean undo)
    {
        String theQuery = "";
        if (undo)
            System.out.println("UNDOING");
        else
            System.out.println("INJECT");
        if(checkIfHasParentFk(db))
        {
            System.out.println("TRANSFERT");
            transferMutationToParent(db,sqlService);
        }

        theQuery = updateQueryBuilder(undo,db,sqlService);
        try
        {
            Statement stmt = sqlService.getConnection().createStatement();
            stmt.execute(theQuery);
            System.out.println("Query success");
            return true;
        }
        catch (Exception e)
        {
            System.out.println("Error ! : Mutation Canceled"+e); // temporerally putting aside the Unique constraint brought by the tranfertToParent Mechanic
            return false;
        }
    }

    public void initPostChangeRow()
    {
        this.post_change_row = this.initial_state_row.clone();
        this.post_change_row.setValueOfColumn(chosenChange.getParentTableColumn().getName(), chosenChange.getNewValue());
    }

    public boolean undo(SqlService sqlService,Database db)
    {
        try
        {
           return this.inject(sqlService,db, true);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public String updateQueryBuilder(boolean undo,Database db, SqlService sqlService) //undo variable tells if the function should build Inject string or Undo string
    {
        String theQuery;

        if (undo)
        {
            if (chosenChange.getParentTableColumn().getTypeName().equals("varchar")
                    || chosenChange.getParentTableColumn().getTypeName().equals("bool")
                    || chosenChange.getParentTableColumn().getTypeName().equals("timestamp")
                    || chosenChange.getParentTableColumn().getTypeName().equals("date")
                    || chosenChange.getParentTableColumn().getTypeName().equals("_text")
                    || chosenChange.getParentTableColumn().getTypeName().equals("text")
                    || chosenChange.getParentTableColumn().getTypeName().equals("fulltext")
                    || chosenChange.getParentTableColumn().getTypeName().equals("email"))
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + "='" + chosenChange.getOldValue().toString() + "', ";
            else
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + " = " + chosenChange.getOldValue().toString() + ", ";
        }
        else
        {
            if (chosenChange.getParentTableColumn().getTypeName().equals("varchar")
                    || chosenChange.getParentTableColumn().getTypeName().equals("bool")
                    || chosenChange.getParentTableColumn().getTypeName().equals("timestamp")
                    || chosenChange.getParentTableColumn().getTypeName().equals("date")
                    || chosenChange.getParentTableColumn().getTypeName().equals("_text")
                    || chosenChange.getParentTableColumn().getTypeName().equals("text")
                    || chosenChange.getParentTableColumn().getTypeName().equals("fulltext")
                    || chosenChange.getParentTableColumn().getTypeName().equals("email"))
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + "='" + chosenChange.getNewValue().toString() + "', ";
            else
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + "=" + chosenChange.getNewValue().toString() + ", ";
        }
        theQuery = theQuery.substring(0, theQuery.lastIndexOf(","));
        theQuery = theQuery + " WHERE ";


            for (Map.Entry<String, Object> entry : initial_state_row.getContent().entrySet())
            {
                if (!entry.getKey().equals(chosenChange.getParentTableColumn().getName()))
                {
                    if (chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("varchar")
                            || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("bool")
                            || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("timestamp")
                            || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("date")
                            || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("_text")
                            || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("text")
                            || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("fulltext")
                            || chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName().equals("email"))
                    {
                        if(entry.getValue() != null )
                            theQuery = theQuery + (entry.getKey() + "='" + entry.getValue().toString() + "' AND ");
                        else
                            theQuery = theQuery + (entry.getKey() + "= null AND ");
                    }
                }
                else
                {
                    if (undo)
                        theQuery = theQuery + (entry.getKey() + "='" + chosenChange.getNewValue().toString() + "' AND ");
                    else
                        theQuery = theQuery + (entry.getKey() + "='" + chosenChange.getOldValue().toString() + "' AND ");
                }
            }
            theQuery = theQuery.substring(0, theQuery.lastIndexOf(" AND "));

            System.out.println(theQuery);

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
        if(this == null || genericTreeNode == null )
            return false;

        if(this.getInitial_state_row() == null || genericTreeNode.getInitial_state_row() == null)
            return false;

        if (this.getId() == genericTreeNode.getId()) {
            return true;
        }

        if (this.initial_state_row.compare(genericTreeNode.getInitial_state_row()) && this.chosenChange.compare(genericTreeNode.getChosenChange()))
            return true;

        return false;
    }

    public boolean undoToMutation(GenericTreeNode target, SchemaAnalyzer analyzer)
    {
        ArrayList<GenericTreeNode> goingUp = findPathToMutation(target).get(0);
        ArrayList<GenericTreeNode> goingDown = findPathToMutation(target).get(1);

        for(GenericTreeNode node : goingUp )
        {
            node.undo(analyzer.getSqlService(),analyzer.getDb());
        }

        for(GenericTreeNode node : goingDown )
        {
            node.inject(analyzer.getSqlService(),analyzer.getDb(), false);
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

    public GenericTreeNode FirstApperanceOf (GenericTreeNode mutation)
    {
        if(mutation.getIsFirstApperance())
            return mutation;

        return FirstApperanceOf(mutation.getParent());
    }


//    public String updateQueryBuilderWrapper(boolean undo,Database db, SqlService sqlService)
//    {
//        String theQuery = "";
//
//            theQuery = "START TRANSACTION; SET CONSTRAINTS ALL DEFERRED;";
//
//                for (ForeignKeyConstraint fk : db.getLesForeignKeys().get(chosenChange.getParentTableColumn().getTable().getName().toUpperCase()))
//                {
//                        for (TableColumn tb : fk.getChildColumns())
//                        {
//                                String semiQuery = "SELECT * FROM " + tb.getTable() + " WHERE " + tb.getName() + "=";
//                                if (chosenChange.getParentTableColumn().getTypeName().equals("varchar")
//                                        || chosenChange.getParentTableColumn().getTypeName().equals("bool")
//                                        || chosenChange.getParentTableColumn().getTypeName().equals("timestamp")
//                                        || chosenChange.getParentTableColumn().getTypeName().equals("date")
//                                        || chosenChange.getParentTableColumn().getTypeName().equals("_text")
//                                        || chosenChange.getParentTableColumn().getTypeName().equals("text")
//                                        || chosenChange.getParentTableColumn().getTypeName().equals("fulltext"))
//                                    semiQuery = semiQuery + "' " + chosenChange.getNewValue() + " ' ORDER BY RANDOM() LIMIT 1";
//                                else
//                                    semiQuery = semiQuery + chosenChange.getNewValue() + " ORDER BY RANDOM() LIMIT 1";
//
//                                QueryResponseParser qrp;
//                                QueryResponse response = null;
//                                try {
//                                    Statement stmt = sqlService.getConnection().createStatement();
//                                    ResultSet res = stmt.executeQuery(semiQuery);
//                                    qrp = new QueryResponseParser();
//                                    ArrayList<Row> rows = new ArrayList<Row>(qrp.parse(res, tb.getTable()).getRows());
//                                    System.out.println(rows);
//                                    response = new QueryResponse(rows);
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                                if (response.getNbRows() == 0) {
//                                    int tmpIndex = semiQuery.lastIndexOf("=") + 1;
//                                    if (chosenChange.getParentTableColumn().getTypeName().equals("varchar")
//                                            || chosenChange.getParentTableColumn().getTypeName().equals("bool")
//                                            || chosenChange.getParentTableColumn().getTypeName().equals("timestamp")
//                                            || chosenChange.getParentTableColumn().getTypeName().equals("date")
//                                            || chosenChange.getParentTableColumn().getTypeName().equals("_text")
//                                            || chosenChange.getParentTableColumn().getTypeName().equals("text")
//                                            || chosenChange.getParentTableColumn().getTypeName().equals("fulltext"))
//                                        semiQuery = semiQuery.substring(0, tmpIndex) + "' " + chosenChange.getOldValue() + "' ";
//                                    else
//                                        semiQuery = semiQuery.substring(0, tmpIndex) + chosenChange.getOldValue();
//
//                                    try {
//                                        Statement stmt = sqlService.getConnection().createStatement();
//                                        ResultSet res = stmt.executeQuery(semiQuery);
//                                        qrp = new QueryResponseParser();
//                                        ArrayList<Row> rows = new ArrayList<Row>(qrp.parse(res, tb.getTable()).getRows());
//                                        response = new QueryResponse(rows);
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                    }
//
//                                    if (response.getRows().size() > 1) {
//                                        FkGenericTreeNode tmp = new FkGenericTreeNode(response.getRows().get(0), this, new SingleChange(tb, this, chosenChange.getOldValue(), chosenChange.getNewValue()), true);
//                                        fkMutations.put(tb, tmp);
//                                        theQuery = theQuery + tmp.updateQueryBuilder(false, db, sqlService) + ";"; // adding semicolon between fk updates inside the transaction
//
//                                    } else if (response.getRows().size() == 1) {
//                                        FkGenericTreeNode tmp = new FkGenericTreeNode(response.getRows().get(0), this, new SingleChange(tb, this, chosenChange.getOldValue(), chosenChange.getNewValue()), false);
//                                        fkMutations.put(tb, tmp);
//                                        theQuery = theQuery + tmp.updateQueryBuilder(false, db, sqlService) + ";"; // adding semicolon between fk updates inside the transaction
//
//                                    }
//                                }
//                        }
//                }
//
//
//
//        theQuery = theQuery + updateQueryBuilder(undo,db,sqlService);
//        theQuery = theQuery + " ; COMMIT TRANSACTION;";
//
//        System.out.println("Total query = "+theQuery);
//        return theQuery;
//    }

    public void transferMutationToParent( Database db,SqlService sqlService)
    {
        TableColumn sgParentColumn = chosenChange.getParentTableColumn();

        QueryResponseParser qrp;
        QueryResponse response = null;

        Collection<ForeignKeyConstraint> lesFk= db.getLesForeignKeys().get(sgParentColumn.getTable().getName().toUpperCase());
        for(ForeignKeyConstraint fk : lesFk)
        {
            if(fk.getChildColumns().contains(sgParentColumn))
            {
                chosenChange.setParentTableColumn(fk.getParentColumns().get(0)); // might require some change if there are multiple parents to one field
            }
        }

        String semiQuery = "SELECT * FROM " + chosenChange.getParentTableColumn().getTable().getName() ;
        if (chosenChange.getParentTableColumn().getTypeName().equals("varchar")
                || chosenChange.getParentTableColumn().getTypeName().equals("bool")
                || chosenChange.getParentTableColumn().getTypeName().equals("timestamp")
                || chosenChange.getParentTableColumn().getTypeName().equals("date")
                || chosenChange.getParentTableColumn().getTypeName().equals("_text")
                || chosenChange.getParentTableColumn().getTypeName().equals("text")
                || chosenChange.getParentTableColumn().getTypeName().equals("fulltext")
                || chosenChange.getParentTableColumn().getTypeName().equals("email"))
            semiQuery = semiQuery + " WHERE " + chosenChange.getParentTableColumn().getName() + "= '"+chosenChange.getOldValue() + " '";
        else
            semiQuery = semiQuery + " WHERE " + chosenChange.getParentTableColumn().getName() + "="+chosenChange.getOldValue();

        try {
            Statement stmt = sqlService.getConnection().createStatement();
            ResultSet res = stmt.executeQuery(semiQuery);
            qrp = new QueryResponseParser();
            ArrayList<Row> rows = new ArrayList<Row>(qrp.parse(res, chosenChange.getParentTableColumn().getTable()).getRows());
            response = new QueryResponse(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }



        initial_state_row = response.getRows().get(0); // there should be only one row.



        semiQuery = "SELECT * FROM " + chosenChange.getParentTableColumn().getTable().getName() ;
        if (chosenChange.getParentTableColumn().getTypeName().equals("varchar")
                || chosenChange.getParentTableColumn().getTypeName().equals("bool")
                || chosenChange.getParentTableColumn().getTypeName().equals("timestamp")
                || chosenChange.getParentTableColumn().getTypeName().equals("date")
                || chosenChange.getParentTableColumn().getTypeName().equals("_text")
                || chosenChange.getParentTableColumn().getTypeName().equals("text")
                || chosenChange.getParentTableColumn().getTypeName().equals("fulltext")
                || chosenChange.getParentTableColumn().getTypeName().equals("email"))
            semiQuery = semiQuery + " WHERE " + chosenChange.getParentTableColumn().getName() + "= '"+chosenChange.getNewValue() + " '";
        else
            semiQuery = semiQuery + " WHERE " + chosenChange.getParentTableColumn().getName() + "="+chosenChange.getNewValue();

        try {
            Statement stmt = sqlService.getConnection().createStatement();
            ResultSet res = stmt.executeQuery(semiQuery);
            qrp = new QueryResponseParser();
            ArrayList<Row> rows = new ArrayList<Row>(qrp.parse(res, chosenChange.getParentTableColumn().getTable()).getRows());
            response = new QueryResponse(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!response.getRows().isEmpty())
        {
            try
            {
                Process fuID = new ProcessBuilder("/bin/bash", "firstUnusedId.sh").start();
                String newValueAsString = getScriptResponse(fuID);
                int newValue = Integer.parseInt(newValueAsString.replaceAll("\\s+",""));
                chosenChange.setNewValue(newValue);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public boolean checkIfHasParentFk(Database db)
    {
        Collection<ForeignKeyConstraint> lesFk= db.getLesForeignKeys().get(chosenChange.getParentTableColumn().getTable().getName().toUpperCase());
        for(ForeignKeyConstraint fk : lesFk)
        {
            if(!fk.getParentColumns().isEmpty())
                return true;
        }
        return false;
    }

    public ArrayList<SingleChange> removePotentialChangesThatDontMatchConstraints(ArrayList<SingleChange> possibilities, SqlService sqlService)
    {
        QueryResponseParser qrp;
        QueryResponse response = null;
        ArrayList<SingleChange> newPossibilities = possibilities;

        for(SingleChange sg : possibilities)
        {
            if(sg.getParentTableColumn().getTable().getPrimaryColumns().contains(sg.getParentTableColumn())) // unique OR PK constraints
            {
                String semiQuery = "SELECT * FROM " + sg.getParentTableColumn().getTable().getName();
                if (sg.getParentTableColumn().getTypeName().equals("varchar")
                        || sg.getParentTableColumn().getTypeName().equals("bool")
                        || sg.getParentTableColumn().getTypeName().equals("timestamp")
                        || sg.getParentTableColumn().getTypeName().equals("date")
                        || sg.getParentTableColumn().getTypeName().equals("_text")
                        || sg.getParentTableColumn().getTypeName().equals("text")
                        || sg.getParentTableColumn().getTypeName().equals("fulltext")
                        || sg.getParentTableColumn().getTypeName().equals("email"))
                    semiQuery = semiQuery + " WHERE " + sg.getParentTableColumn().getName() + "= '" + sg.getNewValue() + " '";
                else
                    semiQuery = semiQuery + " WHERE " + sg.getParentTableColumn().getName() + "=" + sg.getNewValue();

                System.out.println("removing = " + semiQuery);

                try
                {
                    Statement stmt = sqlService.getConnection().createStatement();
                    ResultSet res = stmt.executeQuery(semiQuery);
                    qrp = new QueryResponseParser();
                    ArrayList<Row> rows = new ArrayList<Row>(qrp.parse(res, sg.getParentTableColumn().getTable()).getRows());
                    response = new QueryResponse(rows);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                if(response.getRows() != null)
                    newPossibilities.remove(sg);
            }
        }
        return newPossibilities;
    }

    public String getScriptResponse(Process p)
    {
        String response = "";
        try
        {

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine())!=null) {
                response = response+line;
            }
            r.close();
        }
        catch(Exception e)
        {
            System.out.println("error while reading process output"+e);
        }
        return response;
    }
}
