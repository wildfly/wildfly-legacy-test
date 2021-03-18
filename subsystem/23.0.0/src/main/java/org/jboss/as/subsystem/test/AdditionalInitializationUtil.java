/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat, Inc., and individual contributors as indicated
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

package org.jboss.as.subsystem.test;

import org.jboss.as.controller.ManagementModel;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.subsystem.test.ControllerInitializer.TestControllerAccessor;
import org.wildfly.legacy.test.controller.subsystem_23_0_0.TestModelControllerService23_0_0;

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


    public static void doExtraInitialization(AdditionalInitialization additionalInit, ControllerInitializer controllerInitializer, ExtensionRegistry extensionRegistry, ManagementModel managementModel, TestModelControllerService23_0_0 controller) {
        controllerInitializer.setTestModelControllerAccessor(new TestControllerAccessor23_0_0(controller));
        controllerInitializer.initializeModel(managementModel.getRootResource(), managementModel.getRootResourceRegistration());
        additionalInit.initializeExtraSubystemsAndModel(extensionRegistry, managementModel.getRootResource(), managementModel.getRootResourceRegistration(), managementModel.getCapabilityRegistry());
    }

    private static class TestControllerAccessor23_0_0 implements TestControllerAccessor {
        private final TestModelControllerService23_0_0 delegate;

        public TestControllerAccessor23_0_0(TestModelControllerService23_0_0 delegate) {
            this.delegate = delegate;
        }


        @Override
        public ServerEnvironment getServerEnvironment() {
            return delegate.getServerEnvironment();
        }

    }
}
