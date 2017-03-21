/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017 MicroBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.maven.cdi;

import java.io.File;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.Dependent;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.literal.InjectLiteral;
import javax.enterprise.inject.literal.SingletonLiteral;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import javax.enterprise.util.AnnotationLiteral;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.apache.maven.model.Model;

import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

import org.apache.maven.model.composition.DefaultDependencyManagementImporter;
import org.apache.maven.model.inheritance.DefaultInheritanceAssembler;

import org.apache.maven.model.interpolation.StringSearchModelInterpolator;

import org.apache.maven.model.io.DefaultModelReader;

import org.apache.maven.model.locator.DefaultModelLocator;

import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DefaultPluginManagementInjector;

import org.apache.maven.model.normalization.DefaultModelNormalizer;

import org.apache.maven.model.path.DefaultModelPathTranslator;
import org.apache.maven.model.path.DefaultModelUrlNormalizer;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.DefaultUrlNormalizer;

import org.apache.maven.model.plugin.DefaultPluginConfigurationExpander;
import org.apache.maven.model.plugin.DefaultReportConfigurationExpander;
import org.apache.maven.model.plugin.DefaultReportingConverter;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;

import org.apache.maven.model.profile.DefaultProfileInjector;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileSelector;

import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;

import org.apache.maven.model.superpom.DefaultSuperPomProvider;

import org.apache.maven.model.validation.DefaultModelValidator;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;

import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;

import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;

import org.apache.maven.settings.validation.DefaultSettingsValidator;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifactType;

import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;

import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;

import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.LocalRepositoryProvider;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;

import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider;
import org.eclipse.aether.internal.impl.DefaultDependencyCollector;
import org.eclipse.aether.internal.impl.DefaultDeployer;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider;
import org.eclipse.aether.internal.impl.DefaultMetadataResolver;
import org.eclipse.aether.internal.impl.DefaultOfflineController;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositoryConnectorProvider;
import org.eclipse.aether.internal.impl.DefaultRepositoryEventDispatcher;
import org.eclipse.aether.internal.impl.DefaultRepositoryLayoutProvider;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.internal.impl.DefaultSyncContextFactory;
import org.eclipse.aether.internal.impl.DefaultTransporterProvider;
import org.eclipse.aether.internal.impl.DefaultUpdateCheckManager;
import org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.Maven2RepositoryLayoutFactory;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;

import org.eclipse.aether.internal.impl.slf4j.Slf4jLoggerFactory;

import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RemoteRepository;

import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;

import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;

import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;

import org.eclipse.aether.spi.connector.transport.TransporterFactory;

import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;

import org.eclipse.aether.transfer.TransferListener;

import org.eclipse.aether.transport.file.FileTransporterFactory;

import org.eclipse.aether.transport.http.HttpTransporterFactory;

import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;

import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;

import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;

import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;

import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;

import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

import org.microbean.maven.cdi.annotation.Resolution;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public class MavenExtension implements Extension {

  public MavenExtension() {
    super();
  }

  private final void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery event) {
    if (event != null) {

      event.addQualifier(Hinted.class);

      //
      // Types effectively bound by DefaultServiceLocator
      //
      
      event.addAnnotatedType(DefaultArtifactResolver.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultChecksumPolicyProvider.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultDependencyCollector.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultDeployer.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultFileProcessor.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultInstaller.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultLocalRepositoryProvider.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultMetadataResolver.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultOfflineController.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultRemoteRepositoryManager.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultRepositoryConnectorProvider.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultRepositoryEventDispatcher.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultRepositoryLayoutProvider.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultRepositorySystem.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultSyncContextFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultTransporterProvider.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultUpdateCheckManager.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultUpdatePolicyAnalyzer.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(EnhancedLocalRepositoryManagerFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(Maven2RepositoryLayoutFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(SimpleLocalRepositoryManagerFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(Slf4jLoggerFactory.class, "maven");

      //
      // Types effectively bound by MavenRepositorySystemUtils
      //
      
      event.addAnnotatedType(ClassicDependencyManager.class, "maven").add(SingletonLiteral.INSTANCE);
      // See https://issues.apache.org/jira/browse/MNG-6190; when this
      // is fixed, this line can be restored and the relevant producer
      // method (see below) removed.
      // event.addAnnotatedType(DefaultArtifactDescriptorReader.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultVersionRangeResolver.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultVersionResolver.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(FatArtifactTraverser.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(SnapshotMetadataGeneratorFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(VersionsMetadataGeneratorFactory.class, "maven").add(SingletonLiteral.INSTANCE);

      //
      // Types effectively bound by DefaultModelBuilderFactory
      //
      
      event.addAnnotatedType(DefaultDependencyManagementImporter.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultDependencyManagementInjector.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultInheritanceAssembler.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultModelBuilder.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultModelLocator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultModelNormalizer.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultModelPathTranslator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultModelProcessor.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultModelReader.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultModelUrlNormalizer.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultModelValidator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultPathTranslator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultPluginConfigurationExpander.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultPluginManagementInjector.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultProfileInjector.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultReportConfigurationExpander.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultReportingConverter.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultSuperPomProvider.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultUrlNormalizer.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(FileProfileActivator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(JdkVersionProfileActivator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(OperatingSystemProfileActivator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(PropertyProfileActivator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(StringSearchModelInterpolator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(StubLifecycleBindingsInjector.class, "maven").add(SingletonLiteral.INSTANCE);

      //
      // Types bound by me :-)
      //

      // Somehow, the Producers nested class below is automatically
      // discovered.  I think this is a bug.  If it is fixed, then
      // uncomment this line.
      // event.addAnnotatedType(Producers.class, "maven");
      event.addAnnotatedType(BasicRepositoryConnectorFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultSettingsBuilder.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultSettingsReader.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultSettingsValidator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultSettingsWriter.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(FileTransporterFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(HttpTransporterFactory.class, "maven").add(SingletonLiteral.INSTANCE);      

    }
  }

  private final <X> void processPlexusRequirementAnnotatedMembers(@Observes @WithAnnotations(Requirement.class) final ProcessAnnotatedType<X> event) {
    if (event != null) {
      final AnnotatedType<X> type = event.getAnnotatedType();
      if (type != null && type.getConstructors().stream().noneMatch(c -> c.isAnnotationPresent(Inject.class))) {
        event.configureAnnotatedType()
          .filterFields(f -> {
              return f != null && f.isAnnotationPresent(Requirement.class) && !f.isAnnotationPresent(Inject.class);
            })
          .forEach(fc -> {
              fc.add(InjectLiteral.INSTANCE);
              final AnnotatedField<?> f = fc.getAnnotated();
              if (f != null && !f.isAnnotationPresent(Hinted.class)) {
                final Requirement requirement = f.getAnnotation(Requirement.class);
                if (requirement != null) {
                  final String hint = requirement.hint();
                  if (hint != null && !hint.trim().isEmpty()) {
                    fc.add(Hinted.Literal.of(hint));
                  }
                }
              }
            });
      }
    }
  }

  private final <X> void processPlexusComponentAnnotatedTypes(@Observes @WithAnnotations(Component.class) final ProcessAnnotatedType<X> event) {
    if (event != null) {
      final AnnotatedType<X> type = event.getAnnotatedType();
      if (type != null && !type.isAnnotationPresent(Typed.class)) {
        final Component component = type.getAnnotation(Component.class);
        if (component != null) {
          final AnnotatedTypeConfigurator<X> atc = event.configureAnnotatedType();
          if (atc != null) {
            final Class<?> role = component.role();
            if (role != null && !role.equals(Object.class)) {
              atc.add(Typed.Literal.of(new Class<?>[] { role, Object.class }));
            }
            final String hint = component.hint();
            if (hint != null && !hint.isEmpty() && !type.isAnnotationPresent(Hinted.class)) {
              atc.add(Hinted.Literal.of(hint));
            }
          }
        }
      }
    }
  }


  /*
   * Inner and nested classes.
   */
  

  private static final class Producers {

    @Produces
    @Singleton
    private static final MirrorSelector produceMirrorSelector(final Settings settings) {
      final DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
      if (settings != null) {
        final Collection<? extends Mirror> mirrors = settings.getMirrors();
        if (mirrors != null && !mirrors.isEmpty()) {
          for (final Mirror mirror : mirrors) {
            assert mirror != null;
            mirrorSelector.add(mirror.getId(),
                               mirror.getUrl(),
                               mirror.getLayout(),
                               false, /* not a repository manager; settings.xml does not encode this information */
                               mirror.getMirrorOf(),
                               mirror.getMirrorOfLayouts());
          }
        }
      }
      return mirrorSelector;
    }
    
    @Produces
    @Singleton
    private static final Settings produceSettings(final SettingsBuilder settingsBuilder) throws SettingsBuildingException {
      Objects.requireNonNull(settingsBuilder);
      final DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
      final Properties requestSystemProperties = new Properties();
      final Properties systemProperties = System.getProperties();
      synchronized (systemProperties) {
        final Set<String> keys = systemProperties.stringPropertyNames();
        if (keys != null) {
          for (final String key : keys) {
            assert key != null;
            requestSystemProperties.setProperty(key, systemProperties.getProperty(key));
          }
        }
      }
      request.setSystemProperties(requestSystemProperties);
      // request.setUserProperties(userProperties); // TODO: implement this
      // request.setGlobalSettingsFile(new File("/usr/local/maven/conf/settings.xml")); // TODO: do this for real
      request.setUserSettingsFile(new File(new File(System.getProperty("user.home")), ".m2/settings.xml")); // TODO: from configuration, too
      final SettingsBuildingResult settingsBuildingResult = settingsBuilder.build(request);
      assert settingsBuildingResult != null;
      final List<SettingsProblem> settingsBuildingProblems = settingsBuildingResult.getProblems();
      if (settingsBuildingProblems != null && !settingsBuildingProblems.isEmpty()) {
        // It is guaranteed that these problems will contain warnings
        // only, but that's still a problem!
        throw new SettingsBuildingException(settingsBuildingProblems);
      }
      final Settings returnValue = settingsBuildingResult.getEffectiveSettings();
      return returnValue;
    }
    
    @Produces
    @Singleton
    private static final ProfileSelector produceProfileSelector(@Hinted("jdk-version") final ProfileActivator jdkVersionProfileActivator,
                                                                @Hinted("os") final ProfileActivator operatingSystemProfileActivator,
                                                                @Hinted("property") final ProfileActivator fileProfileActivator,
                                                                @Hinted("file") final ProfileActivator propertyProfileActivator) {
      final DefaultProfileSelector returnValue = new DefaultProfileSelector();
      returnValue.addProfileActivator(jdkVersionProfileActivator);
      returnValue.addProfileActivator(operatingSystemProfileActivator);
      returnValue.addProfileActivator(propertyProfileActivator);
      returnValue.addProfileActivator(fileProfileActivator); // TODO: make sure fileProfileActivator.pathTranslator is non-null (i.e. injection worked)
      return returnValue;
    }

    @Produces
    @Singleton
    private static final ILoggerFactory produceILoggerFactory() {
      return LoggerFactory.getILoggerFactory();
    }

    @Produces
    @Singleton
    private static final LocalRepository produceLocalRepository(final Settings settings) {
      Objects.requireNonNull(settings);
      String localRepositoryString = settings.getLocalRepository();
      if (localRepositoryString == null) {
        localRepositoryString = System.getProperty("user.home") + "/.m2/repository";
      }
      final LocalRepository returnValue = new LocalRepository(localRepositoryString);
      return returnValue;
    }

    @Produces
    @Dependent
    @Resolution
    private static final List<RemoteRepository> produceRemoteRepositoryList(final Settings settings,
                                                                            final RepositorySystem repositorySystem,
                                                                            final RepositorySystemSession session) {
      Objects.requireNonNull(settings);
      Objects.requireNonNull(repositorySystem);
      List<RemoteRepository> remoteRepositories = new ArrayList<>();
      final Map<String, Profile> profiles = settings.getProfilesAsMap();
      if (profiles != null && !profiles.isEmpty()) {
        final Collection<String> activeProfileKeys = settings.getActiveProfiles();
        if (activeProfileKeys != null && !activeProfileKeys.isEmpty()) {
          for (final String activeProfileKey : activeProfileKeys) {
            final Profile activeProfile = profiles.get(activeProfileKey);
            if (activeProfile != null) {
              final Collection<Repository> repositories = activeProfile.getRepositories();
              if (repositories != null && !repositories.isEmpty()) {
                for (final Repository repository : repositories) {
                  if (repository != null) {
                    RemoteRepository.Builder builder = new RemoteRepository.Builder(repository.getId(), repository.getLayout(), repository.getUrl());
                    
                    final org.apache.maven.settings.RepositoryPolicy settingsReleasePolicy = repository.getReleases();
                    if (settingsReleasePolicy != null) {
                      final org.eclipse.aether.repository.RepositoryPolicy releasePolicy = new org.eclipse.aether.repository.RepositoryPolicy(settingsReleasePolicy.isEnabled(), settingsReleasePolicy.getUpdatePolicy(), settingsReleasePolicy.getChecksumPolicy());
                      builder = builder.setReleasePolicy(releasePolicy);
                    }
                    
                    final org.apache.maven.settings.RepositoryPolicy settingsSnapshotPolicy = repository.getSnapshots();
                    if (settingsSnapshotPolicy != null) {
                      final org.eclipse.aether.repository.RepositoryPolicy snapshotPolicy = new org.eclipse.aether.repository.RepositoryPolicy(settingsSnapshotPolicy.isEnabled(), settingsSnapshotPolicy.getUpdatePolicy(), settingsSnapshotPolicy.getChecksumPolicy());
                      builder = builder.setSnapshotPolicy(snapshotPolicy);
                    }
                    
                    final RemoteRepository remoteRepository = builder.build();
                    assert remoteRepository != null;
                    remoteRepositories.add(remoteRepository);
                  }
                }
              }
            }
          }
        }
      }
      final RemoteRepository mavenCentral = new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
      assert mavenCentral != null;
      remoteRepositories.add(mavenCentral);
      remoteRepositories = repositorySystem.newResolutionRepositories(session, remoteRepositories);
      assert remoteRepositories != null;
      return remoteRepositories;
    }

    @Produces
    @Dependent
    private static final RepositorySystemSession produceRepositorySystemSession(final DependencyTraverser dependencyTraverser,
                                                                                final DependencyManager dependencyManager,
                                                                                final DependencySelector dependencySelector,
                                                                                final DependencyGraphTransformer dependencyGraphTransformer,
                                                                                final ArtifactTypeRegistry artifactTypeRegistry,
                                                                                final ArtifactDescriptorPolicy artifactDescriptorPolicy,
                                                                                final Settings settings,
                                                                                final MirrorSelector mirrorSelector,
                                                                                final LocalRepository localRepository,
                                                                                final LocalRepositoryProvider localRepositoryProvider,
                                                                                final Instance<TransferListener> transferListenerInstance)
    throws NoLocalRepositoryManagerException {
      final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
      session.setDependencyTraverser(dependencyTraverser);
      session.setDependencyManager(dependencyManager);
      session.setDependencySelector(dependencySelector);
      session.setDependencyGraphTransformer(dependencyGraphTransformer);
      session.setArtifactTypeRegistry(artifactTypeRegistry);
      session.setArtifactDescriptorPolicy(artifactDescriptorPolicy);
      final Properties sessionSystemProperties = new Properties();
      final Properties systemProperties = System.getProperties();
      synchronized (systemProperties) {
        final Set<String> keys = systemProperties.stringPropertyNames();
        if (keys != null) {
          for (final String key : keys) {
            assert key != null;
            sessionSystemProperties.setProperty(key, systemProperties.getProperty(key));
          }
        }
      }
      session.setSystemProperties(sessionSystemProperties);
      session.setConfigProperties(sessionSystemProperties);
      if (settings != null) {
        session.setOffline(settings.isOffline());
      }
      session.setMirrorSelector(mirrorSelector);
      final LocalRepositoryManager localRepositoryManager = localRepositoryProvider.newLocalRepositoryManager(session, localRepository);
      session.setLocalRepositoryManager(localRepositoryManager);
      if (transferListenerInstance != null && transferListenerInstance.isResolvable()) {
        session.setTransferListener(transferListenerInstance.get());
      }
      return session;
    }

    @Produces
    @Singleton
    private static final DependencySelector produceDependencySelector() {
      return new AndDependencySelector(new ScopeDependencySelector("test", "provided"),
                                       new OptionalDependencySelector(),
                                       new ExclusionDependencySelector());
    }

    @Produces
    @Singleton
    private static final DependencyGraphTransformer produceDependencyGraphTransformer() {
      return new ChainedDependencyGraphTransformer(new ConflictResolver(new NearestVersionSelector(),
                                                                        new JavaScopeSelector(),
                                                                        new SimpleOptionalitySelector(),
                                                                        new JavaScopeDeriver()),
                                                   new JavaDependencyContextRefiner());
    }

    @Produces
    @Singleton
    private static final ArtifactTypeRegistry produceArtifactTypeRegistry() {
      final DefaultArtifactTypeRegistry stereotypes = new DefaultArtifactTypeRegistry();
      stereotypes.add(new DefaultArtifactType("pom"));
      stereotypes.add(new DefaultArtifactType("maven-plugin", "jar", "", "java"));
      stereotypes.add(new DefaultArtifactType("jar", "jar", "", "java"));
      stereotypes.add(new DefaultArtifactType("ejb", "jar", "", "java"));
      stereotypes.add(new DefaultArtifactType("ejb-client", "jar", "client", "java"));
      stereotypes.add(new DefaultArtifactType("test-jar", "jar", "tests", "java"));
      stereotypes.add(new DefaultArtifactType("javadoc", "jar", "javadoc", "java"));
      stereotypes.add(new DefaultArtifactType("java-source", "jar", "sources", "java", false, false));
      stereotypes.add(new DefaultArtifactType("war", "war", "", "java", false, true));
      stereotypes.add(new DefaultArtifactType("ear", "ear", "", "java", false, true));
      stereotypes.add(new DefaultArtifactType("rar", "rar", "", "java", false, true));
      stereotypes.add(new DefaultArtifactType("par", "par", "", "java", false, true));
      return stereotypes;
    }

    @Produces
    @Singleton
    private static final ArtifactDescriptorPolicy produceArtifactDescriptorPolicy() {
      return new SimpleArtifactDescriptorPolicy(true /* ignoreMissing */, true /* ignoreInvalid */);
    }

    /**
     * Returns a {@link DefaultArtifactDescriptorReader} in {@link
     * Singleton} scope.
     *
     * <h2>Implementation Notes</h2>
     *
     * <p>This method is only necessary until <a
     * href="https://issues.apache.org/jira/browse/MNG-6190">MNG-6190</a>
     * is fixed and can then be removed in favor of a simple {@link
     * ProcessAnnotatedType#addAnnotatedType(Class)} invocation.</p>
     *
     * @param remoteRepositoryManager the {@link
     * RemoteRepositoryManager} to build the {@link
     * DefaultArtifactDescriptorReader} with; must not be {@code null}
     *
     * @param versionResolver the {@link VersionResolver} to build the
     * {@link DefaultArtifactDescriptorReader} with; must not be
     * {@code null}
     *
     * @param versionRangeResolver the {@link VersionRangeResolver} to
     * build the {@link DefaultArtifactDescriptorReader} with; must
     * not be {@code null}
     *
     * @param artifactResolver the {@link ArtifactResolver} to build
     * the {@link DefaultArtifactDescriptorReader} with; must not be
     * {@code null}
     *
     * @param modelBuilder the {@link ModelBuilder} to build the
     * {@link DefaultArtifactDescriptorReader} with; must not be
     * {@code null}
     *
     * @param repositoryEventDispatcher the {@link
     * RepositoryEventDispatcher} to build the {@link
     * DefaultArtifactDescriptorReader} with; must not be {@code null}
     *
     * @param loggerFactory the {@link
     * org.eclipse.aether.spi.log.LoggerFactory} to build the {@link
     * DefaultArtifactDescriptorReader} with; must not be {@code null}
     *
     * @return a new {@link DefaultArtifactDescriptorReader}; never
     * {@code null}
     *
     * @exception NullPointerException if any parameter is {@code
     * null}
     *
     * @see <a href="https://issues.apache.org/jira/browse/MNG-6190">MNG-6190</a>
     */
    @Produces
    @Singleton
    private static final ArtifactDescriptorReader produceArtifactDescriptorReader(final RemoteRepositoryManager remoteRepositoryManager,
                                                                                  final VersionResolver versionResolver,
                                                                                  final VersionRangeResolver versionRangeResolver,
                                                                                  final ArtifactResolver artifactResolver,
                                                                                  final ModelBuilder modelBuilder,
                                                                                  final RepositoryEventDispatcher repositoryEventDispatcher,
                                                                                  final org.eclipse.aether.spi.log.LoggerFactory loggerFactory) {
      final DefaultArtifactDescriptorReader returnValue = new DefaultArtifactDescriptorReader();
      returnValue.setRemoteRepositoryManager(remoteRepositoryManager);
      returnValue.setVersionResolver(versionResolver);
      returnValue.setVersionRangeResolver(versionRangeResolver);
      returnValue.setArtifactResolver(artifactResolver);
      returnValue.setModelBuilder(modelBuilder);
      returnValue.setRepositoryEventDispatcher(repositoryEventDispatcher);
      returnValue.setLoggerFactory(loggerFactory);
      return returnValue;
    }


    /*
     * Sets of things.
     */


    @Produces
    @Dependent
    private static final Set<LocalRepositoryManagerFactory> produceLocalRepositoryManagerFactorySet(@Any final Instance<LocalRepositoryManagerFactory> localRepositoryManagerFactories) {
      final Set<LocalRepositoryManagerFactory> returnValue = produceSet(localRepositoryManagerFactories);
      return returnValue;
    }

    @Produces
    @Dependent
    private static final Set<RepositoryLayoutFactory> produceRepositoryLayoutFactorySet(@Any final Instance<RepositoryLayoutFactory> repositoryLayoutFactories) {
      return produceSet(repositoryLayoutFactories);
    }

    @Produces
    @Dependent
    private static final Set<TransporterFactory> produceTransporterFactorySet(@Any final Instance<TransporterFactory> transporterFactories) {
      return produceSet(transporterFactories);
    }
    
    @Produces
    @Dependent
    private static final Set<RepositoryListener> produceRepositoryListenerSet(@Any final Instance<RepositoryListener> repositoryListeners) {
      return produceSet(repositoryListeners);
    }

    @Produces
    @Dependent
    private static final Set<RepositoryConnectorFactory> produceRepositoryConnectorFactorySet(@Any final Instance<RepositoryConnectorFactory> repositoryConnectorFactories) {
      return produceSet(repositoryConnectorFactories);
    }

    @Produces
    @Dependent
    private static final Set<MetadataGeneratorFactory> produceMetadataGeneratorFactorySet(@Any final Instance<MetadataGeneratorFactory> metadataGeneratorFactories) {
      return produceSet(metadataGeneratorFactories);
    }

    private static final <T> Set<T> produceSet(final Instance<? extends T> things) {
      final Set<T> returnValue = new HashSet<>();
      if (things != null) {
        for (final T thing : things) {
          returnValue.add(thing);
        }
      }
      return returnValue;
    }

  }

  /**
   * A {@link LifecycleBindingsInjector} that does nothing.
   *
   * @author <a href="http://about.me/lairdnelson/"
   * target="_parent">Laird Nelson</a>
   *
   * @see LifecycleBindingsInjector
   */
  private static final class StubLifecycleBindingsInjector implements LifecycleBindingsInjector {

    
    /*
     * Instance methods.
     */

    
    /**
     * Does nothing when invoked.
     *
     * @param model ignored
     *
     * @param modelBuildingRequest ignored
     *
     * @param modelProblemCollector ignored
     */
    @Override
    public final void injectLifecycleBindings(final Model model, final ModelBuildingRequest modelBuildingRequest, final ModelProblemCollector modelProblemCollector) {

    }
    
  }

  /**
   * A {@link Qualifier} used to represent the {@link
   * Requirement#hint() hint} element of the {@link Requirement}
   * annotation.
   *
   * @author <a href="http://about.me/lairdnelson/"
   * target="_parent">Laird Nelson</a>
   *
   * @see Requirement
   */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
  private static @interface Hinted {


    /*
     * Annotation elements.
     */

    
    /**
     * The value of the hint.
     */
    String value() default "";


    /*
     * Inner and nested classes.
     */

    
    /**
     * An {@link AnnotationLiteral} that represents an instance of the
     * {@link Hinted} annotation.
     *
     * @author <a href="http://about.me/lairdnelson/"
     * target="_parent">Laird Nelson</a>
     *
     * @see Hinted
     */
    static final class Literal extends AnnotationLiteral<Hinted> implements Hinted {


      /*
       * Static fields.
       */

      
      /**
       * The version of this class for {@linkplain Serializable
       * serialization purposes}.
       */
      private static final long serialVersionUID = 1L;
      
      
      /*
       * Instance fields.
       */

      
      /**
       * The hint being represented.
       *
       * <p>This field is never {@code null}.</p>
       *
       * @see #Literal(String)
       */
      private final String value;


      /*
       * Constructors.
       */


      /**
       * Creates a new {@link Literal}.
       *
       * @param value the value of the hint; may be {@code null} in
       * which case the empty {@link String} will be used instad
       *
       * @see #value()
       */
      private Literal(final String value) {
        super();
        this.value = value == null ? "" : value;
      }


      /*
       * Instance methods.
       */
      

      /**
       * Returns the value of this {@link Literal}.
       *
       * <p>This method never returns {@code null}.</p>
       *
       * @return the value of this {@link Literal}; never {@code null}
       */
      @Override
      public final String value() {
        return this.value;
      }

      /**
       * Returns a new {@link Hinted} representing the supplied {@code
       * value}.
       *
       * <p>This method never returns {@code null}.</p>
       *
       * @param value the value to represent; may be {@code null} in
       * which case the empty {@link String} will be used instead
       *
       * @return a {@link Hinted}; never {@code null}
       */
      private static final Hinted of(final String value) {
        return new Literal(value);
      }
      
    }
    
  }
  
}
