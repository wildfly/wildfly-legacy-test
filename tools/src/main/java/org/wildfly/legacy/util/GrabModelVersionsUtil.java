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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.XML_NAMESPACES;

import java.io.File;
import java.nio.file.Path;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Grabs the model versions for the currently running standalone version for use in the
 * <a href="https://community.jboss.org/wiki/AS7ManagementVersions">AS7 Management Versions wiki</a>.
 * It also saves the model versions in dmr format to {@code target/standalone-model-versions-running.dmr}
 * If this is for a released version so that it can be used for comparisons in the future, this file should be copied to
 * {@code src/test/resources/legacy-models} and {@code running} replaced with the real version of the running server, e.g.
 * {@code src/test/resources/legacy-models/standalone-model-versions-7.1.2.Final}. *
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GrabModelVersionsUtil {

    public static void main(String[] args) throws Exception {
        ModelNode versions = Tools.getCurrentModelVersions();
        System.out.println("<table border=\"1\">");
        System.out.println("<tr><th>Subsystem</th><th>Management Version</th><th>Schemas</th></tr>");
        System.out.print("<tr valign=\"top\" align=\"left\"><td><b>Standalone core</b></td><td>");
        System.out.print(Tools.createModelVersion(versions.get(Tools.CORE, Tools.STANDALONE)));
        System.out.println("</td><td>&nbsp;</td></tr>");

        for (Property entry : versions.get(SUBSYSTEM).asPropertyList()) {
            System.out.print("<tr valign=\"top\" align=\"left\"><td><b>");
            System.out.print(entry.getName());
            System.out.print("</b></td><td>");
            System.out.print(Tools.createModelVersion(entry.getValue()));
            System.out.print("<td>");

            boolean first = true;
            for (ModelNode ns : entry.getValue().get(XML_NAMESPACES).asList()) {
                if (first) {
                    first = false;
                } else {
                    System.out.println("<br/>");
                }
                System.out.print(ns.asString());
            }


            System.out.print("</td>");
            System.out.println("</td></tr>");
        }

        System.out.println("</table>");

        System.out.println("----------------");
        final Path projectDir = Tools.getProjectDirectory();
        final Path target = projectDir.resolve("target");
        Path file = target.resolve("standalone-model-versions-running.dmr");

        Tools.serializeModeNodeToFile(versions, file);
    }
}
