#!/bin/bash

isBinaryInDir=`ls | grep $1`;

if [[ -n "$isBinaryInDir" ]]
then
    echo "chosen binary is : "$1;
else
    echo "couldnt find the binary in the current folder";
    exit 0;
fi

binaryWithoutExtention=`echo $1 | cut -d '.' -f1`
echo "saving result in : "$binaryWithoutExtention;

ulimit -c 9999
echo "--------------------------"
./$1
echo "--------------------------"

checkCoreGen=`ls | grep $1`;

if [[ -n "$checkCoreGen" ]]
then
    mv ./core errorReports/
else
    echo "no core dump generated. Abort";
    exit 0;
fi

echo bt | gdb test_c_crash.exe errorReports/core | sed -e 's/(gdb) //' | grep \# | uniq > errorReports/stackTrace_$binaryWithoutExtention
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

var=`shuf -i 1-10000 -n 1`;
reportFileName=parsedStackTrace_$binaryWithoutExtention_$var
touch $reportFileName

echo "${functionNameArray[@]}" >> errorReports/$reportFileName
echo "${fileNameArray[@]}" >> errorReports/$reportFileName
echo "${lineNumberArray[@]}" >> errorReports/$reportFileName

rm errorReports/core
rm errorReports/stackTrace_$binaryWithoutExtention
