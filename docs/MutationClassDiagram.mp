input metauml;
beginfig(1);
	Class.Mutation("Mutation")
		("+ID: int",
		"+score: int",
		"+subTreeWeight: int",
		"+depth: int",
		"+initialStateRow: Row",
		"+postChangeRow: Row",
		"+potential_changes: List<SingleChange>",
		"+parent: Mutation",
		"+children: List<Mutation>",
		"+chosenChange: SingleChange",
		"+firstAppearance: boolean",
		"+rpv: ReportVector")
		("+pickPotentialChangeBasedOnWeight(): SingleChange",
		 "+discoverFieldPossibilities(column, value): List<SingleChange>",
		 "+inject(mutation, doOrUndo): int",
		 "+updateQueryBuilder(): String",
		 "+compare(Mutation): Boolean",
		 "+findPathTo(Mutation) : List<List<Mutation>>",
		 "+transferToParent(): void");
	drawObjects(Mutation);	
endfig;
end