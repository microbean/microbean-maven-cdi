/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017-2019 microBean™.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import java.util.jar.JarFile;

import javax.enterprise.context.Dependent;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.literal.SingletonLiteral;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;

import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProcessor;

import org.apache.maven.model.composition.DefaultDependencyManagementImporter;

import org.apache.maven.model.inheritance.DefaultInheritanceAssembler;

import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;

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
import org.apache.maven.repository.internal.MavenRepositorySystemUtils; // for javadoc only
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

import org.apache.maven.settings.merge.MavenSettingsMerger;

import org.apache.maven.settings.validation.DefaultSettingsValidator;

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

import org.eclipse.aether.impl.LocalRepositoryProvider;
import org.eclipse.aether.impl.MetadataGeneratorFactory;

import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider;
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

import org.eclipse.aether.internal.impl.collect.DefaultDependencyCollector;

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

import org.microbean.configuration.cdi.annotation.ConfigurationValue;

import org.microbean.maven.cdi.annotation.Resolution;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * A <a
 * href="http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#spi">CDI
 * 2.0 portable extension</a> that exposes the <a
 * href="https://maven.apache.org/resolver/">Maven Artifact Resolver
 * components</a> as CDI beans, particularly for the purposes of
 * dependency resolution.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see RepositorySystem
 *
 * @see RepositorySystemSession
 */
public class MavenExtension implements Extension {


  /*
   * Instance fields.
   */


  /**
   * A {@link Map} of Maven artifact coordinates represented as {@link
   * Properties} objects indexed by {@link URI} objects representing
   * their location.
   *
   * <p>This field is never {@code null}.</p>
   *
   * <p>This field is not safe for concurrent use by multiple threads.
   * Threads should synchronize on its value to coordinate access.</p>
   *
   * @see #getGroupArtifactVersionCoordinates(URL)
   */
  private final Map<URI, Properties> coordinates;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link MavenExtension}.
   */
  public MavenExtension() {
    super();
    this.coordinates = new HashMap<>();
  }


  /*
   * Instance methods.
   */


  /**
   * Registers certain Maven Resolver components as CDI beans in
   * appropriate scopes.
   *
   * @param event the {@link BeforeBeanDiscovery} event indicating
   * that bean discovery has started; may be {@code null} in which
   * case no action will be taken
   *
   * @see BeforeBeanDiscovery#addAnnotatedType(Class, String)
   *
   * @see <a
   * href="https://github.com/apache/maven-resolver/blob/9417310df326cd4a58b0ef534e6c0e29f7b4cb47/maven-resolver-impl/src/main/java/org/eclipse/aether/impl/guice/AetherModule.java#L100-L151">Guice
   * bindings for {@code maven-resolver-impl}</a>
   *
   * @see <a
   * href="https://github.com/apache/maven-resolver/blob/9417310df326cd4a58b0ef534e6c0e29f7b4cb47/maven-resolver-impl/src/main/java/org/eclipse/aether/impl/DefaultServiceLocator.java#L186-L215">{@code
   * DefaultSerivceLocator} bindings</a>
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
      event.addAnnotatedType(SnapshotMetadataGeneratorFactory.class, "maven");
      event.addAnnotatedType(VersionsMetadataGeneratorFactory.class, "maven");

      //
      // Types effectively bound by DefaultModelBuilderFactory.  These
      // all have @Singleton on them already.
      //

      event.addAnnotatedType(DefaultDependencyManagementImporter.class, "maven");
      event.addAnnotatedType(DefaultDependencyManagementInjector.class, "maven");
      event.addAnnotatedType(DefaultInheritanceAssembler.class, "maven");
      event.addAnnotatedType(DefaultModelBuilder.class, "maven");
      event.addAnnotatedType(DefaultModelLocator.class, "maven");
      event.addAnnotatedType(DefaultModelNormalizer.class, "maven");
      event.addAnnotatedType(DefaultModelPathTranslator.class, "maven");
      event.addAnnotatedType(DefaultModelProcessor.class, "maven").add(Typed.Literal.of(new Class<?>[] { ModelProcessor.class }));
      event.addAnnotatedType(DefaultModelReader.class, "maven");
      event.addAnnotatedType(DefaultModelUrlNormalizer.class, "maven");
      event.addAnnotatedType(DefaultModelValidator.class, "maven");
      event.addAnnotatedType(DefaultPathTranslator.class, "maven");
      event.addAnnotatedType(DefaultPluginConfigurationExpander.class, "maven");
      event.addAnnotatedType(DefaultPluginManagementInjector.class, "maven");
      event.addAnnotatedType(DefaultProfileInjector.class, "maven");
      event.addAnnotatedType(DefaultReportConfigurationExpander.class, "maven");
      event.addAnnotatedType(DefaultReportingConverter.class, "maven");
      event.addAnnotatedType(DefaultSuperPomProvider.class, "maven");
      event.addAnnotatedType(DefaultUrlNormalizer.class, "maven");
      event.addAnnotatedType(FileProfileActivator.class, "maven");
      event.addAnnotatedType(JdkVersionProfileActivator.class, "maven");
      event.addAnnotatedType(OperatingSystemProfileActivator.class, "maven");
      event.addAnnotatedType(PropertyProfileActivator.class, "maven");
      event.addAnnotatedType(StringVisitorModelInterpolator.class, "maven");
      event.addAnnotatedType(StubLifecycleBindingsInjector.class, "maven");

      //
      // Types bound by me :-)
      //

      // Somehow, the Producers nested class below is automatically
      // discovered.  I think this is a bug.  If it is fixed, then
      // uncomment this line.
      // event.addAnnotatedType(Producers.class, "maven");
      event.addAnnotatedType(BasicRepositoryConnectorFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(DefaultSettingsBuilder.class, "maven");
      event.addAnnotatedType(DefaultSettingsReader.class, "maven");
      event.addAnnotatedType(DefaultSettingsValidator.class, "maven");
      event.addAnnotatedType(DefaultSettingsWriter.class, "maven");
      event.addAnnotatedType(FileTransporterFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(HttpTransporterFactory.class, "maven").add(SingletonLiteral.INSTANCE);
      event.addAnnotatedType(MavenSettingsMerger.class, "maven").add(SingletonLiteral.INSTANCE);
    }
  }

  /**
   * A hack to work around the fact that Maven's {@link
   * FileProfileActivator} class and {@code maven-resolver-api}'s
   * {@link FileTransporterFactory} both end up with the same CDI <a
   * href="https://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#name_resolution">EL
   * name</a>.
   *
   * <p>This method arbitrarily removes the {@code file} EL name from
   * the {@link FileProfileActivator} managed bean.</p>
   *
   * @param event the event in question; may be {@code null} in which
   * case no action will be taken
   *
   * @see FileProfileActivator
   *
   * @see FileTransporterFactory
   */
  private final void removeBeanName(@Observes final ProcessBeanAttributes<FileProfileActivator> event) {
    if (event != null) {
      final BeanAttributes<FileProfileActivator> beanAttributes = event.getBeanAttributes();
      if (beanAttributes != null) {
        if ("file".equals(beanAttributes.getName())) {
          event.configureBeanAttributes().name(null);
        }
      }
    }
  }

  /**
   * Returns a {@link Properties} object containing the <a
   * href="https://maven.apache.org/shared/maven-archiver/#pom-properties-content"
   * target="_parent">contents of a {@code pom.properties} file as
   * commonly installed in {@code .jar} files by Maven</a>.
   *
   * <p>This method may return {@code null}.</p>
   *
   * <p>This method is not {@code final} due to CDI specification
   * restrictions only.</p>
   *
   * @param location the location of the classpath element (a {@code
   * .jar} file, or, perhaps, a directory); may be {@code null} in
   * which case {@code null} will be returned
   *
   * @return a {@link Properties} object, or {@code null}
   *
   * @exception IOException if an input/output error occurs
   *
   * @exception URISyntaxException if there was a problem constructing
   * a {@link URI}
   */
  public Properties getGroupArtifactVersionCoordinates(final URL location) throws IOException, URISyntaxException {
    Properties returnValue = null;
    if (location != null) {
      final String path = location.getPath();

      URL beanArchiveRoot = null;

      String scheme = location.getProtocol();
      if ("jar".equalsIgnoreCase(scheme)) {
        beanArchiveRoot = new URL(location, "/"); // no jar entry, no matter what was passed in
      } else if (path != null && path.endsWith(".jar")) {
        beanArchiveRoot = new URL(new URL("jar", "", -1, location.toString() + "!/"), "/"); // normalize to jar URL with no jar entry
      } else {
        beanArchiveRoot = location; // no idea
      }
      assert beanArchiveRoot != null;

      synchronized (this.coordinates) {
        returnValue = this.coordinates.get(beanArchiveRoot.toURI());
        if (returnValue == null) {

          scheme = beanArchiveRoot.getProtocol();
          if ("jar".equalsIgnoreCase(scheme)) {
            final JarURLConnection urlConnection = (JarURLConnection)beanArchiveRoot.openConnection();
            if (urlConnection != null) {
              try (final JarFile jarFile = urlConnection.getJarFile()) {
                if (jarFile != null) {
                  returnValue = jarFile.stream()
                    .filter(e -> {
                        final String name = e.getName();
                        return name != null && name.startsWith("META-INF/maven/") && name.endsWith("/pom.properties");
                      })
                    .findAny() // there should be only one
                    .map(jarEntry -> {
                        Properties p = null;
                        try (final InputStream inputStream = new BufferedInputStream(jarFile.getInputStream(jarEntry))) {
                          if (inputStream != null) {
                            p = new Properties();
                            p.load(inputStream);
                          }
                        } catch (final IOException ioException) {
                          throw new RuntimeException(ioException);
                        }
                        return p;
                      })
                    .orElse(null);
                }
              }
            }
          } else if ("file".equalsIgnoreCase(scheme)) {
            File root = new File(beanArchiveRoot.toURI());
            while (root != null) {
              if (root.isDirectory()) {
                final File metaInfMaven = new File(root, "META-INF/maven");
                if (metaInfMaven.isDirectory()) {

                  File scanMe = metaInfMaven;
                  while (scanMe != null && scanMe.isDirectory()) {

                    final File[] subDirectories = scanMe.listFiles(f -> {
                        if (f == null || !f.isDirectory()) {
                          return false;
                        }
                        final String name = f.getName();
                        return name != null && !name.startsWith(".");
                      });

                    if (subDirectories == null || subDirectories.length <= 0) {
                      // We are looking for pom.properties in a
                      // directory that has no further subdirectories.
                      // We've found it.
                      final File pomPropertiesFile = new File(scanMe, "pom.properties");
                      scanMe = null;
                      if (pomPropertiesFile.exists() && !pomPropertiesFile.isDirectory()) {
                        returnValue = new Properties();
                        try (final InputStream stream = new BufferedInputStream(Files.newInputStream(pomPropertiesFile.toPath()))) {
                          returnValue.load(stream);
                          beanArchiveRoot = root.toURI().toURL();
                          root = null;
                        }
                      }
                    } else if (subDirectories.length == 1) {
                      scanMe = subDirectories[0];
                    } else {
                      scanMe = null;
                    }
                  }

                } else {
                  // For common local development purposes, see if
                  // maven-archiver/pom.properties exists under our
                  // root; this will give us coordinates for testing
                  final File mavenArchiverPomProperties = new File(root, "maven-archiver/pom.properties");
                  if (mavenArchiverPomProperties.exists() && !mavenArchiverPomProperties.isDirectory()) {
                    returnValue = new Properties();
                    try (final InputStream stream = new BufferedInputStream(Files.newInputStream(mavenArchiverPomProperties.toPath()))) {
                      returnValue.load(stream);
                      beanArchiveRoot = root.toURI().toURL();
                      root = null;
                    }
                  }
                }
                if (root != null) {
                  root = root.getParentFile();
                }
              } else {
                root = root.getParentFile();
              }
            }
          } else {
            throw new IllegalStateException();
          }

          if (returnValue == null ||
              !returnValue.containsKey("groupId") ||
              !returnValue.containsKey("artifactId") ||
              !returnValue.containsKey("version")) {
            returnValue = null;
          } else {
            this.coordinates.put(beanArchiveRoot.toURI(), returnValue);
          }
        }
      }
    }
    return returnValue;
  }

  /**
   * Given a {@link Bean}, returns a {@link URL} representing the
   * classpath root from which its {@linkplain Bean#getBeanClass()
   * bean class} originates.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @param bean the {@link Bean} whose classpath root should be
   * returned; may be {@code null} in which case {@code null} will be
   * returned
   *
   * @return a {@link URL} representing a classpath root from which
   * the supplied {@link Bean}'s {@linkplain Bean#getBeanClass() bean
   * class} originates, or {@code null}
   *
   * @see #getGroupArtifactVersionCoordinates(URL)
   *
   * @see Bean#getBeanClass()
   *
   * @see Class#getProtectionDomain()
   *
   * @see ProtectionDomain#getCodeSource()
   *
   * @see CodeSource#getLocation()
   */
  public static final URL getBeanArchiveLocation(final Bean<?> bean) {
    URL returnValue = null;
    if (bean != null) {
      final Class<?> beanClass = bean.getBeanClass();
      if (beanClass != null) {
        final ProtectionDomain protectionDomain = beanClass.getProtectionDomain();
        if (protectionDomain != null) {
          final CodeSource codeSource = protectionDomain.getCodeSource();
          if (codeSource != null) {
            returnValue = codeSource.getLocation();
          }
        }
      }
    }
    return returnValue;
  }


  /*
   * Inner and nested classes.
   */


  /**
   * A class housing several <a
   * href="http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#producer_method">producer
   * methods</a> that produce certain <a
   * href="https://maven.apache.org/resolver/">Maven Artifact Resolver
   * components</a>.
   *
   * @author <a href="http://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   */
  private static final class Producers {


    /*
     * Static methods.
     */


    /**
     * Produces a {@link MirrorSelector} in {@link Singleton} scope
     * from the contents of a {@link Settings} object.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param settingsInstance an {@link Instance} of {@link Settings}
     * that may be {@linkplain Instance#isUnsatisfied() unsatisfied}
     * that contains information about mirrors; must not be {@code
     * null}
     *
     * @return a {@link MirrorSelector}; never {@code null}
     *
     * @exception NullPointerException if {@code settingsInstance} is
     * {@code null}
     */
    @Produces
    @Singleton
    private static final MirrorSelector produceMirrorSelector(final Instance<Settings> settingsInstance) {
      Objects.requireNonNull(settingsInstance);
      final DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
      if (settingsInstance.isResolvable()) {
        final Collection<? extends Mirror> mirrors = settingsInstance.get().getMirrors();
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

    /**
     * Produces a {@link Settings} object in {@link Singleton} scope
     * from the supplied {@link SettingsBuilder} object.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param settingsBuilder the {@link SettingsBuilder} that will
     * ultimately be used to create this method's return value; must
     * not be {@code null}
     *
     * @param mavenHomePath a {@link Path} describing a Maven home
     * directory for the purposes of locating the global settings
     * file; may be {@code null}
     *
     * @param mavenConfPath a {@link Path} describing a Maven global
     * configuration directory for the purposes of locating the global
     * settings file; may be {@code null}
     *
     * @param globalSettingsPath a {@link Path} representing where the
     * global {@code settings.xml} file lives; may be {@code null}
     *
     * @param userSettingsPath a {@link Path} representing where the
     * user's {@code settings.xml} file lives; may be {@code null}
     *
     * @return a {@link Settings} object; never {@code null}
     *
     * @exception NullPointerException if {@code settingsBuilder} is
     * {@code null}
     *
     * @exception SettingsBuildingException if the {@link
     * SettingsBuildingResult#getProblems()} method invoked on the
     * return value of the {@link
     * SettingsBuilder#build(SettingsBuildingRequest)} method returns
     * a {@link List} of {@link SettingsProblem}s
     */
    @Produces
    @Singleton
    private static final Settings produceSettings(final SettingsBuilder settingsBuilder,

                                                  @ConfigurationValue(value = {
                                                      "maven.home",
                                                      "M2_HOME"
                                                    })
                                                  Path mavenHomePath,

                                                  @ConfigurationValue(value = {
                                                      "maven.conf"
                                                    })
                                                  Path mavenConfPath,

                                                  @ConfigurationValue(value = {
                                                      "org.microbean.maven.cdi.globalSettingsPath",
                                                      "GLOBAL_SETTINGS_PATH"
                                                    })
                                                  Path globalSettingsPath,

                                                  @ConfigurationValue(value = {
                                                      "org.microbean.maven.cdi.userSettingsPath",
                                                      "USER_SETTINGS_PATH"
                                                    },
                                                    defaultValue = "${configurations[\"user.home\"]}/.m2/settings.xml")
                                                  final Path userSettingsPath)
      throws SettingsBuildingException {
      Objects.requireNonNull(settingsBuilder);
      final DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
      request.setSystemProperties(System.getProperties());
      // request.setUserProperties(userProperties); // TODO: implement this

      final File globalSettingsFile;
      if (globalSettingsPath == null) {
        if (mavenConfPath == null) {
          if (mavenHomePath == null) {
            String m2Home = System.getProperty("maven.home");
            if (m2Home == null) {
              m2Home = System.getenv("M2_HOME");
            }
            if (m2Home == null) {
              globalSettingsFile = null;
            } else {
              mavenHomePath = Paths.get(m2Home);
              mavenConfPath = mavenHomePath.resolve("conf");
              globalSettingsPath = mavenConfPath.resolve("settings.xml");
              globalSettingsFile = globalSettingsPath.toFile();
            }
          } else {
            mavenConfPath = mavenHomePath.resolve("conf");
            assert mavenConfPath != null;
            globalSettingsPath = mavenConfPath.resolve("settings.xml");
            globalSettingsFile = globalSettingsPath.toFile();
          }
        } else {
          globalSettingsPath = mavenConfPath.resolve("settings.xml");
          globalSettingsFile = globalSettingsPath.toFile();
        }
      } else {
        globalSettingsFile = globalSettingsPath.toFile();
      }
      request.setGlobalSettingsFile(globalSettingsFile);

      final File userSettingsFile;
      if (userSettingsPath == null) {
        userSettingsFile = Paths.get(System.getProperty("user.home"), ".m2", "settings.xml").toFile();
      } else {
        userSettingsFile = userSettingsPath.toFile();
      }
      request.setUserSettingsFile(userSettingsFile);
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

    /**
     * Produces a {@link ProfileSelector} in {@link Singleton} scope
     * that makes use of the supplied {@link ProfileActivator}s.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param jdkVersionProfileActivator a {@link ProfileActivator}
     * that can activate a Maven profile based on the version of the
     * JDK; may be {@code null}
     *
     * @param operatingSystemProfileActivator a {@link
     * ProfileActivator} that can activate a Maven profile based on
     * the current operating system; may be {@code null}
     *
     * @param fileProfileActivator a {@link ProfileActivator} that can
     * activate a Maven profile based on the presence of a file; may
     * be {@code null}
     *
     * @param propertyProfileActivator a {@link ProfileActivator} that
     * can activate a maven profile based on the value of a property;
     * may be {@code null}
     *
     * @return a {@link ProfileSelector}; never {@code null}
     *
     * @see JdkVersionProfileActivator
     *
     * @see OperatingSystemProfileActivator
     *
     * @see FileProfileActivator
     *
     * @see PropertyProfileActivator
     *
     * @see DefaultProfileSelector
     */
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

    /**
     * Produces an {@link ILoggerFactory} in {@link Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @return an {@link ILoggerFactory}; never {@code null}
     *
     * @see LoggerFactory#getILoggerFactory()
     */
    @Produces
    @Singleton
    private static final ILoggerFactory produceILoggerFactory() {
      return LoggerFactory.getILoggerFactory();
    }

    /**
     * Produces a {@link LocalRepository} in {@link Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param mavenRepoLocal the value of the {@code maven.repo.local}
     * configuration property; may be (and often is) {@code null}
     *
     * @param settingsInstance an {@link Instance} of {@link Settings}
     * that may be {@linkplain Instance#isUnsatisfied() unsatisfied}
     * that contains information about where a local Maven repository
     * can be found; must not be {@code null}
     *
     * @return a {@link LocalRepository}; never {@code null}
     *
     * @exception NullPointerException if {@code settingsInstance} is
     * {@code null}
     *
     * @see Settings#getLocalRepository()
     */
    @Produces
    @Singleton
    private static final LocalRepository produceLocalRepository(@ConfigurationValue("maven.repo.local")
                                                                final String mavenRepoLocal,
                                                                final Instance<Settings> settingsInstance) {
      // TODO: see https://github.com/apache/maven/blob/eb6b212b567c287734a2dbbef3c113fe650f1def/maven-core/src/main/java/org/apache/maven/internal/aether/DefaultRepositorySystemSessionFactory.java#L133
      Objects.requireNonNull(settingsInstance);
      String localRepositoryString = mavenRepoLocal;
      if (localRepositoryString == null && settingsInstance.isResolvable()) {
        localRepositoryString = settingsInstance.get().getLocalRepository();
      }
      if (localRepositoryString == null) {
        localRepositoryString = System.getProperty("user.home") + "/.m2/repository";
      }
      final LocalRepository returnValue = new LocalRepository(localRepositoryString);
      return returnValue;
    }

    /**
     * Produces a {@link List} of {@link RemoteRepository} instances
     * in {@link Dependent} scope {@linkplain Resolution suitable for
     * artifact resolution} whose contents are inferred from reading
     * the {@linkplain Settings#getActiveProfiles() active profiles
     * contained in the supplied <code>Settings</code> object}.
     *
     * <p>This method will never return {@code null}.</p>
     *
     * @param settingsInstance an {@link Instance} of {@link Settings}
     * that may be {@linkplain Instance#isUnsatisfied() unsatisfied}
     * that contains information about where remote Maven repositories
     * may be found; must not be {@code null}
     *
     * @param repositorySystem the {@link RepositorySystem} whose
     * {@link
     * RepositorySystem#newResolutionRepositories(RepositorySystemSession,
     * List)} method will be called; must not be {@code null}
     *
     * @param session the {@link RepositorySystemSession} currently in
     * effect; must not be {@code null}
     *
     * @return a {@link List} of {@link RemoteRepository} instances
     * {@link Resolution suitable for artifact resolution}; never
     * {@code null}
     *
     * @exception NullPointerException if either {@code
     * settingsInstance}, {@code repositorySystem} or {@code session}
     * is {@code null}
     */
    @Produces
    @Dependent
    @Resolution
    private static final List<RemoteRepository> produceRemoteRepositoryList(final Instance<Settings> settingsInstance,
                                                                            final RepositorySystem repositorySystem,
                                                                            final RepositorySystemSession session) {
      Objects.requireNonNull(settingsInstance);
      Objects.requireNonNull(repositorySystem);
      Objects.requireNonNull(session);
      List<RemoteRepository> remoteRepositories = new ArrayList<>();
      if (settingsInstance.isResolvable()) {
        final Settings settings = settingsInstance.get();
        assert settings != null;
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
      }
      final RemoteRepository mavenCentral = new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
      assert mavenCentral != null;
      remoteRepositories.add(mavenCentral);
      remoteRepositories = repositorySystem.newResolutionRepositories(session, remoteRepositories);
      assert remoteRepositories != null;
      return remoteRepositories;
    }

    /**
     * Produces a {@link RepositorySystemSession} in {@link Dependent} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * <h2>Implementation Notes</h2>
     *
     * <p>This method returns an {@link RepositorySystemSession} that
     * is built in the same manner as that built by the {@link
     * MavenRepositorySystemUtils#newSession()} method.</p>
     *
     * @param dependencyTraverser the {@link DependencyTraverser} that
     * helps in pruning the dependency graph; may be {@code null}
     *
     * @param dependencyManager the {@link DependencyManager} that
     * applies any overriding concerns with respect to particular
     * dependencies' versions and scopes (think {@code
     * <dependencyManagement>} in a Maven {@code pom.xml} file); may
     * be {@code null}
     *
     * @param dependencySelector a {@link DependencySelector} that
     * helps filter dependencies; may be {@code null}
     *
     * @param dependencyGraphTransformer a {@link
     * DependencyGraphTransformer} that helps with conflict
     * resolution; may be {@code null}
     *
     * @param artifactTypeRegistry an {@link ArtifactTypeRegistry}
     * that governs what artifacts will be looked at; may be {@code
     * null}
     *
     * @param artifactDescriptorPolicy an {@link
     * ArtifactDescriptorPolicy} that controls what to do with missing
     * or invalid artifacts; may be {@code null}
     *
     * @param settingsInstance an {@link Instance} of {@link Settings}
     * that may be {@linkplain Instance#isUnsatisfied() unsatisfied}
     * that may be consulted for offline status information; must not
     * be {@code null}
     *
     * @param mirrorSelector a {@link MirrorSelector} that will choose
     * the appropriate mirror for a given repository; may be {@code
     * null}
     *
     * @param localRepository a {@link LocalRepository} instance
     * representing the location where remote artifacts will be
     * cached; must not be {@code null}
     *
     * @param localRepositoryProvider a {@link
     * LocalRepositoryProvider} that will be {@linkplain
     * LocalRepositoryProvider#newLocalRepositoryManager(RepositorySystemSession,
     * LocalRepository) used to obtain a
     * <code>LocalRepositoryManager</code>}; must not be {@code null}
     *
     * @param transferListenerInstance an {@link Instance}
     * representing a {@link TransferListener}, which, {@linkplain
     * Instance#isResolvable() if resolvable}, will be installed on
     * the returned {@link RepositorySystemSession}; may be {@code
     * null}
     *
     * @return a {@link RepositorySystemSession}; never {@code null}
     *
     * @exception NullPointerException if any parameter described
     * above that must be non-{@code null} was {@code null}
     *
     * @exception NoLocalRepositoryManagerException if the supplied
     * {@link LocalRepositoryProvider} could not {@linkplain
     * LocalRepositoryProvider#newLocalRepositoryManager(RepositorySystemSession,
     * LocalRepository) provide a <code>LocalRepositoryManager</code>}
     *
     * @see MavenRepositorySystemUtils#newSession()
     *
     * @see FatArtifactTraverser
     *
     * @see ClassicDependencyManager
     *
     * @see ScopeDependencySelector
     *
     * @see OptionalDependencySelector
     *
     * @see ExclusionDependencySelector
     *
     * @see ConflictResolver
     *
     * @see NearestVersionSelector
     *
     * @see JavaScopeSelector
     *
     * @see SimpleOptionalitySelector
     *
     * @see JavaScopeDeriver
     *
     * @see #produceArtifactTypeRegistry()
     *
     * @see #produceArtifactDescriptorPolicy()
     *
     * @see Settings#isOffline()
     *
     * @see DefaultRepositorySystemSession#setOffline(boolean)
     *
     * @see #produceMirrorSelector(Instance)
     *
     * @see #produceLocalRepository(String, Instance)
     *
     * @see
     * LocalRepositoryProvider#newLocalRepositoryManager(RepositorySystemSession,
     * LocalRepository)
     *
     * @see TransferListener
     *
     * @see
     * DefaultRepositorySystemSession#setTransferListener(TransferListener)
     */
    @Produces
    @Dependent
    private static final RepositorySystemSession produceRepositorySystemSession(final DependencyTraverser dependencyTraverser,
                                                                                final DependencyManager dependencyManager,
                                                                                final DependencySelector dependencySelector,
                                                                                final DependencyGraphTransformer dependencyGraphTransformer,
                                                                                final ArtifactTypeRegistry artifactTypeRegistry,
                                                                                final ArtifactDescriptorPolicy artifactDescriptorPolicy,
                                                                                final Instance<Settings> settingsInstance,
                                                                                final MirrorSelector mirrorSelector,
                                                                                final LocalRepository localRepository,
                                                                                final LocalRepositoryProvider localRepositoryProvider,
                                                                                final Instance<TransferListener> transferListenerInstance)
    throws NoLocalRepositoryManagerException {
      Objects.requireNonNull(settingsInstance);
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
      if (settingsInstance.isResolvable()) {
        session.setOffline(settingsInstance.get().isOffline());
      }
      session.setMirrorSelector(mirrorSelector);
      final LocalRepositoryManager localRepositoryManager = localRepositoryProvider.newLocalRepositoryManager(session, localRepository);
      session.setLocalRepositoryManager(localRepositoryManager);
      if (transferListenerInstance != null && transferListenerInstance.isResolvable()) {
        session.setTransferListener(transferListenerInstance.get());
      }
      return session;
    }

    /**
     * Produces a {@link DependencySelector} in {@link Singleton}
     * scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * <h2>Implementation Notes</h2>
     *
     * <p>This method returns an {@link AndDependencySelector} that is
     * built in the same manner as that built by the {@link
     * MavenRepositorySystemUtils#newSession()} method.</p>
     *
     * @return a {@link DependencySelector}; never {@code null}
     *
     * @see MavenRepositorySystemUtils#newSession()
     */
    @Produces
    @Singleton
    private static final DependencySelector produceDependencySelector() {
      return new AndDependencySelector(new ScopeDependencySelector("test", "provided"), // excludes test and provided scopes
                                       new OptionalDependencySelector(),
                                       new ExclusionDependencySelector());
    }

    /**
     * Produces a {@link DependencyGraphTransformer} in {@link
     * Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * <h2>Implementation Notes</h2>
     *
     * <p>This method returns a {@link
     * ChainedDependencyGraphTransformer} that is built in the same
     * manner as that built by the {@link
     * MavenRepositorySystemUtils#newSession()} method.</p>
     *
     * @return a {@link DependencyGraphTransformer}; never {@code
     * null}
     *
     * @see MavenRepositorySystemUtils#newSession()
     */
    @Produces
    @Singleton
    private static final DependencyGraphTransformer produceDependencyGraphTransformer() {
      return new ChainedDependencyGraphTransformer(new ConflictResolver(new NearestVersionSelector(),
                                                                        new JavaScopeSelector(),
                                                                        new SimpleOptionalitySelector(),
                                                                        new JavaScopeDeriver()),
                                                   new JavaDependencyContextRefiner());
    }

    /**
     * Produces an {@link ArtifactTypeRegistry} in {@link Singleton}
     * scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * <h2>Implementation Notes</h2>
     *
     * <p>This method returns a {@link DefaultArtifactTypeRegistry}
     * that is built in the same manner as that built by the {@link
     * MavenRepositorySystemUtils#newSession()} method.</p>
     *
     * @return an {@link ArtifactTypeRegistry}; never {@code null}
     *
     * @see MavenRepositorySystemUtils#newSession()
     */
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

    /**
     * Produces an {@link ArtifactDescriptorPolicy} in {@link
     * Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * <h2>Implementation Notes</h2>
     *
     * <p>This method returns a {@link SimpleArtifactDescriptorPolicy}
     * that is built in the same manner as that built by the {@link
     * MavenRepositorySystemUtils#newSession()} method.</p>
     *
     * @return an {@link ArtifactDescriptorPolicy}; never {@code null}
     *
     * @see MavenRepositorySystemUtils#newSession()
     */
    @Produces
    @Singleton
    private static final ArtifactDescriptorPolicy produceArtifactDescriptorPolicy() {
      return new SimpleArtifactDescriptorPolicy(true /* ignoreMissing */, true /* ignoreInvalid */);
    }


    /*
     * Sets of things.
     */


    /**
     * Converts an {@link Instance} of {@link
     * LocalRepositoryManagerFactory} instances into a {@link Set} of
     * such instances in {@link Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param localRepositoryManagerFactories an {@link Instance} of
     * {@link LocalRepositoryManagerFactory} instances; may be {@code
     * null}
     *
     * @return a {@link Set} of {@link LocalRepositoryManagerFactory}
     * instances; never {@code null}
     *
     * @see #produceSet(Instance)
     */
    @Produces
    @Singleton
    private static final Set<LocalRepositoryManagerFactory> produceLocalRepositoryManagerFactorySet(@Any final Instance<LocalRepositoryManagerFactory> localRepositoryManagerFactories) {
      final Set<LocalRepositoryManagerFactory> returnValue = produceSet(localRepositoryManagerFactories);
      return returnValue;
    }

    /**
     * Converts an {@link Instance} of {@link RepositoryLayoutFactory}
     * instances into a {@link Set} of such instances in {@link
     * Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param repositoryLayoutFactories an {@link Instance} of {@link
     * RepositoryLayoutFactory} instances; may be {@code null}
     *
     * @return a {@link Set} of {@link RepositoryLayoutFactory}
     * instances; never {@code null}
     *
     * @see #produceSet(Instance)
     */
    @Produces
    @Singleton
    private static final Set<RepositoryLayoutFactory> produceRepositoryLayoutFactorySet(@Any final Instance<RepositoryLayoutFactory> repositoryLayoutFactories) {
      return produceSet(repositoryLayoutFactories);
    }

    /**
     * Converts an {@link Instance} of {@link TransporterFactory}
     * instances into a {@link Set} of such instances in {@link
     * Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param transporterFactories an {@link Instance} of {@link
     * TransporterFactory} instances; may be {@code null}
     *
     * @return a {@link Set} of {@link TransporterFactory} instances;
     * never {@code null}
     *
     * @see #produceSet(Instance)
     */
    @Produces
    @Singleton
    private static final Set<TransporterFactory> produceTransporterFactorySet(@Any final Instance<TransporterFactory> transporterFactories) {
      return produceSet(transporterFactories);
    }

    /**
     * Converts an {@link Instance} of {@link RepositoryListener}
     * instances into a {@link Set} of such instances in {@link
     * Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param repositoryListeners an {@link Instance} of {@link
     * RepositoryListener} instances; may be {@code null}
     *
     * @return a {@link Set} of {@link RepositoryListener} instances;
     * never {@code null}
     *
     * @see #produceSet(Instance)
     */
    @Produces
    @Dependent
    private static final Set<RepositoryListener> produceRepositoryListenerSet(@Any final Instance<RepositoryListener> repositoryListeners) {
      return produceSet(repositoryListeners);
    }

    /**
     * Converts an {@link Instance} of {@link
     * RepositoryConnectorFactory} instances into a {@link Set} of
     * such instances in {@link Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param repositoryConnectorFactories an {@link Instance} of
     * {@link RepositoryConnectorFactory} instances; may be {@code
     * null}
     *
     * @return a {@link Set} of {@link RepositoryConnectorFactory}
     * instances; never {@code null}
     *
     * @see #produceSet(Instance)
     */
    @Produces
    @Singleton
    private static final Set<RepositoryConnectorFactory> produceRepositoryConnectorFactorySet(@Any final Instance<RepositoryConnectorFactory> repositoryConnectorFactories) {
      return produceSet(repositoryConnectorFactories);
    }

    /**
     * Converts an {@link Instance} of {@link
     * MetadataGeneratorFactory} instances into a {@link Set} of such
     * instances in {@link Singleton} scope.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param metadataGeneratorFactories an {@link Instance} of {@link
     * MetadataGeneratorFactory} instances; may be {@code null}
     *
     * @return a {@link Set} of {@link MetadataGeneratorFactory}
     * instances; never {@code null}
     *
     * @see #produceSet(Instance)
     */
    @Produces
    @Singleton
    private static final Set<MetadataGeneratorFactory> produceMetadataGeneratorFactorySet(@Any final Instance<MetadataGeneratorFactory> metadataGeneratorFactories) {
      return produceSet(metadataGeneratorFactories);
    }

    /**
     * Returns a {@link Set} of the objects represented by the
     * supplied {@link Instance}.
     *
     * <p>This method never returns {@code null}.</p>
     *
     * @param <T> the type of object the returned {@link Set} will
     * contain
     *
     * @param things the {@link Instance} whose instances should be
     * returned as a {@link Set}; may be {@code null}
     *
     * @return a non-{@code null} {@link Set}
     */
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

}
