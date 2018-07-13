package org.schemaspy.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReportVector {
    private ArrayList<StackTraceLine> stackTrace;
    private int codeCoverage; //unused right now
    GenericTreeNode parentMutation;

    public ReportVector(GenericTreeNode parentMutation)
    {
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

    public void addStackTraceLine(StackTraceLine stl)
    {
        stackTrace.add(stl);
    }

    public void parseFile(String pathToFile)
    {
        HashMap<String,ArrayList<String>> allLists = new HashMap<>();

        String data;
        String key="uninitialized key.Should not be that way";
        ArrayList<String> currentArray = new ArrayList<>();

        try {
            BufferedReader infile = new BufferedReader(new FileReader(pathToFile));
            while ((data = infile.readLine()) != null) {
                if (data.contains(":")) {
                    if (!currentArray.isEmpty()) {
                        allLists.put(key, currentArray); // putting in the map the "title" of the data and values before stepping into the next block
                        currentArray = new ArrayList<>();
                    }

                    key = data.replace(":", "");
                } else {
                    currentArray.add(data);
                }
            }

            storeLines(allLists);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void storeLines(HashMap<String,ArrayList<String>> allLists)
    {
        int maxSize=0;

        for(Map.Entry<String,ArrayList<String>> entry : allLists.entrySet())
        {
            if(entry.getValue().size() > maxSize)
                maxSize = entry.getValue().size();
        }

        for(int i = 0; i < maxSize ; i ++)
        {
            String functionName = "unknown.this is abnormal behavior";
            String fileName = "unknown.this is abnormal behavior";
            int lineNumber = -1;

            if(i < allLists.get("functionNames").size()) {
                functionName = allLists.get("functionNames").get(i);
            }

            if(i < allLists.get("fileNames").size()) {
                fileName = allLists.get("fileNames").get(i);
            }

            if(i < allLists.get("lineNumbers").size()) {
                try
                {
                    lineNumber = Integer.parseInt(allLists.get("lineNumbers").get(i));
                }
                catch(Exception e) {e.printStackTrace();}
            }

            StackTraceLine stl = new StackTraceLine(functionName,fileName,lineNumber);
            stackTrace.add(stl);
        }


    }
}
