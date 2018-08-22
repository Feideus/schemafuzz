input metauml;
beginfig(1);
	Class.Table("Table")
		("+name: String",
		"+primaryKey: ForeignKeyConstraint")();
	Class.ForeignKeyConstraint("ForeignKeyConstraint")
		("+name: String",
		"+parentField: ForeignKeyConstraint",
		"+childFields: List<ForeignKeyConstraint>",
		"+updateRule: int")();
	Class.Column("Column")
		("+name: String",
		"+type: String",
		"+length: int",
		"+unique: boolean")();	
	Class.View("View")
		("+name: String",
		"+result: ResultSet")();
	Class.CheckConstraint("CheckConstraint")
	("+name: String",
	"+targetField: ForeignKeyConstraint",
	"+valueColumn: ForeignKeyConstraint",
	"+constantValue: Object")();	
		
	topToBottom(50)(Table,View);	
	leftToRight(50)(Table, ForeignKeyConstraint);
	topToBottom(50)(Column,ForeignKeyConstraint);
	topToBottom(50)(ForeignKeyConstraint,CheckConstraint);	
	drawObjects(Table,ForeignKeyConstraint,View,CheckConstraint,Column);

		
    link(association)(Table.e -- ForeignKeyConstraint.w);
    item(iAssoc)("1")(obj.sw = Table.e);
    item(iAssoc)("n")(obj.se = ForeignKeyConstraint.w);
	
	
    link(association)(Table.n -- Column.w);
    item(iAssoc)("1")(obj.s = Table.n);
    item(iAssoc)("n")(obj.e = Column.w);	
	
    link(association)(Table.s -- CheckConstraint.w);
    item(iAssoc)("1")(obj.n = Table.s);
    item(iAssoc)("n")(obj.ne = CheckConstraint.w);

		
	
endfig;
end