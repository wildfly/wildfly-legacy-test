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
package org.wildfly.legacy.test.controller.core_23_0_0;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.jboss.as.controller.BootErrorCollector;
import org.jboss.as.controller.CapabilityRegistry;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ManagementModel;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.capability.registry.ImmutableCapabilityRegistry;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.resources.DomainRootDefinition;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.host.controller.HostControllerConfigurationPersister;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.as.host.controller.HostModelUtil.HostModelRegistrar;
import org.jboss.as.host.controller.HostPathManagerService;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.mgmt.DomainHostExcludeRegistry;
import org.jboss.as.host.controller.model.host.HostResourceDefinition;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestOperationValidatorFilter;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.repository.ContentReference;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.jboss.as.server.ServerEnvironmentResourceDescription;
import org.jboss.as.server.ServerPathManagerService;
import org.jboss.as.server.controller.resources.ServerRootResourceDefinition;
import org.jboss.as.server.controller.resources.VersionModelInitializer;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * @author Tomaz Cerar
 */
class TestModelControllerService25_0_0 extends ModelTestModelControllerService {

    private final InjectedValue<ContentRepository> injectedContentRepository = new InjectedValue<>();
    private final TestModelType type;
    private final RunningModeControl runningModeControl;
    private final PathManagerService pathManagerService;
    private final ModelInitializer modelInitializer;
    private final DelegatingResourceDefinition rootResourceDefinition;
    private final ControlledProcessState processState;
    private final ExtensionRegistry extensionRegistry;
    private volatile Initializer initializer;
    private final CapabilityRegistry capabilityRegistry;

    TestModelControllerService25_0_0(ProcessType processType, RunningModeControl runningModeControl, StringConfigurationPersister persister, ModelTestOperationValidatorFilter validateOpsFilter,
                                     TestModelType type, ModelInitializer modelInitializer, DelegatingResourceDefinition rootResourceDefinition, ControlledProcessState processState,
                                     ExtensionRegistry extensionRegistry, CapabilityRegistry capabilityRegistry) {
        super(processType, runningModeControl, null, persister, validateOpsFilter, rootResourceDefinition, processState, ExpressionResolver.TEST_RESOLVER, capabilityRegistry, Controller23x.INSTANCE);
        this.type = type;
        this.runningModeControl = runningModeControl;
        this.capabilityRegistry = capabilityRegistry;
        this.pathManagerService = type == TestModelType.STANDALONE ? new ServerPathManagerService(capabilityRegistry) : new HostPathManagerService(capabilityRegistry);
        this.modelInitializer = modelInitializer;
        this.rootResourceDefinition = rootResourceDefinition;
        this.processState = processState;
        this.extensionRegistry = extensionRegistry;

        if (type == TestModelType.STANDALONE) {
            initializer = new ServerInitializer();
        } else if (type == TestModelType.HOST) {
            //Remove the write-local-domain-controller operation since we already simulate that here
            for (Iterator<ModelNode> it = persister.getBootOperations().iterator() ; it.hasNext() ; ) {
                ModelNode op = it.next();
                if (op.get(OP).asString().equals("write-local-domain-controller")) {
                    System.out.println("WARNING: Test framework is removing the 'write-local-domain-controller' operation. If you are comparing xml results use a " +
                             "ModelWriteSanitizer to add the \"domain-controller\" => {\"local\" => {}} part (See ShippedConfigurationsModelTestCase.testHostXml() for an example)");
                    it.remove();
                    break;
                }
            }
            initializer = new HostInitializer();
        } else if (type == TestModelType.DOMAIN) {
            initializer = new DomainInitializer();
        }
    }

    static TestModelControllerService25_0_0 create(ProcessType processType, RunningModeControl runningModeControl, StringConfigurationPersister persister, ModelTestOperationValidatorFilter validateOpsFilter,
                                                   TestModelType type, ModelInitializer modelInitializer, ExtensionRegistry extensionRegistry) {
        CapabilityRegistry capabilityRegistry = new CapabilityRegistry(type == TestModelType.STANDALONE);
        return new TestModelControllerService25_0_0(processType, runningModeControl, persister, validateOpsFilter, type, modelInitializer, new DelegatingResourceDefinition(type), new ControlledProcessState(true), extensionRegistry, capabilityRegistry);
    }

    InjectedValue<ContentRepository> getContentRepositoryInjector(){
        return injectedContentRepository;
    }


    @Override
    public void start(StartContext context) throws StartException {
        if (initializer != null) {
            initializer.setRootResourceDefinitionDelegate();
        }
        super.start(context);
    }

    @Override
    protected void initCoreModel(ManagementModel managementModel, Resource modelControllerResource) {
        //super.initCoreModel(managementModel, modelControllerResource);
        Resource rootResource = managementModel.getRootResource();
        ManagementResourceRegistration rootRegistration = managementModel.getRootResourceRegistration();

        initializer.initCoreModel(rootResource, rootRegistration, modelControllerResource);
        if (modelInitializer != null) {
            modelInitializer.populateModel(managementModel);
        }
    }

    @Override
    public void stop(StopContext context) {
        super.stop(context);
    }

    private ServerEnvironment createStandaloneServerEnvironment() {
        Properties props = new Properties();
        File home = new File("target/jbossas");
        delete(home);
        home.mkdir();
        delay(10);
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

        return new ServerEnvironment(null, props, new HashMap<String, String>(), "standalone.xml", null, LaunchType.STANDALONE, runningModeControl.getRunningMode(), null);
    }

    private HostControllerEnvironment createHostControllerEnvironment() {
        try {
            Map<String, String> props = new HashMap<String, String>();
            File home = new File("target/jbossas");
            delete(home);
            home.mkdir();
            int sleep = 10;
            delay(sleep);
            props.put(HostControllerEnvironment.HOME_DIR, home.getAbsolutePath());

            File domain = new File(home, "domain");
            domain.mkdir();
            delay(sleep);
            props.put(HostControllerEnvironment.DOMAIN_BASE_DIR, domain.getAbsolutePath());

            File configuration = new File(domain, "configuration");
            configuration.mkdir();
            delay(sleep);
            props.put(HostControllerEnvironment.DOMAIN_CONFIG_DIR, configuration.getAbsolutePath());


            boolean isRestart = false;
            String modulePath = "";
            InetAddress processControllerAddress = InetAddress.getLocalHost();
            Integer processControllerPort = 9999;
            InetAddress hostControllerAddress = InetAddress.getLocalHost();
            Integer hostControllerPort = 1234;
            String defaultJVM = null;
            String domainConfig = null;
            String initialDomainConfig = null;
            String hostConfig = null;
            String initialHostConfig = null;
            RunningMode initialRunningMode = runningModeControl.getRunningMode();
            boolean backupDomainFiles = false;
            boolean useCachedDc = false;
            ProductConfig productConfig = new ProductConfig(null, "",  props);
            return new HostControllerEnvironment(props, isRestart, modulePath, processControllerAddress, processControllerPort,
                    hostControllerAddress, hostControllerPort, defaultJVM, domainConfig, initialDomainConfig, hostConfig, initialHostConfig,
                    initialRunningMode, backupDomainFiles, useCachedDc, productConfig);
        } catch (UnknownHostException e) {
            // AutoGenerated
            throw new RuntimeException(e);
        }
    }

    private void delay(int sleep) {
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalHostControllerInfoImpl createLocalHostControllerInfo(HostControllerEnvironment env) {
        return new LocalHostControllerInfoImpl(null, env);
    }

    private HostFileRepository createHostFileRepository() {
        return new HostFileRepository() {

            @Override
            public File[] getDeploymentFiles(ContentReference contentReference) {
                return new File[0];
            }

            @Override
            public File getDeploymentRoot(ContentReference contentReference) {
                return null;
            }

            @Override
            public void deleteDeployment(ContentReference contentReference) {

            }

            @Override
            public File getFile(String relativePath) {
                return null;
            }

            @Override
            public File getConfigurationFile(String relativePath) {
                return null;
            }
        };
    }

    private DomainController createDomainController(final HostControllerEnvironment env, final LocalHostControllerInfoImpl info) {
        return new DomainController() {

            @Override
            public void unregisterRunningServer(String serverName) {
            }

            @Override
            public void unregisterRemoteHost(final String id, Long remoteConnectionId, boolean cleanUnregistration){

            }

            @Override
            public ImmutableCapabilityRegistry getCapabilityRegistry() {
                return null;
            }

            @Override
            public void stopLocalHost(int exitCode) {
            }

            @Override
            public void stopLocalHost() {
            }

            @Override
            public void registerRunningServer(ProxyController serverControllerClient) {
            }


            @Override
            public void pingRemoteHost(String hostName) {
            }

            @Override
            public boolean isHostRegistered(String id) {
                return false;
            }

            @Override
            public HostFileRepository getRemoteFileRepository() {
                return null;
            }

            @Override
            public ModelNode getProfileOperations(String profileName) {
                return null;
            }

            @Override
            public LocalHostControllerInfo getLocalHostInfo() {
                return info;
            }

            @Override
            public HostFileRepository getLocalFileRepository() {
                return null;
            }

            @Override
            public ExtensionRegistry getExtensionRegistry() {
                return null;
            }

            @Override
            public RunningMode getCurrentRunningMode() {
                return null;
            }

            @Override
            public ExpressionResolver getExpressionResolver() {
                return null;
            }

            @Override
            public void initializeMasterDomainRegistry(ManagementResourceRegistration root,
                    ExtensibleConfigurationPersister configurationPersister, ContentRepository contentRepository,
                    HostFileRepository fileRepository, ExtensionRegistry extensionRegistry, PathManagerService pathManager) {
            }

            @Override
            public void initializeSlaveDomainRegistry(ManagementResourceRegistration root,
                    ExtensibleConfigurationPersister configurationPersister, ContentRepository contentRepository,
                    HostFileRepository fileRepository, LocalHostControllerInfo hostControllerInfo,
                    ExtensionRegistry extensionRegistry, IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                    PathManagerService pathManager) {
            }

            @Override
            public void registerRemoteHost(String string, ManagementChannelHandler mch, Transformers t, Long l, boolean bln) throws SlaveRegistrationException {
            }
        };
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        file.delete();
    }

    private interface Initializer {
        void setRootResourceDefinitionDelegate();
        void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration, Resource modelControllerResource);
    }

    private class ServerInitializer implements Initializer {
        final ExtensibleConfigurationPersister persister = new NullConfigurationPersister();
        final ServerEnvironment environment = createStandaloneServerEnvironment();
        final boolean parallelBoot = false;

        public void setRootResourceDefinitionDelegate() {
            rootResourceDefinition.setDelegate(new ServerRootResourceDefinition(
                    injectedContentRepository.getValue(),
                    persister,
                    environment,
                    processState,
                    runningModeControl,
                    extensionRegistry,
                    parallelBoot,
                    pathManagerService,
                    null,
                    authorizer,
                    null,
                    AuditLogger.NO_OP_LOGGER,
                    getMutableRootResourceRegistrationProvider(),
                    BOOT_ERROR_COLLECTOR,
                    capabilityRegistry));
        }

        @Override
        public void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration, Resource modelControllerResource) {
            VersionModelInitializer.registerRootResource(rootResource, null);
            Resource managementResource = Resource.Factory.create();
            rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT), managementResource);
            rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.SERVICE_CONTAINER), Resource.Factory.create());
            rootResource.registerChild(ServerEnvironmentResourceDescription.RESOURCE_PATH, Resource.Factory.create());
            pathManagerService.addPathManagerResources(rootResource);
        }
    }

    private class HostInitializer implements Initializer {
        final String hostName = "master";
        final HostControllerEnvironment env = createHostControllerEnvironment();
        final LocalHostControllerInfoImpl info = createLocalHostControllerInfo(env);
        final IgnoredDomainResourceRegistry ignoredRegistry = new IgnoredDomainResourceRegistry(info);
        final HostControllerConfigurationPersister persister = new HostControllerConfigurationPersister(env, info, Executors.newCachedThreadPool(), extensionRegistry, extensionRegistry);
        final HostFileRepository hostFileRepository = createHostFileRepository();
        final DomainController domainController = createDomainController(env, info);

        @Override
        public void setRootResourceDefinitionDelegate() {
            rootResourceDefinition.setDelegate(
                    new HostResourceDefinition(
                            hostName,
                            persister,
                            env,
                            (HostRunningModeControl)runningModeControl,
                            hostFileRepository,
                            info,
                            null /*serverInventory*/,
                            null /*remoteFileRepository*/,
                            injectedContentRepository.getValue(),
                            domainController,
                            extensionRegistry,
                            ignoredRegistry,
                            processState,
                            pathManagerService,
                            authorizer,
                            null,
                            AuditLogger.NO_OP_LOGGER,
                            BOOT_ERROR_COLLECTOR));
        }

        @Override
        public void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration, Resource modelControllerResource) {
            HostModelUtil.createRootRegistry(
                    rootRegistration,
                    env,
                    ignoredRegistry,
                    new HostModelRegistrar() {
                        @Override
                        public void registerHostModel(String hostName, ManagementResourceRegistration rootRegistration) {
                        }
                    },
                    ProcessType.HOST_CONTROLLER,
                    authorizer,
                    modelControllerResource,
                    new LocalHostControllerInfoImpl(processState, "host"),
                    capabilityRegistry);

            HostModelUtil.createHostRegistry(
                    hostName,
                    rootRegistration,
                    persister,
                    env,
                    (HostRunningModeControl)runningModeControl,
                    hostFileRepository,
                    info,
                    null /*serverInventory*/,
                    null /*remoteFileRepository*/,
                    injectedContentRepository.getValue(),
                    domainController,
                    extensionRegistry,
                    extensionRegistry,
                    ignoredRegistry,
                    processState,
                    pathManagerService,
                    authorizer,
                    null,
                    AuditLogger.NO_OP_LOGGER,
                    BOOT_ERROR_COLLECTOR);
        }
    }

    private class DomainInitializer implements Initializer {

        @Override
        public void setRootResourceDefinitionDelegate() {
        }

        @Override
        public void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration, Resource modelControllerResource) {
            VersionModelInitializer.registerRootResource(rootResource, null);
            final HostControllerEnvironment env = createHostControllerEnvironment();
            final LocalHostControllerInfoImpl info = createLocalHostControllerInfo(env);
            final IgnoredDomainResourceRegistry ignoredRegistry = new IgnoredDomainResourceRegistry(info);
            final ExtensibleConfigurationPersister persister = new NullConfigurationPersister();
            final HostFileRepository hostFileRepository = createHostFileRepository();
            final DomainController domainController = createDomainController(env, info);
            final DomainHostExcludeRegistry domainHostExcludeRegistry = new DomainHostExcludeRegistry();

            DomainRootDefinition domainDefinition = new DomainRootDefinition(
                    domainController,
                    env,
                    persister,
                    injectedContentRepository.getValue(),
                    hostFileRepository,
                    true,
                    info,
                    extensionRegistry,
                    null,
                    pathManagerService,
                    authorizer,
                    null,
                    null,
                    domainHostExcludeRegistry,
                    getMutableRootResourceRegistrationProvider());
            domainDefinition.initialize(rootRegistration);
            rootResourceDefinition.setDelegate(domainDefinition);

            HostModelUtil.createRootRegistry(
                    rootRegistration,
                    env, ignoredRegistry,
                    (hostName, root) -> {},
                    processType,
                    authorizer,
                    modelControllerResource,
                    new LocalHostControllerInfoImpl(processState, "host"),
                    capabilityRegistry);
            CoreManagementResourceDefinition.registerDomainResource(rootResource, null);
        }

    }

    static class DelegatingResourceDefinition extends org.jboss.as.controller.DelegatingResourceDefinition {
        private final TestModelType type;

        public DelegatingResourceDefinition(TestModelType type) {
            this.type = type;
        }

        @Override
        public void setDelegate(ResourceDefinition delegate) {
            super.setDelegate(delegate);
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            if (type == TestModelType.DOMAIN) {
                return;
            }
            super.registerOperations(resourceRegistration);
        }

        @Override
        public void registerChildren(ManagementResourceRegistration resourceRegistration) {
            if (type == TestModelType.DOMAIN) {
                return;
            }
            super.registerChildren(resourceRegistration);
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            if (type == TestModelType.DOMAIN) {
                return;
            }
            super.registerAttributes(resourceRegistration);
        }

        @Override
        public void registerNotifications(ManagementResourceRegistration resourceRegistration) {
            if (type == TestModelType.DOMAIN) {
                return;
            }
            super.registerNotifications(resourceRegistration);
        }

        @Override
        public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
            if (type == TestModelType.DOMAIN) {
                return;
            }
            super.registerCapabilities(resourceRegistration);
        }

        @Override
        public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
            if (type == TestModelType.DOMAIN) {
                return;
            }
            super.registerAdditionalRuntimePackages(resourceRegistration);
        }
    }

    private static BootErrorCollector BOOT_ERROR_COLLECTOR = new BootErrorCollector();
}
