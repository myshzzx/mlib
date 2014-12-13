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

export CLASS_PATH=main/core/
for f in main/core/*.jar; do CLASS_PATH=$CLASS_PATH:$f; done
java -cp $CLASS_PATH -Dfile.encoding=UTF-8 mysh.cluster.starter.ClusterStart
