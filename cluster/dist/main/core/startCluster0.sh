#!/bin/sh

kill -9 $1
ping -W 2 -c 1 254.254.254.254

./update.sh
rm -f update.sh

cp -r update/* main
rm -f -r update

mkdir main/user
mkdir update
mkdir update/core

export CLUSTER_CP=main/core/
for f in main/core/*.jar; do CLUSTER_CP=$CLUSTER_CP:$f; done
java -cp $CLUSTER_CP -Dfile.encoding=UTF-8 mysh.cluster.starter.ClusterStart
