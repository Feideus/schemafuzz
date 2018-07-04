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
mv ./core errorReports/

res=`echo bt | gdb test_c_crash.exe errorReports/core | sed -e 's/(gdb) //' | grep \# | uniq > errorReports/stackTrace_$binaryWithoutExtention `
RESULT=`cat errorReports/stackTrace_$binaryWithoutExtention | sed 's/.* in //' | cut -d "(" -f1`

i=0
echo $RESULT |
while IFS= read -r line;
do
    array[i]=`echo $line`
    i=i+1
done

echo "${array[0]}"
echo "${array[1]}"
echo "${array[2]}"
#echo $res;
#cat errorReports/stackTrace_$binaryWithoutExtention;










#rm errorReports/core A LA FIN
#rm stackTrace files depending on option ?