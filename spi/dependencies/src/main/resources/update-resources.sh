#!/bin/sh

# To update, checkout the wildfly-core tag to update to and build it.
# Then run this script to update the sources, passing in the wildfly-core
# checkout root folder as the argument


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DIR=$DIR/../../../lib

echo Updating jars from $1 to $DIR 

echo
   
rm $DIR/*.jar

cp -v $1/controller/target/wildfly-controller-*.jar $DIR
cp -v $1/core-model-test/framework/target/wildfly-core-model-test-framework-*.jar $DIR
cp -v $1/deployment-repository/target/wildfly-deployment-repository-*.jar $DIR
cp -v $1/model-test/target/wildfly-model-test-*.jar $DIR
cp -v $1/server/target/wildfly-server-*.jar $DIR
cp -v $1/subsystem-test/framework/target/wildfly-subsystem-test-framework-*.jar $DIR
