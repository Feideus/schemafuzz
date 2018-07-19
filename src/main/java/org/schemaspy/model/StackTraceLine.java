package org.schemaspy.model;

public class StackTraceLine {

    private String functionName;
    private String fileName;
    private int lineNumber;

    public StackTraceLine(String functionName, String fileName, int lineNumber) {
        this.functionName = functionName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public String getfunctionName() {
        return functionName;
    }

    public void setfunctionName(String functionName) {
        this.functionName = functionName;
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

    @Override
    public String toString() {
        return "StackTraceLine{" +
                "functionName='" + functionName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", lineNumber=" + lineNumber +
                '}';
    }

    public boolean compare(StackTraceLine stl)
    {
        if(stl.fileName.equals(this.fileName) && stl.functionName.equals(this.functionName) && stl.lineNumber == this.lineNumber)
            return true;
        return false;
    }
}
