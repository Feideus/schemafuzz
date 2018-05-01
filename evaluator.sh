
#!/bin/bash

RESULT="$(psql -d sample_database2 -c 'SELECT * FROM TEST_TABLE')";
RESULT=$( echo $RESULT | grep -P -o "\- .*" )
RESULT=$( echo $RESULT | cut -d "-" -f 2 )
RESULT=$( echo $RESULT | cut -d "(" -f1 )
IFS=' | ' read -ra array <<< "$RESULT"

SCORE=10

if [ "${array[1]}" = "Moy" ]
then
  SCORE=$((SCORE+10))
fi

if [ "${array[2]}" = "t" ]
then
  SCORE=$((SCORE+10))
fi

if [[ "${array[0]}" -eq 32767 ]]
then
  SCORE=$((SCORE+10))
fi

if [[ ${array[0]} -eq 32767 && "${array[2]}" = "t" && "${array[1]}" = "Moy" ]]
then
  SCORE=-1
fi

echo $SCORE
