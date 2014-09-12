#!/bin/bash
while read i
do
echo i|./mrtandem -reducer2_1 /tmp/ 1410486302887capercloud_test.mgf.xml
done
hadoop dfs -put /tmp/reducer2_1 .
