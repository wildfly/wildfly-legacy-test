wildfly-legacy-test
===================

Legacy core-model-test and subsystem-test test controllers

When doing a release bump the version using perl to update the version 
in all the poms and Version.java:

    perl -pi -e 's/1.2.0-SNAPSHOT/1.2.0.GA/g' `find . -name \*.xml -or -name \*.java`

Then checkout the tag of wildfly-core corresponding to the version in 

   wildfly.current.version

in the root pom. Build wildfly-core, since it uses the build directories as the location
for the jars needed from wildfly-core.

To build legacy test then do

    mvn clean install -Dwildfly.lib.directory=/path/to/wildfly-core/checkout 

If the wildfly-core classes/interfaces needed by legacy test have not been released yet,
you can cheat by passing in -Dwildfly.current.version to override it, e.g:

    mvn clean install -Dwildfly.current.version=1.0.0.Alpha19-SNAPSHOT -Dwildfly.lib.directory=/path/to/wildfly-core/checkout

For the above to work you would have needed to built that wildfly-core version. The wildfly-core jars required
are plain system dependencies. They are only needed to have something to build against, to resolve circular dependencies.
They do not in any way become part of the released legacy test artifacts.
 
