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

package org.wildfly.legacy.test.controller.subsystem_10_0_0;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestOperationValidatorFilter;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.AdditionalInitializationUtil;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.wildfly.legacy.test.spi.subsystem.TestModelControllerFactory;

/**
 *
 * @author Tomaz Cerar
 */
public class TestModelControllerFactory10_0_0 implements TestModelControllerFactory {

    @Override
    public ModelTestModelControllerService create(Extension mainExtension, ControllerInitializer controllerInitializer,
            AdditionalInitialization additionalInit, ExtensionRegistry extensionRegistry, StringConfigurationPersister persister,
            ModelTestOperationValidatorFilter validateOpsFilter, boolean registerTransformers) {
        return new TestModelControllerService10_0_0(mainExtension, controllerInitializer, additionalInit, new RunningModeControl(AdditionalInitializationUtil.getRunningMode(additionalInit)), extensionRegistry, persister, validateOpsFilter, registerTransformers);
    }
}
