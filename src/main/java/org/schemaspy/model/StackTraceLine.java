package org.schemaspy.model;

public class StackTraceLine {

    private String functionname;
    private String fileName;
    private int lineNumber;

    public StackTraceLine(String functionname, String fileName, int lineNumber) {
        this.functionname = functionname;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public String getFunctionname() {
        return functionname;
    }

    public void setFunctionname(String functionname) {
        this.functionname = functionname;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
