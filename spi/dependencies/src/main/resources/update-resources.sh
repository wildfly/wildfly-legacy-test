#!/bin/sh


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DIR=$DIR/../../../lib

echo Updating jars from $1 to $DIR 

echo
   
rm $DIR/*-SNAPSHOT.jar

cp -v $1/controller/target/wildfly-controller-*-SNAPSHOT.jar $DIR
cp -v $1/core-model-test/framework/target/wildfly-core-model-test-framework-*-SNAPSHOT.jar $DIR
cp -v $1/deployment-repository/target/wildfly-deployment-repository-*-SNAPSHOT.jar $DIR
cp -v $1/model-test/target/wildfly-model-test-*-SNAPSHOT.jar $DIR
cp -v $1/server/target/wildfly-server-*-SNAPSHOT.jar $DIR
cp -v $1/subsystem-test/framework/target/wildfly-subsystem-test-framework-*-SNAPSHOT.jar $DIR