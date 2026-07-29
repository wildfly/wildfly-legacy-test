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
the current WildFly Core dependencies, however their implementations and the TestModelControllerService created from those
factories
needs to be compiled against the classes provided by the legacy server dependencies.

The target legacy server version is controlled by the `property.old.wildfly-core` maven property configured on each
wildfly-legacy-subsystem-XX.YY.ZZ and wildfly-legacy-core-XX.YY.ZZ modules. The current WildFly version is controlled
by the `wildfly.current.version` maven property.


Guidelines to integrate a new controller
===================
The general approach to build when we are integrating a new controller is the following:

1. Create the wildfly-legacy-subsystem-XX.YY.ZZ and wildfly-legacy-core-XX.YY.ZZ new modules in this project. These new maven modules will be handling the dependencies for the legacy model controllers. The simple approach is to created them using the previous ones as the base template.
2. Update the `property.old.wildfly-core` property to the version of the WildFly Core server we want to launch in the testsuite.
3. Update the `wildfly.current.version` property to the version of the WildFly Core server where this model controllers will be integrated.
4. Generate all the legacy dmr resources which will provided on [legacy-models](tools/src/main/resources/legacy-models). To do so, use the tools available on [util](tools/src/main/java/org/wildfly/legacy/util) package. See [README.md](tools/README.md) for more details.

## Notes:

When doing a release bump the version using perl to update the version
in all the poms and Version.java:

    perl -pi -e 's/(\D)7.0.0.Final/${1}7.0.1-Final-SNAPSHOT/g' $(find . -name \*.xml -or -name \*.java)

spi/dependencies/lib directory contains the dependency versions for the WildFly Core server were the version of this project will be integrated. This directory is automatically deleted and populated when you build this project with:

    mvn clean install

If the wildfly-core classes/interfaces needed by legacy test have not been released yet, you can cheat by passing in -Dwildfly.current.version to override and keeping the wildfly.current.version value in this project to the immediate previous version released. This will ensure that the project can be compiled when we push the changes to GitHub. e.g:

    mvn clean install -Dwildfly.current.version=1.0.0.Alpha19-SNAPSHOT

For the above to work you would have needed to built that wildfly-core version. The wildfly-core jars required are plain system dependencies. They are only needed to have something to build against, to resolve circular
dependencies. They do not in any way become part of the released legacy test artifacts.

