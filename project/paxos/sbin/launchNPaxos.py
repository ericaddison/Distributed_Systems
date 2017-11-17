#!/bin/python
# launch a collection of paxos apps programatically

import sys
import subprocess

# get number of nodes to create
if len(sys.argv) != 2:
	print("Usage: {0} <node list>".format(sys.argv[0]))
	exit(1)

nodefile = sys.argv[1]

# count lines in the file
with open(nodefile, 'r') as f:
	lines = f.readlines()
	nprocs = len(lines)

# launch processes



pids = [subprocess.Popen(['sbin/runPaxos.sh', str(node_id), str(nodefile)]).pid for node_id in range(nprocs)]

with open('pids.txt', 'w') as f:
	for pid in pids:
		f.write('{0} '.format(pid))
