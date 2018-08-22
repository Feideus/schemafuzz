input metauml;
beginfig(1);
    Class.ForeignKeyConstraint("ForeignKeyConstraint")
           ("+name: String",
           	"+parent: Table",
           	"+parentColumns: List<TableColumns>",
           	"+childTable: Table",
           	"+deleteRule: int",
           	"+updateRule: int")
           ("+CompareTo(ForeignKeyConstraint): Boolean");

    drawObjects(ForeignKeyConstraint);
endfig;
end