
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
SCORE=10;


if [ ${array[1]} = "Moy" ]
then
  SCORE=$((SCORE+10))
fi

if [ ${array[2]} = "t" ]
then
  SCORE=$((SCORE+10))
fi

if [ ${array[0]} -eq 32767 ]
then
  SCORE=$((SCORE+10))
fi

if [ ${array[0]} -eq 32767 -a ${array[2]} = "t" -a ${array[1]} = "Moy" ]
then
  SCORE=-1
fi

echo $SCORE
