/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.legacy.test.controller.core_35_0_0;

import org.jboss.as.controller.CapabilityRegistry;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestOperationValidatorFilter;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.repository.ContentRepository;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.legacy.test.spi.core.TestModelControllerFactory;

/**
 *
 * @author Tomaz Cerar
 */
public class TestModelControllerFactory35_0_0 implements TestModelControllerFactory {

    @Override
    public ModelTestModelControllerService create(ProcessType processType, RunningModeControl runningModeControl,
                                                  StringConfigurationPersister persister, ModelTestOperationValidatorFilter validateOpsFilter, TestModelType type,
                                                  ModelInitializer modelInitializer, ExtensionRegistry extensionRegistry) {
        ControlledProcessState processState = new ControlledProcessState(true);
        CapabilityRegistry capabilityRegistry = new CapabilityRegistry(type == TestModelType.STANDALONE);

        return new TestModelControllerService35_0_0(processType, runningModeControl, persister, validateOpsFilter, type,
                modelInitializer, new TestModelControllerService35_0_0.DelegatingResourceDefinition(type), processState,
                extensionRegistry, capabilityRegistry);
    }

    @Override
    public InjectedValue<ContentRepository> getContentRepositoryInjector(ModelTestModelControllerService service) {
        return ((TestModelControllerService35_0_0) service).getContentRepositoryInjector();
    }
}
