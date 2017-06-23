/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.legacy.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.as.controller.ModelVersion;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Compares the current running server with the passed in version. The passed in version
 * must have model and resource definition dumps in {@code src/test/resources/legacy-models} as
 *
 * @author Tomaz Cerar
 */
public class GenerateProductModelVersionsTable {

    private final ModelNode currentModelVersions;
    private final ModelNode legacyModelVersions;

    private GenerateProductModelVersionsTable(ModelNode legacyModelVersions,
                                              ModelNode currentModelVersions

    ) throws Exception {
        this.currentModelVersions = currentModelVersions;
        this.legacyModelVersions = legacyModelVersions;
    }

    public static void main(String[] args) throws Exception {

        if (GenerateProductModelVersionsTable.class.getProtectionDomain().getCodeSource().getLocation().toString().endsWith(".jar")) {
            throw new Exception("This currently does not work as a jar. Please import a clone of https://github.com/kabir/wildfly-legacy-test into your IDE");
        }

        String version = System.getProperty("jboss.as.compare.version", null);
        String fromTgt = System.getProperty("jboss.as.compare.from.target", null);

        if (version == null) {
            System.out.print("Enter legacy EAP version: ");
            version = readInput(null);
        }
        System.out.println("Using target model: " + version);


        if (fromTgt == null) {
            System.out.print("Read from target directory or from the legacy-models directory - t/[l]:");
            fromTgt = readInput("l");
        }

        final Path fromDirectory;
        if (fromTgt.equals("l")) {
            URL legacyModels = Thread.currentThread().getContextClassLoader().getResource("legacy-models");
            fromDirectory = Paths.get(legacyModels.toURI());
        } else if (fromTgt.equals("t")) {
            fromDirectory = Tools.getProjectDirectory().resolve("target");
        } else {
            throw new IllegalArgumentException("Please enter 'l' for legacy-models directory or 't' for target directory");
        }

        System.out.println("Loading legacy model versions for " + version + "....");
        ModelNode legacyModelVersions = Tools.loadModelNodeFromFile(fromDirectory.resolve("standalone-model-versions-" + version + ".dmr"));
        System.out.println("Loaded legacy model versions");

        System.out.println("Loading model versions for currently running server...");
        ModelNode currentModelVersions = Tools.getCurrentModelVersions();
        System.out.println("Loaded current model versions");

        doCompare(legacyModelVersions, currentModelVersions);

    }

    private static void doCompare(ModelNode legacyModelVersions,
                                  ModelNode currentModelVersions) throws Exception {

        GenerateProductModelVersionsTable compareModelVersionsUtil = new GenerateProductModelVersionsTable(legacyModelVersions, currentModelVersions);

        System.out.println("Starting comparison of the current....\n");
        compareModelVersionsUtil.generateReport();

    }

    private static String readInput(String defaultAnswer) throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = (char) System.in.read();
        while (c != '\n') {
            sb.append(c);
            c = (char) System.in.read();
        }
        String s = sb.toString().trim();
        if (s.equals("")) {
            if (defaultAnswer != null) {
                return defaultAnswer;
            }
            throw new IllegalArgumentException("Please enter a valid answer");
        }
        return s;
    }

    private void generateReport() {
        StringBuilder sb = new StringBuilder();
        ModelVersion legacyVersion = Tools.createModelVersion(legacyModelVersions.get(Tools.CORE, Tools.STANDALONE));
        ModelVersion currentVersion = Tools.createModelVersion(currentModelVersions.get(Tools.CORE, Tools.STANDALONE));
        String currentEAPVersion = currentModelVersions.get(Tools.CORE, "product").get(PRODUCT_VERSION).asString();
        String legacyEAPVersion = legacyModelVersions.get(Tools.CORE, "product").get(PRODUCT_VERSION).asString();
        sb.append("| Subsystem | ")
                .append(legacyEAPVersion).append(" - *").append(legacyVersion).append("* | ")
                .append(currentEAPVersion).append(" - *").append(currentVersion).append("* |\n");
        sb.append("| --- | --- | --- | \n");
        for (Property entry : legacyModelVersions.get(SUBSYSTEM).asPropertyList()) {
            ModelVersion legacy = Tools.createModelVersion(entry.getValue());
            ModelVersion current = Tools.createModelVersion(currentModelVersions.get(SUBSYSTEM, entry.getName()));
            boolean diff = !legacy.equals(current);
            String name = diff ? ("**" + entry.getName() + "**") : entry.getName();
            sb.append("| ")
                    .append(name).append(" | ")
                    .append(legacy).append(" | ")
                    .append(current).append(" | ")
                    .append("\n");

        }

        System.out.println(sb.toString());
    }


}
