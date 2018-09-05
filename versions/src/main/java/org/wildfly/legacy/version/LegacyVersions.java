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
package org.wildfly.legacy.version;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.as.controller.ModelVersion;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Tomaz Cerar
 */
public class LegacyVersions {


    private static ModelVersion createModelVersion(ModelNode node) {
        return ModelVersion.create(
                readVersion(node, MANAGEMENT_MAJOR_VERSION),
                readVersion(node, MANAGEMENT_MINOR_VERSION),
                readVersion(node, MANAGEMENT_MICRO_VERSION));
    }

    private static int readVersion(ModelNode node, String name) {
        if (!node.hasDefined(name)) {
            return 0;
        }
        return node.get(name).asInt();
    }

    public static void main(String[] args) {
        // TODO rename all the files with the wf14 suffix (also in tools/src/main/resources/legacy-models) when EAP 7.2.0 is out.
        output("wf14");
        output("7.0.0");
        output("7.1.0");
        output("6.4.0");
    }

    private static void output(String version) {
        System.out.println("==== " + version + "====");
        System.out.println(getModelVersions(version));
    }

    public static Map<String, ModelVersion> getModelVersions(String testControllerVersion) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("legacy-versions/standalone-model-versions-" + testControllerVersion + ".dmr")) {
            ModelNode legacyModelVersions = ModelNode.fromStream(stream);
            return legacyModelVersions.get(SUBSYSTEM).asPropertyList()
                    .stream()
                    .collect(Collectors.toMap(Property::getName, p -> createModelVersion(p.getValue())));
        } catch (IOException e) {
            throw new RuntimeException("Could not load legacy subsystem version");
        }

    }


    public static ModelVersion getSubsystemModelVersion(String testControllerVersion, String subsystemName) {
        return getModelVersions(testControllerVersion).get(subsystemName);

    }
}
