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
package org.wildfly.legacy.test.controller.core_7_4_0;

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
import org.wildfly.legacy.test.controller.core_7_4_0.TestModelControllerService7_4_0.DelegatingResourceDefinition;
import org.wildfly.legacy.test.spi.core.TestModelControllerFactory;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TestModelControllerFactory7_4_0 implements TestModelControllerFactory {

    @Override
    public ModelTestModelControllerService create(ProcessType processType, RunningModeControl runningModeControl,
            StringConfigurationPersister persister, ModelTestOperationValidatorFilter validateOpsFilter, TestModelType type,
            ModelInitializer modelInitializer, ExtensionRegistry extensionRegistry) {
        ControlledProcessState processState = new ControlledProcessState(true);
        return new TestModelControllerService7_4_0(processType, runningModeControl, persister, validateOpsFilter, type, modelInitializer, new DelegatingResourceDefinition(type), processState, extensionRegistry);    }

    @Override
    public InjectedValue<ContentRepository> getContentRepositoryInjector(ModelTestModelControllerService service) {
        return ((TestModelControllerService7_4_0)service).getContentRepositoryInjector();
    }
}
