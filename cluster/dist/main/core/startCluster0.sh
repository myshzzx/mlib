#!/bin/sh

ping -W 2 -c 1 254.254.254.254
kill -9 $1

chmod +x update.sh
./update.sh
rm -f update.sh

cp -r update/* main
rm -f -r update

export CLUSTER_ROOT=$PWD
export CLUSTER_CP=main/core/
for f in main/core/*.jar; do CLUSTER_CP=$CLUSTER_CP:$f; done
java -cp $CLUSTER_CP -Xms100m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8535 -Dfile.encoding=UTF-8 -Djava.security.manager=mysh.cluster.ClusterSecMgr -Djava.security.policy=main/core/permission.txt mysh.cluster.starter.ClusterStart
