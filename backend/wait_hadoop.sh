#!/bin/bash

no_rnodes=0
n_nodes=$1

while [ $no_rnodes -ne $n_nodes ]; do
    echo "Building hadoop-cluster...please wait..."$no_rnodes 
    /usr/local/hadoop-1.2.1/bin/hadoop dfsadmin -report > tmp.clus
    no_rnodes1=`grep Name: tmp.clus | wc -l`
    no_rnodes=$no_rnodes1
done
echo "Completed cluster building with total number of nodes : "$no_rnodes
