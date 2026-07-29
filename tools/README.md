wildfly-legacy-test tools
=========================

The files in [legacy-models](src/main/resources/legacy-models) folder contains dmr files used by the testsuite. They contain the management model resource definitions (-resource-definition-XYZ.dmr) and model versions (-model-versions-SYX.dmr suffix ). The XYZ represents the EAP/WildFly release these files were created from.

The wfXY.dmr files are generated using the WildFly version which was the base version used to create the future EAP.

For example, the -wf41.dmr files were created using WildFly 4.1, which was the base for EAP 8.2.0.Beta.

Once EAP 8.2.0.Beta gets released, we will create a new set of dmr files with the suffix -8.2.0.dmr.

Those dmr files will be used by the Wildfly testsuites.

# How to generate the -dmr files.

1. Start a standalone WildFly server version you want to use to generate the dmr files for. For example, to generate a -wf41.dmr file, you need to start a WildFly 41.0.0.Final server.
2. Run [DumpStandaloneResourceDefinitionUtil.java](src/main/java/org/wildfly/legacy/util/DumpStandaloneResourceDefinitionUtil.java) and [GrabModelVersionsUtil.java](src/main/java/org/wildfly/legacy/util/GrabModelVersionsUtil.java). Those classes will generate the resource definition and version -dmr files in the target folder. Copy them from the files from the target folder and place them in the [legacy-models](src/main/resources/legacy-models) folder following the same nomenclature as the existing files.
3. The -resource-definition-XYZ.dmr files must be copied to [legacy-versions](../versions/src/main/resources/legacy-versions) directory.
4. Adjust the main method of [LegacyVersions.java](../versions/src/main/java/org/wildfly/legacy/version/LegacyVersions.java) to verify these version files are loaded correctly.
5. Repeat the process but this time using a server running in Domain mode and using  [DumpDomainResourceDefinitionUtil.java](src/main/java/org/wildfly/legacy/util/DumpDomainResourceDefinitionUtil.java) to generate the resource variant instead. There is no need to run GrabModelVersionsUtil.java again for the domain variants.