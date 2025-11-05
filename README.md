wildfly-legacy-test
===================

Legacy core-model-test and subsystem-test test controllers for WF Core.
This project supplies the classes to boostrap legacy controllers for the WildFly
core-model-test and subsystem-test modules.

The WildFly core-model-test and subsystem-test modules are in charge of preparing all the necessary classes to boostrap
the
controllers for the legacy servers. The wildfly-legacy-subsystem-XX.YY.ZZ and wildfly-legacy-core-XX.YY.ZZ artifacts
provided by this project will be added on the classpath of the transformer tests launched by the WildFly testsuite
together with the dependencies for the legacy controllers. It means that wildfly-legacy-subsystem-XX.YY.ZZ and
wildfly-legacy-core-XX.YY.ZZ
need to be compiled using a mix of dependencies, for example the TestModelControllerFactory under the SPI module needs
to be compiled with
the current WildFly dependencies, however their implementations and the TestModelControllerService created from those
factories
needs to be compiled against the classes provided by the legacy server modules.

The target legacy server version is controlled by the `property.old.wildfly-core` maven property configured on each
wildfly-legacy-subsystem-XX.YY.ZZ and wildfly-legacy-core-XX.YY.ZZ modules. The current WildFly version is controlled
by the `wildfly.current.version` maven property.


Guidelines to integrate a new controller
===================
The general approach to build when we are integrating a new controller is the following:

1. Update the `property.old.wildfly-core` property to the version of the target WildFly server we want to launch.
2. Update the `wildfly.current.version` property to the version of the WildFly server.
3. Keep the wildfly-legacy-subsystem-XX.YY.ZZ and wildfly-legacy-core-XX.YY.ZZ compiled with the classes provided by the
   legacy server dependencies.

When doing a release bump the version using perl to update the version
in all the poms and Version.java:

    perl -pi -e 's/(\D)7.0.0.Final/${1}7.0.1-Final-SNAPSHOT/g' $(find . -name \*.xml -or -name \*.java)

To build legacy test then bump the version of wildfly.current.version to the version containing the
wildfly-core classes you need, and build as normal (note you may need to delete tha classes in
spi/dependencies/lib):

    mvn clean install

If the wildfly-core classes/interfaces needed by legacy test have not been released yet,
you can cheat by passing in -Dwildfly.current.version to override it, e.g:

    mvn clean install -Dwildfly.current.version=1.0.0.Alpha19-SNAPSHOT

For the above to work you would have needed to built that wildfly-core version. The wildfly-core jars required
are plain system dependencies. They are only needed to have something to build against, to resolve circular
dependencies.
They do not in any way become part of the released legacy test artifacts.
 
