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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import java.nio.file.Path;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 * Grabs the full domain resource description of a running domain instance and writes it out to {@code target/domain-resource-definition-running.dmr}.
 * The host model is written out to {@code target/host-resource-definition-running.dmr}.
 * If this is for a released version so that it can be used for comparisons in the future, these files should be copied to
 * {@code src/test/resources/legacy-models} and {@code running} replaced with the real version of the running server, e.g.
 * {@code src/test/resources/legacy-models/domain-resource-definition-7.1.2.Final} and {@code src/test/resources/legacy-models/host-resource-definition-7.1.2.Final}.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DumpDomainResourceDefinitionUtil {

    public static void main(String[] args) throws Exception {
        ModelNode resourceDefinition = Tools.getCurrentRunningDomainResourceDefinition();
        System.out.println("Dumping domain model....");

        final Path projectDir = Tools.getProjectDirectory();
        final Path target = projectDir.resolve("target");
        Path file = target.resolve(ResourceType.DOMAIN.toString().toLowerCase() + "-resource-definition-running.dmr");

        Tools.serializeModeNodeToFile(resourceDefinition, file);

        resourceDefinition = Tools.getCurrentRunningResourceDefinition(PathAddress.pathAddress(PathElement.pathElement(HOST, "master")));
        System.out.println("Dumping host model....");

        file = target.resolve(ResourceType.HOST.toString().toLowerCase() + "-resource-definition-running.dmr");
        Tools.serializeModeNodeToFile(resourceDefinition, file);

    }
}
