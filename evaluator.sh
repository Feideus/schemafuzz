
#!/bin/bash

RESULT="$(psql -d sample_database2 -c 'SELECT * FROM TEST_TABLE')";
RESULT=$( echo $RESULT | grep -P -o "\- .*" )
RESULT=$( echo $RESULT | cut -d "-" -f 2 )
echo $RESULT
