# Weighted Paxos

This is an implementation of weighted Paxos, done as a final project for Distributed Systems at UT Austin, Fall 2017. 

## Building
This project has an `ant` build file. With `ant` installed, simply running the command `ant` should build the project and create the jar file.

## Running
There are several scripts for convenience. The list of commands below show how to perform some of the basic tasks. In addition, there are several directories under `./demo/` that have simple `run.sh` scripts which will launch a Paxos system automatically.

## Common useful commands


Kill all and remove pids files
```
python sbin/killPaxosNodes.py `cat pids_*` ; rm -rf pids_*
```


Run ten nodes
```
python sbin/launchNPaxos.py inputs/tenNodes.txt
```


Kill all, remove pids files, build, run ten nodes
```
python sbin/killPaxosNodes.py `cat pids_*` ; rm -rf pids_* ; ant ; python sbin/launchNPaxos.py inputs/tenNodes_unreliable_diffweights.txt
```


Kill first node in pids_0
```
cat pids_0.txt | cut -d' ' -f1 | xargs python sbin/killPaxosNodes.py
```


Restart node 0
```
python sbin/restartPaxos.py inputs/tenNodes.txt states/node_0.state
```


show INFO logs for proc 0
```
cat logs/log_0.log | grep INFO
```


manual kill ALL PretendApp procs using
```
jps | grep PretendApp | cut -f1 -d' ' | xargs kill
```


look for pretendapp messages on log0
```
cat logs/log_0.log | grep -i pretendapp
```


look for crash messages on all logs
```
cat logs/log_*.log | grep -i crash
```


scrape paxos round times
```
cat logs/log_0.log | grep -i pretendapp | grep time | rev | cut -d' ' -f3 | rev > times.dat
```
