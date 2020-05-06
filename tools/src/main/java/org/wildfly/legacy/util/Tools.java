/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors as indicated
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROXIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.XML_NAMESPACES;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Tools {

    static final String CORE = "core";
    static final String STANDALONE = "standalone";
    static final String VERSION = "version";

    static ModelNode getAndCheckResult(ModelNode result) {
        if (!result.get(OUTCOME).asString().equals(SUCCESS)) {
            throw new RuntimeException(result.get(FAILURE_DESCRIPTION).toString());
        }
        return result.get(RESULT);
    }

    static ModelVersion createModelVersion(ModelNode node) {
        return ModelVersion.create(
                readVersion(node, MANAGEMENT_MAJOR_VERSION),
                readVersion(node, MANAGEMENT_MINOR_VERSION),
                readVersion(node, MANAGEMENT_MICRO_VERSION));
    }

    static ModelNode readModelVersionFields(ModelNode node) {
        ModelNode version = new ModelNode();
        if (node.hasDefined(MANAGEMENT_MAJOR_VERSION)) {
            version.get(MANAGEMENT_MAJOR_VERSION).set(node.get(MANAGEMENT_MAJOR_VERSION));
        }
        if (node.hasDefined(MANAGEMENT_MINOR_VERSION)) {
            version.get(MANAGEMENT_MINOR_VERSION).set(node.get(MANAGEMENT_MINOR_VERSION));

        }
        if (node.hasDefined(MANAGEMENT_MICRO_VERSION)) {
            version.get(MANAGEMENT_MICRO_VERSION).set(node.get(MANAGEMENT_MICRO_VERSION));
        }
        return version;
    }

    static ModelNode readProductInfo(ModelNode node) {
        ModelNode version = new ModelNode();
        if (node.hasDefined(PRODUCT_NAME)) {
            version.get(PRODUCT_NAME).set(node.get(PRODUCT_NAME));
        }
        if (node.hasDefined(PRODUCT_VERSION)) {
            version.get(PRODUCT_VERSION).set(node.get(PRODUCT_VERSION));

        }
        return version;
    }


    static ModelNode getCurrentModelVersions() throws Exception {
        ModelControllerClient client = getClient();
        try {
            ModelNode allVersions = new ModelNode();

            ModelNode op = new ModelNode();
            op.get(OP).set(READ_RESOURCE_OPERATION);
            op.get(INCLUDE_RUNTIME).set(true);
            op.get(OP_ADDR).setEmptyList();
            ModelNode result = Tools.getAndCheckResult(client.execute(op));

            allVersions.get(CORE, STANDALONE).set(readModelVersionFields(result));
            allVersions.get(CORE, "product").set(readProductInfo(result));

            op.get(OP_ADDR).add(EXTENSION, "*").add(SUBSYSTEM, "*");
            result = Tools.getAndCheckResult(client.execute(op));

            //Shove it into a tree map to sort the subsystems alphabetically
            TreeMap<String, ModelNode> map = new TreeMap<>();
            List<ModelNode> subsystemResults = result.asList();
            for (ModelNode subsystemResult : subsystemResults) {
                String subsystemName = PathAddress.pathAddress(subsystemResult.get(OP_ADDR)).getLastElement().getValue();
                map.put(subsystemName, Tools.getAndCheckResult(subsystemResult));
            }

            for (Map.Entry<String, ModelNode> entry : map.entrySet()) {
                allVersions.get(SUBSYSTEM, entry.getKey()).set(readModelVersionFields(entry.getValue()));
                allVersions.get(SUBSYSTEM, entry.getKey()).get(XML_NAMESPACES).set(entry.getValue().get(XML_NAMESPACES));
            }
            return allVersions;
        } finally {
            IoUtils.safeClose(client);
        }
    }

    static ModelNode getCurrentRunningResourceDefinition(PathAddress pathAddress) throws Exception {
        ModelControllerClient client = getClient();
        try {
            ModelNode op = new ModelNode();
            op.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
            op.get(OP_ADDR).set(pathAddress.toModelNode());
            op.get(RECURSIVE).set(true);
            op.get(OPERATIONS).set(true);
            op.get(PROXIES).set(false);
            op.get(INHERITED).set(false);

            return Tools.getAndCheckResult(client.execute(op));

        } finally {
            IoUtils.safeClose(client);
        }
    }

    static ModelNode getCurrentRunningDomainResourceDefinition() throws Exception {
        ModelNode node = getCurrentRunningResourceDefinition(PathAddress.EMPTY_ADDRESS);
        ModelNode profile = node.get(CHILDREN).require(PROFILE).require(MODEL_DESCRIPTION).require("*");
        node.get(CHILDREN).require(HOST);
        node.require(CHILDREN).remove(HOST);

        //Get rid of the profile children. Subsystems are handled for the standalone model
        profile.get(CHILDREN).setEmptyList();
        return node;
    }

    static int readVersion(ModelNode node, String name) {
        if (!node.hasDefined(name)) {
            return 0;
        }
        return node.get(name).asInt();
    }

    static void serializeModeNodeToFile(ModelNode modelNode, Path file) throws Exception {
        Files.deleteIfExists(file);
        PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file));
        try {
            modelNode.writeString(writer, false);
            System.out.println("Resource definition for running server written to: " + file.toString());
        } finally {
            IoUtils.safeClose(writer);
        }
    }

    static Path getProjectDirectory() throws URISyntaxException {
        //Try to work around IntilliJ's crappy current directory handling
        return Paths.get(CompareModelVersionsUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().getParent();
    }

    static ModelNode loadModelNodeFromFile(Path path) throws Exception {
        if (Files.notExists(path)) {
            throw new IllegalArgumentException("File does not exist " + path);
        }

        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                line = reader.readLine();
            }
        }
        return ModelNode.fromString(sb.toString());
    }

    private static ModelControllerClient getClient() throws UnknownHostException {
        String protocol = System.getProperty("wildfly.util.protocol");
        int port = Integer.valueOf(System.getProperty("wildfly.util.port", "-1"));

        if (protocol != null || port >= 0) {
            if (protocol != null && port >= 0) {
                return ModelControllerClient.Factory.create(protocol, "localhost", port);
            } else if (protocol != null) {
                if (protocol.equals("remote")) {
                    port = 9999;
                } else if (protocol.equals("http-remoting")) {
                    port = 9990;
                }
                return ModelControllerClient.Factory.create(protocol, "localhost", port);
            } else {
                throw new IllegalStateException("port specified without protocol");
            }
        }

        //Try to figure out how to connect if the user did not specify anything
        ModelControllerClient client = ModelControllerClient.Factory.create("http-remoting", "localhost", 9990);
        try {
            client.execute(Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS));
            return client;
        } catch (Exception e) {
            IoUtils.safeClose(client);
        }

        client = ModelControllerClient.Factory.create("remote", "localhost", 9999);
        try {
            client.execute(Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS));
            return client;
        } catch (Exception e) {
            IoUtils.safeClose(client);
        }

        throw new IllegalStateException("Could not figure out how to connect to the host. " +
                "Please use -Dwildfly.util.protocol and wildfly.util.port to specify where to connect");
    }

    public static ModelVersion getLegacySubsystemVersion(String testControllerVersion, String subsystemName) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("legacy-models/standalone-model-versions-" + testControllerVersion + ".dmr")) {
            ModelNode legacyModelVersions = ModelNode.fromStream(stream);
            return Tools.createModelVersion(legacyModelVersions.get(SUBSYSTEM, subsystemName));

        } catch (IOException e) {
            throw new RuntimeException("Could not load legacy subsystem version");
        }

    }
}
