package org.schemaspy.model;

import java.util.ArrayList;

public class ReportVector {
    private ArrayList<StackTraceLine> stackTrace;
    private int codeCoverage; //unused right now
    GenericTreeNode parentMutation;

    public ReportVector(ArrayList<StackTraceLine> stackTrace, int codeCoverage, GenericTreeNode parentMutation) {
        this.stackTrace = stackTrace;
        this.codeCoverage = codeCoverage;
        this.parentMutation = parentMutation;
    }

    public ArrayList<StackTraceLine> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(ArrayList<StackTraceLine> stackTrace) {
        this.stackTrace = stackTrace;
    }

    public int getCodeCoverage() {
        return codeCoverage;
    }

    public void setCodeCoverage(int codeCoverage) {
        this.codeCoverage = codeCoverage;
    }

    public GenericTreeNode getParentMutation() {
        return parentMutation;
    }

    public void setParentMutation(GenericTreeNode parentMutation) {
        this.parentMutation = parentMutation;
    }
}
