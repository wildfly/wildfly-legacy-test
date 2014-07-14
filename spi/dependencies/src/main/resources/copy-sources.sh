#!/bin/sh


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DIR=$DIR/../../../lib

echo Updating jars from $1 to $DIR 

echo
   
rm $DIR/*-sources.jar

cp -v $1/controller/target/wildfly-controller-*-sources.jar $DIR
cp -v $1/core-model-test/framework/target/wildfly-core-model-test-framework-*-sources.jar $DIR
cp -v $1/deployment-repository/target/wildfly-deployment-repository-*-sources.jar $DIR
cp -v $1/model-test/target/wildfly-model-test-*-sources.jar $DIR
cp -v $1/server/target/wildfly-server-*-sources.jar $DIR
cp -v $1/subsystem-test/framework/target/wildfly-subsystem-test-framework-*-sources.jar $DIR