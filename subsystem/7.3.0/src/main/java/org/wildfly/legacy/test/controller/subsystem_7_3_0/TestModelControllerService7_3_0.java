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

package org.wildfly.legacy.test.controller.subsystem_7_3_0;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.BootContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestOperationValidatorFilter;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.controller.resources.ServerDeploymentResourceDefinition;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.AdditionalInitializationUtil;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.dmr.ModelNode;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class TestModelControllerService7_3_0 extends ModelTestModelControllerService {

    private final ExtensionRegistry extensionRegistry;
    private final AdditionalInitialization additionalInit;
    private final ControllerInitializer controllerInitializer;
    private final Extension mainExtension;

    TestModelControllerService7_3_0(final Extension mainExtension, final ControllerInitializer controllerInitializer,
                                    final AdditionalInitialization additionalInit, final RunningModeControl runningModeControl, final ExtensionRegistry extensionRegistry,
                                    final StringConfigurationPersister persister, final ModelTestOperationValidatorFilter validateOpsFilter, final boolean registerTransformers) {
        super(AdditionalInitializationUtil.getProcessType(additionalInit), runningModeControl, extensionRegistry.getTransformerRegistry(), persister, validateOpsFilter,
                DESC_PROVIDER, new ControlledProcessState(true), Controller73x.INSTANCE);
        this.extensionRegistry = extensionRegistry;
        this.additionalInit = additionalInit;
        this.controllerInitializer = controllerInitializer;
        this.mainExtension = mainExtension;
    }

    private static final DescriptionProvider DESC_PROVIDER = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ModelNode model = new ModelNode();
            model.get("description").set("The test model controller");
            return model;
        }
    };

    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        initModel(rootResource, rootRegistration, null);
    }

    @Override
    protected void initExtraModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        rootResource.getModel().get(SUBSYSTEM);

        ManagementResourceRegistration deployments = rootRegistration.registerSubModel(ServerDeploymentResourceDefinition.create(new ContentRepository() {

            @Override
            public boolean syncContent(byte[] hash) {
                return false;
            }

            @Override
            public void removeContent(byte[] hash, Object reference) {
            }

            @Override
            public boolean hasContent(byte[] hash) {
                return false;
            }

            @Override
            public VirtualFile getContent(byte[] hash) {
                return null;
            }

            @Override
            public void addContentReference(byte[] hash, Object reference) {
            }

            @Override
            public byte[] addContent(InputStream stream) throws IOException {
                return null;
            }
        }, null));

        //Hack to be able to access the registry for the jmx facade

        rootRegistration.registerOperationHandler(RootResourceHack.DEFINITION, RootResourceHack.INSTANCE);

        extensionRegistry.setSubsystemParentResourceRegistrations(rootRegistration, deployments);
        AdditionalInitializationUtil.doExtraInitialization(additionalInit, controllerInitializer, extensionRegistry, rootResource, rootRegistration);
    }


    @Override
    protected void boot(BootContext context) throws ConfigurationPersistenceException {
        try {
            super.boot(context);
        } finally {
            countdownDoneLatch();
        }
    }

    @Override
    protected void preBoot(List<ModelNode> bootOperations, boolean rollbackOnRuntimeFailure) {
        mainExtension.initialize(extensionRegistry.getExtensionContext("Test", true));
    }

}
