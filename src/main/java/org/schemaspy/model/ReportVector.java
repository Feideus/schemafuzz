package org.schemaspy.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReportVector {
    private ArrayList<StackTraceLine> stackTrace;
    private double[] stackTraceHash;
    private int codeCoverage; //unused right now
    GenericTreeNode parentMutation;

    public ReportVector(GenericTreeNode parentMutation)
    {
        this.parentMutation = parentMutation;
        stackTrace = new ArrayList<StackTraceLine>();
    }

    public double[] getStackTraceHash() { return stackTraceHash; }

    public void setStackTraceHash(double[] stackTraceHash) { this.stackTraceHash = stackTraceHash; }

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
                    currentArray.add(data.replace(",",""));
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

    @Override
    public String toString() {
        return "ReportVector{" +
                "stackTrace=" + stackTrace +
                ", parentMutation=" + parentMutation +
                '}';
    }

    public boolean compareStackTrace (ReportVector rpv)
    {
        if(rpv.stackTrace.size() != this.stackTrace.size())
            return false;

        int i = 0;
        for(StackTraceLine stl : rpv.stackTrace)
        {
            if(!stl.compare(this.stackTrace.get(i)))
                return false;
            
            i++;
        }
        return true;
    }

    public double[] hashStackTrace(GenericTree mutationTree)
    {
        ArrayList<double[]> stackTraceLinesHashes = new ArrayList<>();
        double var1=500,var2=500,var3=500;

        if(parentMutation.getId() == 1)
        {
            for(StackTraceLine stl :stackTrace)
            {
                stl.setFileNameHash(500);
                stl.setFunctionNameHash(500);
            }
            double [] tmp = {500,500,500};
            return tmp;
        }

        for(StackTraceLine stl : this.getStackTrace())
        {
            var1 = stl.consistentFunctionNameHash(mutationTree);
            stl.setFunctionNameHash(var1);
            var2 = stl.consistentFileNameHash(mutationTree);
            stl.setFileNameHash(var2);
            var3 = stl.getLineNumber();


            double[] lineHash = {var1,var2,var3};
            stackTraceLinesHashes.add(lineHash);
        }

        double functionNameHashTotal=0,fileNameHashTotal=0,lineNumberHashTotal=0;
        for(double[] lineHash : stackTraceLinesHashes)
        {
            functionNameHashTotal = functionNameHashTotal + lineHash[0];
            fileNameHashTotal= fileNameHashTotal+ lineHash[1];
            lineNumberHashTotal= lineNumberHashTotal+ lineHash[2];
        }

        int denominator = stackTraceLinesHashes.size();
        double[] stackTraceHash ={functionNameHashTotal/denominator,fileNameHashTotal/denominator,lineNumberHashTotal/denominator};
        return stackTraceHash;
        
    }


}
