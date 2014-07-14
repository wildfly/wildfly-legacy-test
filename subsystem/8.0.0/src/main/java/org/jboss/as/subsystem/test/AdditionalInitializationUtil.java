/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.subsystem.test;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.subsystem.test.ControllerInitializer.TestControllerAccessor;
import org.wildfly.legacy.test.controller.subsystem_8_0_0.TestModelControllerService8_0_0;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class AdditionalInitializationUtil {

    private AdditionalInitializationUtil() {
    }

    public static ProcessType getProcessType(AdditionalInitialization additionalInit) {
        return additionalInit.getProcessType();
    }

    public static RunningMode getRunningMode(AdditionalInitialization additionalInit) {
        return additionalInit.getRunningMode();
    }


    public static void doExtraInitialization(AdditionalInitialization additionalInit, ControllerInitializer controllerInitializer, ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, TestModelControllerService8_0_0 controller) {
        controllerInitializer.setTestModelControllerAccessor(new TestControllerAccessor8_0_0(controller));
        controllerInitializer.initializeModel(rootResource, rootRegistration);
        additionalInit.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration);
    }

    private static class TestControllerAccessor8_0_0 implements TestControllerAccessor {
        private final TestModelControllerService8_0_0 delegate;

        public TestControllerAccessor8_0_0(TestModelControllerService8_0_0 delegate) {
            this.delegate = delegate;
        }


        @Override
        public ServerEnvironment getServerEnvironment() {
            return delegate.getServerEnvironment();
        }

    }
}
