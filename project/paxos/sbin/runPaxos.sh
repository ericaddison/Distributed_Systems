#! /bin/bash
id=$1
file=$2
restart=$3

dist_classpath=./dist/lib
lib_classpath=./libs
java -classpath $dist_classpath/Paxos.jar:$lib_classpath/gson-2.6.2.jar:. paxos.application.PretendApp $id $file $restart
