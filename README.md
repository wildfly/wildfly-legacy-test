wildfly-legacy-test
===================

Legacy core-model-test and subsystem-test test controllers

When doing a release bump the version using perl to update the version 
in all the poms and Version.java:

    perl -pi -e 's/1.2.0-SNAPSHOT/1.2.0.GA/g' `find . -name \*.xml -or -name \*.java`


To build legacy test then bump the version of wildfly.current.version to the version containing the
wildfly-core classes you need, and build as normal (note you may need to delete tha classes in
spi/dependencies/lib):

    mvn clean install

If the wildfly-core classes/interfaces needed by legacy test have not been released yet,
you can cheat by passing in -Dwildfly.current.version to override it, e.g:

    mvn clean install -Dwildfly.current.version=1.0.0.Alpha19-SNAPSHOT

For the above to work you would have needed to built that wildfly-core version. The wildfly-core jars required
are plain system dependencies. They are only needed to have something to build against, to resolve circular dependencies.
They do not in any way become part of the released legacy test artifacts.
 
