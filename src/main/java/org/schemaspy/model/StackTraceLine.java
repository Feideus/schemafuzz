package org.schemaspy.model;

import java.util.ArrayList;

public class StackTraceLine {

    private String functionName;
    private double functionNameHash;
    private String fileName;
    private double fileNameHash;
    private int lineNumber;

    public StackTraceLine(String functionName, String fileName, int lineNumber) {
        this.functionName = functionName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public String getFunctionName() { return functionName; }

    public void setFunctionName(String functionName) { this.functionName = functionName; }

    public double getFunctionNameHash() { return functionNameHash; }

    public void setFunctionNameHash(double functionNameHash) { this.functionNameHash = functionNameHash; }

    public double getFileNameHash() { return fileNameHash; }

    public void setFileNameHash(double fileNameHash) { this.fileNameHash = fileNameHash; }

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

    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }

    public static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    public double consistentFunctionNameHash(GenericTree mutationTree,GenericTreeNode currentMutation)
    {
        double maxSimilarity = 0.0;
        StackTraceLine closestStl = null;
        ArrayList<GenericTreeNode> treeAsArray = mutationTree.toArray();
        treeAsArray.remove(currentMutation); // remove the currentMutation so that the loop doesnt try to get the being-built rpv
        for(GenericTreeNode gtn: treeAsArray)
        {
            for(StackTraceLine stl : gtn.getReportVector().getStackTrace())
            {
                double currentSimilarity = similarity(functionName,stl.getFunctionName());
                if(currentSimilarity > maxSimilarity)
                {
                    maxSimilarity = currentSimilarity;
                    closestStl = stl;
                }
            }
        }

        if(maxSimilarity == 0.0 || closestStl == null)
            return maxSimilarity;
        else
        {
            return closestStl.getFunctionNameHash() / maxSimilarity;
        }
    }

    public double consistentFileNameHash(GenericTree mutationTree,GenericTreeNode currentMutation)
    {
        double maxSimilarity = 0.0;
        StackTraceLine closestStl = null;
        ArrayList<GenericTreeNode> treeAsArray = mutationTree.toArray();
        treeAsArray.remove(currentMutation);
        for(GenericTreeNode gtn: treeAsArray)
        {
            for(StackTraceLine stl : gtn.getReportVector().getStackTrace())
            {
                double currentSimilarity = similarity(fileName,stl.getFileName());
                if(currentSimilarity > maxSimilarity)
                {
                    maxSimilarity = currentSimilarity;
                    closestStl = stl;
                }
            }
        }

        if(maxSimilarity == 0.0 || closestStl == null)
            return maxSimilarity;
        else
        {
            return closestStl.getFunctionNameHash() / maxSimilarity;
        }
    }
}
