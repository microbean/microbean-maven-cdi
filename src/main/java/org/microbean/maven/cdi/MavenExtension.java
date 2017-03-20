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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.Dependent;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.literal.InjectLiteral;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.literal.SingletonLiteral;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;

import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProcessor;

import org.apache.maven.model.composition.DefaultDependencyManagementImporter;
import org.apache.maven.model.composition.DependencyManagementImporter;

import org.apache.maven.model.locator.DefaultModelLocator;
import org.apache.maven.model.locator.ModelLocator;

import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.inheritance.DefaultInheritanceAssembler;

import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.interpolation.StringSearchModelInterpolator;

import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.DefaultModelReader;

import org.apache.maven.model.locator.DefaultModelLocator;
import org.apache.maven.model.locator.ModelLocator;

import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DefaultPluginManagementInjector;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.apache.maven.model.management.PluginManagementInjector;

import org.apache.maven.model.normalization.DefaultModelNormalizer;
import org.apache.maven.model.normalization.ModelNormalizer;

import org.apache.maven.model.path.DefaultModelPathTranslator;
import org.apache.maven.model.path.DefaultModelUrlNormalizer;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.DefaultUrlNormalizer;
import org.apache.maven.model.path.ModelPathTranslator;
import org.apache.maven.model.path.ModelUrlNormalizer;
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.path.UrlNormalizer;

import org.apache.maven.model.plugin.DefaultPluginConfigurationExpander;
import org.apache.maven.model.plugin.DefaultReportConfigurationExpander;
import org.apache.maven.model.plugin.DefaultReportingConverter;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;
import org.apache.maven.model.plugin.PluginConfigurationExpander;
import org.apache.maven.model.plugin.ReportConfigurationExpander;
import org.apache.maven.model.plugin.ReportingConverter;

import org.apache.maven.model.profile.DefaultProfileInjector;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileInjector;
import org.apache.maven.model.profile.ProfileSelector;

import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;

import org.apache.maven.model.superpom.DefaultSuperPomProvider;
import org.apache.maven.model.superpom.SuperPomProvider;

import org.apache.maven.model.validation.DefaultModelValidator;
import org.apache.maven.model.validation.ModelValidator;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;

import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifactType;

import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;

import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;

import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
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

import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;

import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;

import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;

import org.eclipse.aether.spi.connector.transport.TransporterFactory;

import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;

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

import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public class MavenExtension implements Extension {

  public MavenExtension() {
    super();
  }

  /*
   * TODO:
   *
   * * process all beans; turn injection points anotated with @Requirement into @Inject
   *
   * * process all beans; turn methods annotated with @Provides into @Produces
   *
   * * process all beans; turn com.google.inject.Named annotations into javax.inject.Named annotations?
   */

  private final void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery event) {
    if (event != null) {

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
      event.addAnnotatedType(DefaultArtifactDescriptorReader.class, "maven").add(SingletonLiteral.INSTANCE);
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
      event.addAnnotatedType(StubLifecycleBindingsInjector.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(JdkVersionProfileActivator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(OperatingSystemProfileActivator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(FileProfileActivator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(PropertyProfileActivator.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(StringSearchModelInterpolator.class, "maven").add(SingletonLiteral.INSTANCE);

      //
      // Types bound by me :-)
      //

      // Somehow, the Producers nested class below is automatically
      // discovered.  I think this is a bug.  If it is fixed, then
      // uncomment this line.
      // event.addAnnotatedType(Producers.class, "maven");
      event.addAnnotatedType(BasicRepositoryConnectorFactory.class, "maven").add(SingletonLiteral.INSTANCE);

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
              if (f != null && !f.isAnnotationPresent(Named.class)) {
                final Requirement requirement = f.getAnnotation(Requirement.class);
                if (requirement != null) {
                  final String hint = requirement.hint();
                  if (hint != null && !hint.trim().isEmpty()) {
                    fc.add(NamedLiteral.of(hint));
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
            if (hint != null && !hint.isEmpty() && !type.isAnnotationPresent(Named.class)) {
              atc.add(NamedLiteral.of(hint));
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
    private static final ProfileSelector produceProfileSelector(@Named("jdk-version") final ProfileActivator jdkVersionProfileActivator,
                                                                @Named("os") final ProfileActivator operatingSystemProfileActivator,
                                                                @Named("property") final ProfileActivator fileProfileActivator,
                                                                @Named("file") final ProfileActivator propertyProfileActivator) {
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
    @Dependent
    private static final RepositorySystemSession produceRepositorySystemSession(final DependencyTraverser dependencyTraverser,
                                                                                final DependencyManager dependencyManager,
                                                                                final DependencySelector dependencySelector,
                                                                                final DependencyGraphTransformer dependencyGraphTransformer,
                                                                                final ArtifactTypeRegistry artifactTypeRegistry,
                                                                                final ArtifactDescriptorPolicy artifactDescriptorPolicy) {
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


    /*
     * Sets of things.
     */


    @Produces
    @Dependent
    private static final Set<LocalRepositoryManagerFactory> produceLocalRepositoryManagerFactorySet(@Any final Instance<LocalRepositoryManagerFactory> localRepositoryManagerFactories) {
      return produceSet(localRepositoryManagerFactories);
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
      if (things != null && things.isResolvable()) {
        for (final T thing : things) {
          returnValue.add(thing);
        }
      }
      return returnValue;
    }

  }
  
  private static final class StubLifecycleBindingsInjector implements LifecycleBindingsInjector {

    @Override
    public void injectLifecycleBindings(final Model model, final ModelBuildingRequest modelBuildingRequest, final ModelProblemCollector modelProblemCollector) {

    }
    
  }
  
}
