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
package org.wildfly.legacy.test.controller.subsystem_31_0_0;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.as.controller.CapabilityRegistry;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ManagementModel;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.operations.common.ProcessEnvironment;
import org.jboss.as.controller.operations.global.GlobalNotifications;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestOperationValidatorFilter;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.repository.ContentReference;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.DeployerChainAddHandler;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.jboss.as.server.controller.resources.ServerDeploymentResourceDefinition;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.AdditionalInitializationUtil;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.version.ProductConfig;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.vfs.VirtualFile;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Tomaz Cerar
 */
public class TestModelControllerService31_0_0 extends ModelTestModelControllerService {
    private final Extension mainExtension;
    private final AdditionalInitialization additionalInit;
    private final ControllerInitializer controllerInitializer;
    private final ExtensionRegistry extensionRegistry;
    private final RunningModeControl runningModeControl;
    private final ContentRepository contentRepository = new MockContentRepository();
    private final boolean registerTransformers;

    TestModelControllerService31_0_0(final ProcessType processType, final Extension mainExtension, final ControllerInitializer controllerInitializer,
                                     final AdditionalInitialization additionalInit, final RunningModeControl runningModeControl, final ExtensionRegistry extensionRegistry,
                                     final StringConfigurationPersister persister, final ModelTestOperationValidatorFilter validateOpsFilter, final boolean registerTransformers,
                                     final CapabilityRegistry capabilityRegistry, ExpressionResolver expressionResolver) {

        super(processType, additionalInit.getStability(), runningModeControl, extensionRegistry.getTransformerRegistry(), persister, validateOpsFilter,
                ResourceDefinition.builder(ResourceRegistration.of(null, additionalInit.getStability()), NonResolvingResourceDescriptionResolver.INSTANCE).build(),
                expressionResolver, new ControlledProcessState(true), capabilityRegistry, Controller31x.INSTANCE);

        this.mainExtension = mainExtension;
        this.additionalInit = additionalInit;
        this.controllerInitializer = controllerInitializer;
        this.extensionRegistry = extensionRegistry;
        this.runningModeControl = runningModeControl;
        this.registerTransformers = registerTransformers;
    }

    @Override
    protected void initExtraModel(ManagementModel managementModel) {
        Resource rootResource = managementModel.getRootResource();
        ManagementResourceRegistration rootRegistration = managementModel.getRootResourceRegistration();
        rootResource.getModel().get(SUBSYSTEM);

        ManagementResourceRegistration deployments = rootRegistration.registerSubModel(ServerDeploymentResourceDefinition.create(contentRepository, getServerEnvironment()));

        //Hack to be able to access the registry for the jmx facade
        //rootRegistration.registerOperationHandler(RootResourceHack.DEFINITION, RootResourceHack.INSTANCE);

        //extensionRegistry.setSubsystemParentResourceRegistrations(rootRegistration, deployments);
        AdditionalInitializationUtil.doExtraInitialization(additionalInit, controllerInitializer, extensionRegistry, managementModel, this);
    }

    @Override
    protected void initModel(ManagementModel managementModel, Resource modelControllerResource) {
        super.initModel(managementModel, modelControllerResource);
    }

    @Override
    protected void initCoreModel(ManagementModel managementModel, Resource modelControllerResource) {
        super.initCoreModel(managementModel, modelControllerResource);
        // register the global notifications so there is no warning that emitted notifications are not described by the resource.
        GlobalNotifications.registerGlobalNotifications(managementModel.getRootResourceRegistration(), processType);
    }

    @Override
    protected void preBoot(List<ModelNode> bootOperations, boolean rollbackOnRuntimeFailure) {
        mainExtension.initialize(extensionRegistry.getExtensionContext("Test", getRootRegistration(), ExtensionRegistryType.MASTER));
    }


    protected void postBoot() {
        DeployerChainAddHandler.INSTANCE.clearDeployerMap();
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        file.delete();
    }

    public ServerEnvironment getServerEnvironment() {
        Properties props = new Properties();
        File home = new File("target/jbossas");
        delete(home);
        home.mkdir();
        props.put(ServerEnvironment.HOME_DIR, home.getAbsolutePath());

        File standalone = new File(home, "standalone");
        standalone.mkdir();
        props.put(ServerEnvironment.SERVER_BASE_DIR, standalone.getAbsolutePath());

        File configuration = new File(standalone, "configuration");
        configuration.mkdir();
        props.put(ServerEnvironment.SERVER_CONFIG_DIR, configuration.getAbsolutePath());

        File xml = new File(configuration, "standalone.xml");
        try {
            xml.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        props.put(ServerEnvironment.JBOSS_SERVER_DEFAULT_CONFIG, "standalone.xml");
        props.put(ProcessEnvironment.STABILITY, this.additionalInit.getStability().toString());

        ProductConfig productConfig = doPrivileged(this::createProductConfig);

        return new ServerEnvironment(null, props, new HashMap<>(), "standalone.xml", null, LaunchType.STANDALONE, runningModeControl.getRunningMode(), productConfig, false);
    }

    static <T> T doPrivileged(final PrivilegedAction<T> action) {
        return WildFlySecurityManager.isChecking() ? AccessController.doPrivileged(action) : action.run();
    }

    /**
     * Create a product config object with the stability set to the one specified in the additional initialization.
     * We need to use reflection to set the stability field as it is private and there is no setter in the ProductConfig
     * class we shipped in WildFly 31.
     *
     * @return the product config object
     */
    private ProductConfig createProductConfig() {
        ProductConfig productConfig = new ProductConfig("Standalone-31.0.0", "31.0.0", "mail");
        try {
            Field stabilityField = ProductConfig.class.getDeclaredField("defaultStability");
            stabilityField.setAccessible(true);
            stabilityField.set(productConfig, additionalInit.getStability());

            Field stabilitiesField = ProductConfig.class.getDeclaredField("stabilities");
            stabilitiesField.setAccessible(true);

            Set<Stability> stabilitySet = EnumSet.allOf(Stability.class).stream()
                    .filter(additionalInit.getStability()::enables)
                    .collect(Collectors.toUnmodifiableSet());
            stabilitiesField.set(productConfig, stabilitySet);

            return productConfig;
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set stability field for the Product Config", e);
        }
    }

    private static class MockContentRepository implements ContentRepository {

        @Override
        public byte[] addContent(InputStream stream) throws IOException {
            return null;
        }

        @Override
        public VirtualFile getContent(byte[] hash) {
            return null;
        }

        @Override
        public boolean hasContent(byte[] hash) {
            return false;
        }

        @Override
        public boolean syncContent(ContentReference reference) {
            return false;
        }

        @Override
        public void removeContent(ContentReference reference) {
        }

        @Override
        public void addContentReference(ContentReference reference) {
        }

        @Override
        public Map<String, Set<String>> cleanObsoleteContent() {
            return null;
        }
    }
}
