#!/bin/bash

#isBinaryInDir=`ls | grep $1`;
#echo $isBinaryInDir

#if [[ -n "$isBinaryInDir" ]]
#then
 #   echo "chosen binary is : "$1;
#else
 #   echo "couldnt find the binary in the current folder";
  #  exit 0;
#fi

binaryWithoutExtention=`echo $1 | cut -d '.' -f1`
echo "saving result in : "$binaryWithoutExtention;

ulimit -c 9999
echo "--------------------------"
./$1 ## -c $3  -c SHOULD BE IN THE JAVA CODE AS A NESTED ARGUMENT. This is for manual testing
echo "--------------------------"

checkCoreGen=`ls | grep core`;

if [[ -n "$checkCoreGen" ]]
then
    mv ./core errorReports/
else
    echo "no core dump generated. Abort";
    echo "0" >> errorReports/parsedStackTrace_$2
    exit 0;
fi

echo bt | gdb $1 errorReports/core | sed -e 's/(gdb) //' | grep \# | uniq > errorReports/stackTrace_$binaryWithoutExtention
tmp=`cat errorReports/stackTrace_$binaryWithoutExtention | sed 's/.* in //' | cut -d "(" -f1`

echo "function names : "$tmp
functionNameArray=(${tmp// / })

tmp2=`cat errorReports/stackTrace_$binaryWithoutExtention | sed 's/.* at //' | cut -d "(" -f1 | cut -d "#" -f1`
fileNameArray=(${tmp2// / })
lineNumberArray=(${tmp2// / })

cpt=0
for i in "${fileNameArray[@]}"
do
   loopVar=$i
   fileNameArray[cpt]="${loopVar%:*}"
   cpt=$cpt+1
done
echo "file names : " "${fileNameArray[@]}"

cpt=0
for i in "${lineNumberArray[@]}"
do
   loopVar=$i
   lineNumberArray[cpt]=`echo $loopVar | sed 's/.*://'`
   cpt=$cpt+1
done
echo "line numbers : " "${lineNumberArray[@]}"

resultLS=`ls errorReports | grep parsedStackTrace_$2`

if [[ ! -z $resultLS ]]
then
    fileExists=1
    while [ $fileExists -eq 1 ]
    do
        var=`shuf -i 1-10000 -n 1`;
        resultLS=`ls errorReports | grep parsedStackTrace_$var`
        echo $resultLS

        if [[ -n "$resultLS" ]]
        then
            fileExists=1
        else
            fileExists=0
        fi
    done
else
    var=$2
fi

reportFileName=parsedStackTrace_$var
echo $reportFileName

checkFileExists=`ls errorReports | grep $reportFileName`;

if [[ -n "$checkFileExists" ]]
then
    rm errorReports/$reportFileName
fi

touch errorReports/$reportFileName

echo "functionNames:" >> errorReports/$reportFileName
cpt=0
for j in "${functionNameArray[@]}"
do
    echo $j"," >> errorReports/$reportFileName
done

echo "fileNames:" >> errorReports/$reportFileName
cpt=0
for j in "${fileNameArray[@]}"
do
    echo $j"," >> errorReports/$reportFileName
done

echo "lineNumbers:" >> errorReports/$reportFileName
cpt=0
for j in "${lineNumberArray[@]}"
do
    echo $j >> errorReports/$reportFileName
done

echo "end:" >> errorReports/$reportFileName


echo "path:" >> errorReports/$reportFileName
compteur=1;
tmpString="mut_"$compteur
boolean=1

while [ $boolean -eq 1 ]
do
        echo ${!tmpString} >> errorReports/$reportFileName
        compteur=$((compteur+1))
        tmpString="mut_"$compteur
        tmpString2=${!tmpString}
        if [[ ! -n "$tmpString2" ]]
        then
            boolean=0
        fi
done

echo "endpath:" >> errorReports/$reportFileName

rm errorReports/core
rm errorReports/stackTrace_$binaryWithoutExtention

