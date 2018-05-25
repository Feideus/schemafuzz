#!/bin/bash

RESULT="$(psql -d sample_database2 -c 'SELECT * FROM test_table2')";

RESULT=$( echo $RESULT | grep -P -o "\- .*" )
RESULT=$( echo $RESULT | cut -d "-" -f 2 )
RESULT=$( echo $RESULT | cut -d "(" -f1 )
IFS=' | ' read -ra array <<< "$RESULT"

echo $RESULT
echo ${array[0]}
echo ${array[1]}
echo ${array[2]}
echo ${array[3]}

SCORE=0

if [[ ${array[0]} = "t" && "${array[1]}" = "t" && "${array[2]}" = "t" && "${array[3]}" = "t" ]]
then
  SCORE=$((SCORE+100))
fi

if [[ ${array[0]} = "t" && "${array[1]}" = "t" && "${array[2]}" = "t" && "${array[3]}" = "f" ]]
then
  SCORE=$((SCORE+10))
fi

if [[ ${array[0]} = "t" && "${array[1]}" = "t" && "${array[2]}" = "f" && "${array[3]}" = "f" ]]
then
  SCORE=$((SCORE+25))
fi

if [[ ${array[0]} = "t" && "${array[1]}" = "f" && "${array[2]}" = "f" && "${array[3]}" = "f" ]]
then
  SCORE=$((SCORE+200))
fi

if [[ ${array[0]} = "f" && "${array[1]}" = "f" && "${array[2]}" = "f" && "${array[3]}" = "f" ]]
then
  SCORE=$((SCORE))
fi

if [[ ${array[0]} = "f" && "${array[1]}" = "f" && "${array[2]}" = "t" && "${array[3]}" = "f" ]]
then
  SCORE=$((SCORE+5))
fi

if [[ ${array[0]} = "f" && "${array[1]}" = "t" && "${array[2]}" = "f" && "${array[3]}" = "f" ]]
then
  SCORE=$((SCORE+60))
fi

if [[ ${array[0]} = "f" && "${array[1]}" = "t" && "${array[2]}" = "f" && "${array[3]}" = "t" ]]
then
  SCORE=$((SCORE+60))
fi

if [[ ${array[0]} = "t" && "${array[1]}" = "t" && "${array[2]}" = "f" && "${array[3]}" = "t" ]]
then
  SCORE=$((SCORE+2))
fi

if [[ ${array[0]} = "t" && "${array[1]}" = "f" && "${array[2]}" = "t" && "${array[3]}" = "t" ]]
then
  SCORE=$((SCORE+15))
fi

if [[ ${array[0]} = "f" && "${array[1]}" = "t" && "${array[2]}" = "t" && "${array[3]}" = "t" ]]
then
  SCORE=$((SCORE+40))
fi

if [[ ${array[0]} = "f" && "${array[1]}" = "t" && "${array[2]}" = "t" && "${array[3]}" = "f" ]]
then
  SCORE=$((SCORE+135))
fi

if [[ ${array[0]} = "t" && "${array[1]}" = "f" && "${array[2]}" = "f" && "${array[3]}" = "t" ]]
then
  SCORE=$((SCORE+12))
fi

if [[ ${array[0]} = "t" && "${array[1]}" = "f" && "${array[2]}" = "f" && "${array[3]}" = "f" ]]
then
  SCORE=$((SCORE+1))
fi

if [[ ${array[0]} = "t" && "${array[1]}" = "f" && "${array[2]}" = "t" && "${array[3]}" = "f" ]]
then
  SCORE=$((SCORE+10))
fi

if [[ ${array[0]} = "f" && "${array[1]}" = "f" && "${array[2]}" = "f" && "${array[3]}" = "t" ]]
then
  SCORE=$((SCORE+25))
fi

echo $SCORE


