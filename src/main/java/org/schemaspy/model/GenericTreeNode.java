package org.schemaspy.model;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import org.schemaspy.*;
import org.schemaspy.service.SqlService;
import org.springframework.util.SerializationUtils;

public class GenericTreeNode {


    private final Integer id;
    private int interest_mark;
    private int weight;
    private Integer subTreeWeight;
    private int depth;
    private Row initial_state_row;
    private Row post_change_row;
    private ArrayList<SingleChange> potential_changes = new ArrayList<SingleChange>();
    private GenericTreeNode parent;
    private ArrayList<GenericTreeNode> children = new ArrayList<GenericTreeNode>();
    private SingleChange chosenChange;
    private boolean isFirstApperance;
    private HashMap<TableColumn, FkGenericTreeNode> fkMutations = new HashMap<TableColumn, FkGenericTreeNode>();
    private ReportVector rpv;

    /**
     * Default GenericTreeNode constructor
     */
    public GenericTreeNode(Row initial_state_row, int id, SqlService sqlService) { // used only for rootMutation and Tests
        //this.cascadingFK = false;
        this.subTreeWeight = 0;
        this.parent = null;
        this.weight = 1;
        this.depth = 0;
        this.id = id;
        this.isFirstApperance = true;
        this.initial_state_row = initial_state_row;
        this.potential_changes = discoverMutationPossibilities(sqlService);
    }


    public GenericTreeNode(Row initial_state_row, int id, GenericTreeNode rootMutation, GenericTreeNode parentMutation, boolean isFirstApperance, SqlService sqlService) {
        //this.cascadingFK = false;
        this.parent = parentMutation;
        this.subTreeWeight = 0;
        this.weight = 1;
        this.id = id;
        initDepth();
        this.isFirstApperance = isFirstApperance;
        this.initial_state_row = initial_state_row;
        this.potential_changes = discoverMutationPossibilities(sqlService);
    }

    public GenericTreeNode( GenericTreeNode parentMutation) {
        //this.cascadingFK = false;
        this.parent = parentMutation;
        id=new Random().nextInt(Integer.MAX_VALUE);
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

    public void setSubTreeWeight(int subTreeWeight) {
        this.subTreeWeight = subTreeWeight;
    }

    public ReportVector getReportVector() { return rpv; }

    public void setReportVector(ReportVector rpv) { this.rpv = rpv; }

    public void setPotential_changes(ArrayList<SingleChange> potCh) //used in tests
    {
        this.potential_changes = potCh;
    }

    public int getWeight() {
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
    public SingleChange singleChangeBasedOnWeight() {
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
            double w = n.getWeight();
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
        this.chosenChange.getAttachedToMutation().getPotential_changes().remove(chosenChange);
        this.chosenChange.setAttachedToMutation(this);

        assert(sc.getAttachedToMutation().getId().equals(this.getId()));
    }

    /**
     * Sets new value of children
     *
     * @param
     */

    public void setParent(GenericTreeNode parent) {
        this.parent = parent;
    }

    public ArrayList<SingleChange> discoverMutationPossibilities( SqlService sqlService) {

        ArrayList<SingleChange> possibilities = new ArrayList<SingleChange>();

        //TRYING TO DISCOVER RAW POSSIBILITIES
        for (Map.Entry<String, Object> content : initial_state_row.getContent().entrySet()) {
            try {
                TableColumn parentColumn = initial_state_row.getParentTable().findTableColumn(content.getKey());
                possibilities.addAll(discoverFieldPossibilities(parentColumn, content.getValue()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        possibilities = removePotentialChangesThatDontMatchConstraints(possibilities,sqlService);
        if (possibilities.isEmpty())
            System.out.println("No raw Mutation could be found for this row");

        return possibilities;
    }

    public ArrayList<SingleChange> discoverFieldPossibilities(TableColumn tableColumn, Object column_value) throws Exception //listing of the mutation possibilities on the specified row
    {

        ArrayList<SingleChange> oneChange = new ArrayList<SingleChange>();

        String typeName = tableColumn.getTypeName();
        GenericTreeNode rootForThisMutation = FirstApperanceOf(this);

        switch (typeName) {
            case "smallint":
            case "integer":
            case "int2":
            case "int8":
            case "serial":
            case "bigserial":
                Object tmp = rootForThisMutation.getInitial_state_row().getContent().get(tableColumn.getName());
                Random rand = new Random();
                if (tmp != null && tmp.toString() != "") {
                    int tmp2;
                    if (typeName.equals("int2") || typeName.equals("serial")) {
                        tmp2 = Integer.parseInt(rootForThisMutation.getInitial_state_row().getContent().get(tableColumn.getName()).toString());
                        oneChange.add(new SingleChange(tableColumn, this, column_value, Integer.toString(tmp2++)));
                        oneChange.add(new SingleChange(tableColumn, this, column_value, Integer.toString(32767)));
                        oneChange.add(new SingleChange(tableColumn, this, column_value, rand.nextInt(32767)));
                    } else if (typeName.equals("int8") || typeName.equals("bigserial")) {
                        BigInteger bigInt = new BigInteger(rootForThisMutation.getInitial_state_row().getContent().get(tableColumn.getName()).toString());
                        bigInt = bigInt.add(new BigInteger("1"));
                        oneChange.add(new SingleChange(tableColumn, this, column_value, bigInt));
                        oneChange.add(new SingleChange(tableColumn, this, column_value, new BigInteger("9223372036854775806")));
                        oneChange.add(new SingleChange(tableColumn, this, column_value, nextRandomBigInteger(new BigInteger("9223372036854775806"))));
                    }
                    oneChange.add(new SingleChange(tableColumn, this, column_value, Integer.toString(0)));
                    break;
                }

            case "character":
            case "character varying":
                //case "bytea":
            case "varchar":
                tmp = rootForThisMutation.getInitial_state_row().getContent().get(tableColumn.getName());
                if (typeName.equals("bytea")) {
                    byte[] bytes = SerializationUtils.serialize(tmp);
                    tmp = Arrays.toString(bytes);
                }

                if (tmp != null && !tmp.toString().isEmpty() && tmp.toString().length() >0)
                {
                    String tmp2 = tmp.toString();
                    Random rand3 = new Random();
                    int tmpRand = rand3.nextInt(tmp2.length());
                    char nextChar = (char) (tmp2.charAt(tmpRand) + 1);
                    char prevChar = (char) (tmp2.charAt(tmpRand) - 1);
                    oneChange.add(new SingleChange(tableColumn, this, column_value, column_value.toString().substring(0,tmpRand)+nextChar+column_value.toString().substring(tmpRand+1)));
                    oneChange.add(new SingleChange(tableColumn, this, column_value, column_value.toString().substring(0,tmpRand)+prevChar+column_value.toString().substring(tmpRand+1)));
                }
                break;

            case "bool":
                if (column_value.equals("f"))
                    oneChange.add(new SingleChange(tableColumn, this, column_value, "t"));
                if (column_value.equals("t"))
                    oneChange.add(new SingleChange(tableColumn, this, column_value, "f"));
                break;

            case "text":
                tmp = rootForThisMutation.getInitial_state_row().getContent().get(tableColumn.getName());
                if (tmp != null && tmp.toString() != "")
                {
                    Random rand2 = new Random();
                    int randNum = rand2.nextInt(tmp.toString().length());
                    char tmp3 = tmp.toString().charAt(randNum);

                    oneChange.add(new SingleChange(tableColumn, this, column_value, tmp.toString().substring(0, randNum) + (Character.toString(tmp3++)) + tmp.toString().substring(randNum + 1)));
                    oneChange.add(new SingleChange(tableColumn, this, column_value, tmp.toString().substring(0, randNum) + (Character.toString(tmp3--)) + tmp.toString().substring(randNum + 1)));
                    break;
                }


            /*case "bytea":
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

              case 6:  int8 = "June";
                       break;
              case 7:  bigSerial = "July";
                       break;
              case 8:  text = "August";
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
                System.out.println("Unsupported dataType = " + typeName);
        }
        return oneChange;
    }

    public int inject(SqlService sqlService, Database db,GenericTree mutationTree, boolean undo)
    {
            boolean transfered = false;
            String theQuery = "";

            if (undo)
                System.out.println("UNDOING");
            else
                System.out.println("INJECT");

            if (checkIfHasParentFk(db)) {
                transfered = true;
                System.out.println("TRANSFERT");
                transferMutationToParent(db, sqlService);
            }

            theQuery = updateQueryBuilder(undo, db, sqlService);
            try
            {
                Statement stmt = sqlService.getConnection().createStatement();
                int nbUpdates = stmt.executeUpdate(theQuery);
                if(nbUpdates > 0)
                    //handleCascadeUpdate(this,db,mutationTree);
                if(transfered && nbUpdates > 0)
                    return -1;

                return nbUpdates;
            }
            catch (Exception e)
            {
                if(!e.getMessage().contains("unique constraint")) {
                    e.printStackTrace(); // TransfertToMutation Modifies the tree and provoques the happenning of 2 do's on one single mutation during undoToMutation.
                    return 0;
                }
                else {
                    System.out.println("Value already in use");
                    return 0;
                }
            }

    }


    public void initPostChangeRow() {
        this.post_change_row = this.initial_state_row.myClone();
        this.post_change_row.setValueOfColumn(chosenChange.getParentTableColumn().getName(), chosenChange.getNewValue());
        if(!post_change_row.getValueOfColumn(chosenChange.getParentTableColumn().getName()).equals(chosenChange.getNewValue()))
            System.out.println("problem");
    }

    public int undo(SqlService sqlService, Database db,GenericTree mutationTree) {
        try {
            return this.inject(sqlService, db,mutationTree, true);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String updateQueryBuilder(boolean undo, Database db, SqlService sqlService) //undo variable tells if the function should build Inject string or Undo string
    {

        String theQuery;

        if (undo) {
            if (requireQuotes(chosenChange.getParentTableColumn()) == 1)
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + "='" + chosenChange.getOldValue().toString() + "', ";
            else
                theQuery = "UPDATE " + initial_state_row.getParentTable().getName() + " SET " + chosenChange.getParentTableColumn().getName() + " = " + chosenChange.getOldValue().toString() + ", ";
        } else {
            if (requireQuotes(chosenChange.getParentTableColumn()) == 1)
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
                if (chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()) != null && !isUnHandledType(chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey()).getTypeName()))
                {
                    if (requireQuotes(chosenChange.getParentTableColumn().getTable().getColumn(entry.getKey())) == 1)
                    {
                        if (entry.getValue() != null)
                            theQuery = theQuery + (entry.getKey() + "='" + entry.getValue().toString() + "' AND ");
                        else
                            theQuery = theQuery + (entry.getKey() + " IS NULL AND ");
                    }
                    else
                    {
                        if (entry.getValue() != null)
                            theQuery = theQuery + (entry.getKey() + "=" + entry.getValue().toString() + " AND ");
                        else
                            theQuery = theQuery + (entry.getKey() + " IS NULL AND ");
                    }
                }

            } else {
                if (undo)
                    theQuery = theQuery + (entry.getKey() + "='" + post_change_row.getContent().get(entry.getKey()) + "' AND ");
                else
                    theQuery = theQuery + (entry.getKey() + "='" + initial_state_row.getContent().get(entry.getKey()) + "' AND ");
            }
        }
        try {
            theQuery = theQuery.substring(0, theQuery.lastIndexOf(" AND "));
        } catch (Exception e) {

        }
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


    public boolean compare(GenericTreeNode genericTreeNode) {
        if (this == null || genericTreeNode == null)
            return false;

        if (this.getInitial_state_row() == null || genericTreeNode.getInitial_state_row() == null)
            return false;

        if (this.getId() == genericTreeNode.getId()) {
            return true;
        }

        if (this.initial_state_row.compare(genericTreeNode.getInitial_state_row()) && this.chosenChange.compare(genericTreeNode.getChosenChange()))
            return true;

        return false;
    }

    public boolean undoToMutation(GenericTreeNode target, SchemaAnalyzer analyzer,GenericTree mutationTree) {
        ArrayList<GenericTreeNode> goingUp = findPathToMutation(target).get(0);
        ArrayList<GenericTreeNode> goingDown = findPathToMutation(target).get(1);

        try
        {
            for (GenericTreeNode node : goingUp) {
                if (node.undo(analyzer.getSqlService(), analyzer.getDb(),mutationTree) > 0)
                    System.out.println("success undoing :" + node.getId());
            }

            for (GenericTreeNode node : goingDown) {
                if (node.inject(analyzer.getSqlService(), analyzer.getDb(),mutationTree, false) > 0)
                    System.out.println("success doing :" + node.getId());

            }
        }

        catch(Exception e)
        {
            System.out.println(e.toString());
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

    public void setChildren(ArrayList<GenericTreeNode> children) {
        this.children = children;
    }

    public void addChild(GenericTreeNode child) {
        child.parent = this;
        children.add(child);
    }

    public String toString() {
        return "[ MUT ID " + this.getId() + " Depth = " + this.getDepth() + " SG " + this.chosenChange + "]";
    }

    public ArrayList<ArrayList<GenericTreeNode>> findPathToMutation(GenericTreeNode target) {
        ArrayList<ArrayList<GenericTreeNode>> finalPath = new ArrayList<ArrayList<GenericTreeNode>>();
        ArrayList<GenericTreeNode> thisPath = new ArrayList<GenericTreeNode>();
        ArrayList<GenericTreeNode> targetPath = new ArrayList<GenericTreeNode>();

        GenericTreeNode tmpTarget = target;
        GenericTreeNode tmpThis = this;

        int depthOffset = -1;

        while (depthOffset != 0) {
            depthOffset = tmpThis.getDepth() - tmpTarget.getDepth();
            if (depthOffset > 0) {
                thisPath.add(tmpThis);
                tmpThis = tmpThis.getParent();
            } else if (depthOffset < 0) {
                targetPath.add(tmpTarget);
                tmpTarget = tmpTarget.getParent();
            }
        }

        while (!tmpThis.compare(tmpTarget)) {
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

    public boolean isSingleChangeOnCurrentPath(GenericTreeNode rootMutation) {
        ArrayList<GenericTreeNode> finalPath = new ArrayList<GenericTreeNode>();
        finalPath.addAll(this.findPathToMutation(rootMutation).get(0));
        finalPath.addAll(this.findPathToMutation(rootMutation).get(1));
        finalPath.remove(this);

        for (GenericTreeNode mutOnPath : finalPath) {
            if (mutOnPath.getChosenChange().compare(this.getChosenChange()))
                return true;
        }
        return false;
    }


    public void propagateWeight() {
        this.updateSubTreeWeight();

        if (this.getParent() != null)
            this.getParent().propagateWeight();
    }

    public GenericTreeNode FirstApperanceOf(GenericTreeNode mutation) {
        if (mutation.getIsFirstApperance())
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

    public void transferMutationToParent(Database db, SqlService sqlService) {
        TableColumn sgParentColumn = chosenChange.getParentTableColumn();

        Collection<ForeignKeyConstraint> lesFk = db.getLesForeignKeys().get(sgParentColumn.getTable().getName().toUpperCase());
        for (ForeignKeyConstraint fk : lesFk) {
            if (fk.getChildColumns().contains(sgParentColumn)) {
                chosenChange.setParentTableColumn(fk.getParentColumns().get(0)); // might require some change if there are multiple parents to one field
            }
        }

        String semiQuery = "SELECT * FROM " + chosenChange.getParentTableColumn().getTable().getName();
        if (requireQuotes(chosenChange.getParentTableColumn()) == 1)
            semiQuery = semiQuery + " WHERE " + chosenChange.getParentTableColumn().getName() + "= '" + chosenChange.getOldValue() + " '";
        else
            semiQuery = semiQuery + " WHERE " + chosenChange.getParentTableColumn().getName() + "=" + chosenChange.getOldValue();

        System.out.println(semiQuery);

        QueryResponse response = fetchingDataFromDatabase(semiQuery, chosenChange.getParentTableColumn().getTable(), sqlService);


        semiQuery = "SELECT * FROM " + chosenChange.getParentTableColumn().getTable().getName();

        setInitial_state_row(response.getRows().get(0),sqlService); // Crashes sometimes due to 0 row found. to be fixed. Update: doesnt SEEM to crash amymore.

        if (requireQuotes(chosenChange.getParentTableColumn()) == 1) {
            semiQuery = semiQuery + " WHERE " + chosenChange.getParentTableColumn().getName() + "= '" + chosenChange.getNewValue() + " '";
        }
        else {
            semiQuery = semiQuery + " WHERE " + chosenChange.getParentTableColumn().getName() + "=" + chosenChange.getNewValue();
        }

        response = fetchingDataFromDatabase(semiQuery, chosenChange.getParentTableColumn().getTable(), sqlService);

        if (!response.getRows().isEmpty()) {
            handleAlreadySetValue(sqlService);
        }
    }


    public void setInitial_state_row(Row initial_state_row,SqlService sqlService) {
        this.initial_state_row = initial_state_row;
        this.potential_changes = null;
        this.potential_changes = discoverMutationPossibilities(sqlService);
        initPostChangeRow();
    }

    public boolean checkIfHasParentFk(Database db) {
        Collection<ForeignKeyConstraint> lesFk = db.getLesForeignKeys().get(chosenChange.getParentTableColumn().getTable().getName().toUpperCase());
        for (ForeignKeyConstraint fk : lesFk) {
            if (fk.getChildColumns().contains(chosenChange.getParentTableColumn()) && !fk.getParentColumns().isEmpty())
                return true;
        }
        return false;
    }

    public ArrayList<SingleChange> removePotentialChangesThatDontMatchConstraints(ArrayList<SingleChange> possibilities, SqlService sqlService) {
        ArrayList<SingleChange> toBeRemoved = new ArrayList<SingleChange>();

        for (SingleChange sg : possibilities) {
            if (sg.getParentTableColumn().getTable().getPrimaryColumns().contains(sg.getParentTableColumn())) // unique OR PK constraints
            {
                String semiQuery = "SELECT * FROM " + sg.getParentTableColumn().getTable().getName();
                if (requireQuotes(sg.getParentTableColumn()) == 1)
                    semiQuery = semiQuery + " WHERE " + sg.getParentTableColumn().getName() + "=' " + sg.getNewValue() + " '";
                else
                    semiQuery = semiQuery + " WHERE " + sg.getParentTableColumn().getName() + "=" + sg.getNewValue();

                QueryResponse response = fetchingDataFromDatabase(semiQuery, sg.getParentTableColumn().getTable(), sqlService);

                SingleChange tmp = sg;
                if (response.getNbRows() > 0)
                {
                    System.out.println("removing "+sg);
                    toBeRemoved.add(tmp);
                }
            }
        }
        possibilities.removeAll(toBeRemoved);
        return possibilities;
    }

    public int requireQuotes(TableColumn column) // checks if column is of "Stringish" (needs sql quotes) as 1 or "integerish" as 0. more typeishes can be added in the future. existing lists can be edited
    {
        if (column.getTypeName().equals("varchar")
                || column.getTypeName().equals("bool")
                || column.getTypeName().equals("timestamp")
                || column.getTypeName().equals("date")
                || column.getTypeName().equals("_text")
                || column.getTypeName().equals("text")
                || column.getTypeName().equals("email")
                || column.getTypeName().equals("bytea")
                || column.getTypeName().equals("bpchar"))
            return 1;
        else
            return 0;
    }

    public QueryResponse fetchingDataFromDatabase(String semiQuery, Table parentTable, SqlService sqlService) {
        QueryResponseParser qrp;
        QueryResponse response = null;
        try {
            Statement stmt = sqlService.getConnection().createStatement();
            ResultSet res = stmt.executeQuery(semiQuery);
            qrp = new QueryResponseParser();
            ArrayList<Row> rows = new ArrayList<Row>(qrp.parse(res, parentTable).getRows());
            response = new QueryResponse(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public boolean isUnHandledType(String typeName)
    {
        if(typeName.equals("tsvector")
                || typeName.equals("timestamp")
                || typeName.equals("mpaa-rating"))
            return true;
        return false;
    }

    public void updatePotentialChangeAfterInjection()
    {
        for(SingleChange sg : potential_changes)
        {
            if(sg.getOldValue().equals(initial_state_row.getValueOfColumn(sg.getParentTableColumn().getName())))
            {
                sg.setOldValue(post_change_row.getValueOfColumn(sg.getParentTableColumn().getName()));
            }
        }
    }

    public void handleAlreadySetValue(SqlService sqlService)
    {
        String columnName = chosenChange.getParentTableColumn().getName();
        String tableName = chosenChange.getParentTableColumn().getTable().getName();

        String semiQuery = "SELECT * FROM ( SELECT  1 AS " + columnName + " ) q1 WHERE NOT EXISTS ( SELECT  1 FROM " + tableName + " WHERE   " + columnName + " = 1) " +
                "UNION ALL SELECT  * FROM    ( SELECT  " + columnName + " + 1 FROM " + tableName + " t WHERE NOT EXISTS ( SELECT  1 FROM " + tableName + " ti WHERE ti." + columnName + " = t." + columnName + " + 1) " +
                "ORDER BY " + columnName + " LIMIT 1) q2 LIMIT 1";

        QueryResponse response = fetchingDataFromDatabase(semiQuery, chosenChange.getParentTableColumn().getTable(), sqlService);

        chosenChange.setNewValue(response.getRows().get(0).getValueOfColumn(columnName));
        initPostChangeRow();
    }

    public void handleCascadeUpdate(GenericTreeNode currentMutation, Database db,GenericTree mutationTree)
    {
        SingleChange sgCurrMut = currentMutation.getChosenChange();
        Map<String,Collection<ForeignKeyConstraint>> lesFk = db.getLesForeignKeys();
        List<TableColumn> cascadedColumns = new ArrayList<>();

        for (Map.Entry<String,Collection<ForeignKeyConstraint>> fkCol: lesFk.entrySet())
        {
            for(ForeignKeyConstraint fk : fkCol.getValue())
            {
                if (fk.getParentColumns().contains(sgCurrMut.getParentTableColumn()))
                {
                    cascadedColumns = fk.getChildColumns();
                }
            }
        }

        if(cascadedColumns.isEmpty())
            return;

        ArrayList<GenericTreeNode> treeAsArray = mutationTree.toArray();
        treeAsArray.remove(this);
        for(GenericTreeNode gtn : treeAsArray)
        {
            for(TableColumn tb : cascadedColumns)
            {
                Object objectToBeChanged = gtn.getInitial_state_row().getContent().get(tb.getName());
                if(objectToBeChanged != null && objectToBeChanged.toString().equals(sgCurrMut.getOldValue()))
                    gtn.getInitial_state_row().setValueOfColumn(tb.getName(),sgCurrMut.getNewValue());
            }
        }
    }

    public BigInteger nextRandomBigInteger(BigInteger n) {
        Random rand = new Random();
        BigInteger result = new BigInteger(n.bitLength(), rand);
        while( result.compareTo(n) >= 0 ) {
            result = new BigInteger(n.bitLength(), rand);
        }
        return result;
    }

    public ArrayList<GenericTreeNode> pathToRoot()
    {
        ArrayList<GenericTreeNode> res = new ArrayList<GenericTreeNode>();
        GenericTreeNode tmp = this;
        do{
            res.add(tmp);
            tmp = tmp.getParent();

        }while(tmp != null);
        return res;
    }

    public static GenericTreeNode parseInit(String line, SchemaAnalyzer analyzer,ArrayList<GenericTreeNode> mutationsOnPath) throws Exception
    {
        String buffer = "";
        buffer = line.substring(line.indexOf("attachedToMutation : "),line.indexOf("|"));
        GenericTreeNode parentMutation = null;

        for(GenericTreeNode tmp : mutationsOnPath)
        {
            if(tmp.getId().equals(Integer.parseInt(buffer)))
                parentMutation = tmp;
        }
        if(parentMutation == null && mutationsOnPath.size() != 0)
            throw new Exception("ParentMutation not found during parsing");


        line = line.substring(line.indexOf(buffer));

        buffer = line.substring(line.indexOf("parentTable : "),line.indexOf("|"));
        Table parentTable = analyzer.getDb().getTablesByName().get(buffer);


        if(parentTable == null)
            throw new Exception("ParentTable not found during parsing");

        line.substring(line.indexOf(buffer));

        buffer = line.substring(line.indexOf("parentTableColumn : "),line.indexOf("|"));
        TableColumn parentTableColumn = analyzer.getDb().getTablesByName().get(parentTable.getName()).getColumn(buffer);

        if(parentTableColumn == null)
            throw new Exception("ParentTableColumn not found during parsing");

        line.substring(line.indexOf(buffer));

        buffer = line.substring(line.indexOf("OV : "),line.indexOf("|"));
        line.substring(line.indexOf(buffer));
        String buffer2 =  line.substring(line.indexOf("NV : "),line.indexOf("|"));

        GenericTreeNode res = new GenericTreeNode(parentMutation);
        SingleChange sg = new SingleChange(parentTableColumn,res,buffer,buffer2);
        res.setChosenChange(sg);


        if(res == null || res.getChosenChange() == null)
            throw new Exception("SingleChange couldnt be parsed");

        return res;
    }
}
