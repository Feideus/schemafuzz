
#!/bin/bash

RESULT="$(psql -d sample_database2 -c 'SELECT * FROM TEST_TABLE')";
RESULT=$( echo $RESULT | grep -P -o "\- .*" )
RESULT=$( echo $RESULT | cut -d "-" -f 2 )
RESULT=$( echo $RESULT | cut -d "(" -f1 )
IFS=' | ' read -ra array <<< "$RESULT"


ITER=0
STOP=3
for i in ${array[@]}
do
    if [ $ITER -lt $STOP ]
    then
      array2[$ITER]=${array[$ITER]}
      array[$ITER]=${array[$ITER+3]}
    fi
    ((ITER++))

done
SCORE=0;

if [ ${array[0]} -eq 10 ]
then
  SCORE=$((SCORE+15+${array[0]}*1000))
fi

if [ ${array[0]} -lt 10 ]
then
  SCORE=$((SCORE+${array[0]}*1000))
fi

if [ ${array[0]} -gt 10 ]
then
  SCORE=$((SCORE+${array[0]}/1000))
fi

if [ ${array[2]} = "t" ]
then
  SCORE=$((SCORE+1000))
fi

if [ ${array2[0]} -eq 32767 ]
then
  SCORE=$((SCORE+15+${array2[0]}*1000))
fi

if [ ${array2[0]} -lt 32767 ]
then
  SCORE=$((SCORE+${array2[0]}/1000))
fi

echo $SCORE
