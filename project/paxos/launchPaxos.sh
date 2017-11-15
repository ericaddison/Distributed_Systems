#! /bin/bash
id=$1
file=$2
restart=$3

classpath=./dist/lib
java -classpath $classpath/Paxos.jar:. paxos.NetworkNode $id $file $restart
