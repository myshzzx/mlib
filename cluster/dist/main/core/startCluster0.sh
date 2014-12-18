#!/bin/sh

ping -W 2 -c 1 254.254.254.254
kill -9 $1

./update.sh
rm -f update.sh

cp -r update/* main
rm -f -r update

export CLUSTER_CP=main/core/
for f in main/core/*.jar; do CLUSTER_CP=$CLUSTER_CP:$f; done
java -cp $CLUSTER_CP -Dfile.encoding=UTF-8 -Djava.security.manager=mysh.cluster.ClusterSecMgr -Djava.security.policy=main/core/permission.txt mysh.cluster.starter.ClusterStart
