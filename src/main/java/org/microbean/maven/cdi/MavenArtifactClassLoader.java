/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017-2018 microBean.
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler; // for javadoc only
import java.net.URLStreamHandlerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.stream.Collectors;

import org.eclipse.aether.artifact.ArtifactProperties;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import org.eclipse.aether.collection.CollectRequest;

import org.eclipse.aether.graph.Dependency;

import org.eclipse.aether.repository.RemoteRepository;

import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

import org.eclipse.aether.util.artifact.JavaScopes;

import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * A {@link URLClassLoader} that uses the <a
 * href="https://maven.apache.org/resolver/apidocs/index.html?org/eclipse/aether/util/filter/DependencyFilterUtils.html">Maven
 * Resolver API</a> to resolve artifacts from Maven repositories.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #MavenArtifactClassLoader(RepositorySystem,
 * RepositorySystemSession, DependencyRequest, ClassLoader,
 * URLStreamHandlerFactory)
 */
public class MavenArtifactClassLoader extends URLClassLoader {


  /*
   * Static fields.
   */


  /**
   * The {@link String} used to separate classpath entries.
   *
   * <p>This field is never {@code null}.</p>
   */
  private static final String classpathSeparator = System.getProperty("path.separator", ":");


  /*
   * Constructors.
   */

  /**
   * Creates a new {@link MavenArtifactClassLoader}.
   *
   * @param repositorySystem the {@link RepositorySystem} responsible
   * for resolving artifacts; must not be {@code null}
   *
   * @param session the {@link RepositorySystemSession} governing
   * certain aspects of the resolution process; must not be {@code
   * null}
   *
   * @param dependencyRequest the {@link DependencyRequest} that
   * describes what artifacts should be resolved; must not be {@code
   * null}
   *
   * @exception NullPointerException if a parameter that must not be
   * {@code null} is discovered to be {@code null}
   *
   * @exception DependencyResolutionException if there was a problem
   * resolving dependencies
   *
   * @see #getUrls(RepositorySystem, RepositorySystemSession,
   * DependencyRequest)
   *
   * @see #getDependencyRequest(String, List)
   *
   * @see URLClassLoader#URLClassLoader(URL[])
   */
  public MavenArtifactClassLoader(final RepositorySystem repositorySystem,
                                  final RepositorySystemSession session,
                                  final DependencyRequest dependencyRequest)
    throws DependencyResolutionException {
    super(getUrlArray(getUrls(repositorySystem, session, dependencyRequest)));
  }

  /**
   * Creates a new {@link MavenArtifactClassLoader}.
   *
   * @param repositorySystem the {@link RepositorySystem} responsible
   * for resolving artifacts; must not be {@code null}
   *
   * @param session the {@link RepositorySystemSession} governing
   * certain aspects of the resolution process; must not be {@code
   * null}
   *
   * @param dependencyRequest the {@link DependencyRequest} that
   * describes what artifacts should be resolved; must not be {@code
   * null}
   *
   * @param parentClassLoader the {@link ClassLoader} that will
   * {@linkplain ClassLoader#getParent() parent} this one; may be
   * {@code null}
   *
   * @exception NullPointerException if a parameter that must not be
   * {@code null} is discovered to be {@code null}
   *
   * @exception DependencyResolutionException if there was a problem
   * resolving dependencies
   *
   * @see #getUrls(RepositorySystem, RepositorySystemSession,
   * DependencyRequest)
   *
   * @see #getDependencyRequest(String, List)
   *
   * @see URLClassLoader#URLClassLoader(URL[], ClassLoader)
   */
  public MavenArtifactClassLoader(final RepositorySystem repositorySystem,
                                  final RepositorySystemSession session,
                                  final DependencyRequest dependencyRequest,
                                  final ClassLoader parentClassLoader)
    throws DependencyResolutionException {
    super(getUrlArray(getUrls(repositorySystem, session, dependencyRequest)),
          parentClassLoader);
  }

  /**
   * Creates a new {@link MavenArtifactClassLoader}.
   *
   * @param repositorySystem the {@link RepositorySystem} responsible
   * for resolving artifacts; must not be {@code null}
   *
   * @param session the {@link RepositorySystemSession} governing
   * certain aspects of the resolution process; must not be {@code
   * null}
   *
   * @param dependencyRequest the {@link DependencyRequest} that
   * describes what artifacts should be resolved; must not be {@code
   * null}
   *
   * @param parentClassLoader the {@link ClassLoader} that will
   * {@linkplain ClassLoader#getParent() parent} this one; may be
   * {@code null}
   *
   * @param urlStreamHandlerFactory the {@link
   * URLStreamHandlerFactory} to create {@link URLStreamHandler}s for
   * the {@link URL}s {@linkplain #getURLs() <code>URL</code>s that
   * constitute this <code>MavenArtifactClassLoader</code>'s
   * classpath}; {@link URLClassLoader} does not define whether this
   * can be {@code null} or not
   *
   * @exception NullPointerException if a parameter that must not be
   * {@code null} is discovered to be {@code null}
   *
   * @exception DependencyResolutionException if there was a problem
   * resolving dependencies
   *
   * @see #getUrls(RepositorySystem, RepositorySystemSession,
   * DependencyRequest)
   *
   * @see #getDependencyRequest(String, List)
   *
   * @see URLClassLoader#URLClassLoader(URL[], ClassLoader,
   * URLStreamHandlerFactory)
   */
  public MavenArtifactClassLoader(final RepositorySystem repositorySystem,
                                  final RepositorySystemSession session,
                                  final DependencyRequest dependencyRequest,
                                  final ClassLoader parentClassLoader,
                                  final URLStreamHandlerFactory urlStreamHandlerFactory)
    throws DependencyResolutionException {
    super(getUrlArray(getUrls(repositorySystem, session, dependencyRequest)),
          parentClassLoader,
          urlStreamHandlerFactory);
  }


  /*
   * Instance methods.
   */
  

  /**
   * Returns a non-{@code null} {@link String} with a classpath-like
   * format that can represent this {@linkplain #getURLs()
   * <code>MavenArtifactClassLoader</code>'s <code>URL</code>s}.
   *
   * <p>The format of the {@link String} that is returned is exactly
   * like a classpath string appropriate for the current platform with
   * the exception that the platform-specific classpath separator is
   * doubled.</p>
   *
   * <p>For any given {@link URL}, the returned {@link String} will
   * represent it as its {@linkplain URL#toExternalForm()
   * <code>String</code> form}, unless it has a {@linkplain
   * URL#getProtocol() protocol} equal to {@code file}, in which case
   * will represent it as its {@linkplain URL#getPath() path}.</p>
   *
   * @return a non-{@code null} classpath-like {@link String} (with
   * doubled classpath separators) consisting of this {@linkplain
   * #getURLs() <code>MavenArtifactClassLoader</code>'s
   * <code>URL</code>s} represented as described above
   */
  public String toClasspath() {
    final StringBuilder sb = new StringBuilder();
    final URL[] urls = this.getURLs();
    if (urls != null && urls.length > 0) {
      final String separator = classpathSeparator + classpathSeparator;
      for (int i = 0; i < urls.length; i++) {
        final URL url = urls[i];
        if (url != null && "file".equals(url.getProtocol())) {
          sb.append(url.getPath());
          if (i + 1 < urls.length) {
            sb.append(separator);
          }
        }
      }
    }
    return sb.toString();
  }


  /*
   * Static methods.
   */


  /**
   * Returns a non-{@code null} {@link DependencyRequest} by parsing
   * the supplied classpath-like {@link String} for Maven artifact
   * coordinates, and then invoking and returning the result of the
   * {@link #getDependencyRequest(Set, List)} method.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Elements within the supplied {@code classpathLikeString} are
   * separated by double occurrences of the platform-specific
   * classpath separator.  For example, on Unix and Unix-derived
   * systems, a classpath-like string of the form {@code
   * com.foo:bar:1.0::com.fizz:buzz:1.0} will yield two elements:
   * {@code com.foo:bar:1.0} and {@code com.fizz:buzz:1.0}.  On
   * Windows systems, a classpath-like string of the form {@code
   * com.foo:bar:1.0;;com.fizz:buzz:1.0} will yield two elements:
   * {@code com.foo:bar:1.0} and {@code com.fizz:buzz:1.0}.</p>
   *
   * @param classpathLikeString a classpath-like {@link String}
   * formatted as described above; may be {@code null}
   *
   * @param remoteRepositories a {@link List} of {@link
   * RemoteRepository} instances that will be forwarded on to the
   * {@link #getDependencyRequest(Set, List)} method; must not be
   * {@code null}
   *
   * @return the return value that results from invoking the {@link
   * #getDependencyRequest(Set, List)} method
   *
   * @exception NullPointerException if {@code remoteRepositories} is
   * {@code null}
   */
  public static final DependencyRequest getDependencyRequest(final String classpathLikeString,
                                                             final List<RemoteRepository> remoteRepositories) {
    final Set<Artifact> artifacts;
    if (classpathLikeString == null) {
      artifacts = Collections.emptySet();
    } else {
      final String separator = "\\s*" + classpathSeparator + classpathSeparator + "\\s*";
      final String[] artifactIdentifiers = classpathLikeString.split(separator);
      assert artifactIdentifiers != null;
      artifacts = new LinkedHashSet<>();
      for (final String artifactIdentifier : artifactIdentifiers) {
        if (artifactIdentifier != null && !artifactIdentifier.isEmpty()) {
          final Artifact artifact = new DefaultArtifact(artifactIdentifier);
          final String extension = artifact.getExtension();
          if ("war".equals(extension) || "ear".equals(extension)) {
            final Map<String, String> originalProperties = artifact.getProperties();
            final Map<String, String> newProperties;
            if (originalProperties == null) {
              newProperties = new HashMap<>();
            } else {
              newProperties = new HashMap<>(originalProperties);
            }
            newProperties.put(ArtifactProperties.INCLUDES_DEPENDENCIES, "true");
            artifacts.add(artifact.setProperties(newProperties));
          } else {
            artifacts.add(artifact);
          }
        }
      }
    }
    return getDependencyRequest(artifacts, remoteRepositories);
  }

  /**
   * Given a {@link Set} of {@link Artifact}s and a {@link List} of
   * {@link RemoteRepository} instances representing repositories from
   * which they might be resolved, creates and returns a {@link
   * DependencyRequest} for their resolution, using {@linkplain
   * JavaScopes#RUNTIME runtime scope}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param artifacts a {@link Set} of {@link Artifact}s to resolve;
   * may be {@code null}
   *
   * @param remoteRepositories a {@link List} of {@link
   * RemoteRepository} instances representing Maven repositories from
   * which the supplied {@link Artifact} instances may be resolved;
   * must not be {@code null}
   *
   * @return a non-{@code null} {@link DependencyRequest}
   *
   * @exception NullPointerException if {@code remoteRepositories} is
   * {@code null}
   *
   * @see #getDependencyRequest(Set, String, List)
   */
  public static final DependencyRequest getDependencyRequest(final Set<? extends Artifact> artifacts,
                                                             final List<RemoteRepository> remoteRepositories) {
    return getDependencyRequest(artifacts, JavaScopes.RUNTIME, remoteRepositories);
  }

  /**
   * Given a {@link Set} of {@link Artifact}s and a {@link List} of
   * {@link RemoteRepository} instances representing repositories from
   * which they might be resolved, creates and returns a {@link
   * DependencyRequest} for their resolution in the supplied scope.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param artifacts a {@link Set} of {@link Artifact}s to resolve;
   * may be {@code null}
   *
   * @param scope the scope in which resolution should take place; may
   * be {@code null} in which case {@link JavaScopes#RUNTIME} will be
   * used instead; see {@link JavaScopes} for commonly-used scopes
   *
   * @param remoteRepositories a {@link List} of {@link
   * RemoteRepository} instances representing Maven repositories from
   * which the supplied {@link Artifact} instances may be resolved;
   * must not be {@code null}
   *
   * @return a non-{@code null} {@link DependencyRequest}
   *
   * @exception NullPointerException if {@code remoteRepositories} is
   * {@code null}
   *
   * @see JavaScopes
   *
   * @see #getDependencyRequest(CollectRequest)
   */
  public static final DependencyRequest getDependencyRequest(final Set<? extends Artifact> artifacts,
                                                             String scope,
                                                             final List<RemoteRepository> remoteRepositories) {
    if (scope == null) {
      scope = JavaScopes.RUNTIME;
    }
    CollectRequest collectRequest = new CollectRequest()
      .setRoot(null)
      .setRepositories(remoteRepositories);
    if (artifacts != null && !artifacts.isEmpty()) {
      if (artifacts.size() == 1) {
        final Artifact root = artifacts.iterator().next();
        if (root != null) {
          collectRequest = collectRequest.setRoot(new Dependency(root, scope));
        }
      } else {
        for (final Artifact artifact : artifacts) {
          if (artifact != null) {
            collectRequest = collectRequest.addDependency(new Dependency(artifact, scope));
          }
        }
      }
    }
    return getDependencyRequest(collectRequest);
  }

  /**
   * Returns a non-{@code null} {@link DependencyRequest} suitable for
   * the supplied {@link CollectRequest}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param collectRequest a {@link CollectRequest} describing
   * dependency collection; must not be {@code null}
   *
   * @return a new {@link DependencyRequest}; never {@code null}
   *
   * @exception NullPointerException if {@code collectRequest} is
   * {@code null}
   *
   * @see CollectRequest
   *
   * @see DependencyRequest
   *
   * @see #MavenArtifactClassLoader(RepositorySystem,
   * RepositorySystemSession, DependencyRequest, ClassLoader,
   * URLStreamHandlerFactory)
   */
  public static final DependencyRequest getDependencyRequest(final CollectRequest collectRequest) {
    Objects.requireNonNull(collectRequest);
    final Dependency root = collectRequest.getRoot();
    final String scope;
    if (root != null) {
      scope = root.getScope();
    } else {
      scope = JavaScopes.RUNTIME;
    }
    return new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(scope));
  }
  
  private static final URL[] getUrlArray(final Collection<? extends URL> urls) {
    if (urls == null || urls.isEmpty()) {
      return new URL[0];
    }
    return urls.toArray(new URL[urls.size()]);
  }
  
  /**
   * Returns a {@link Collection} of ({@code file}) {@link URL}s that
   * results from resolution of the dependencies described by the
   * supplied {@link DependencyRequest}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param repositorySystem the {@link RepositorySystem} responsible
   * for resolving artifacts; must not be {@code null}
   *
   * @param session the {@link RepositorySystemSession} governing
   * certain aspects of the resolution process; must not be {@code
   * null}
   *
   * @param dependencyRequest the {@link DependencyRequest} that
   * describes what artifacts should be resolved; must not be {@code
   * null}
   *
   * @return a non-{@code null} {@link Collection} of distinct {@link
   * URL}s; a {@link Set} is not part of the contract of this method
   * only because the {@link URL#equals(Object)} method involves DNS
   * lookups
   *
   * @exception NullPointerException if any parameter is {@code null}
   *
   * @exception DependencyResolutionException if there was a problem
   * with dependency resolution
   */
  public static final Collection<? extends URL> getUrls(final RepositorySystem repositorySystem,
                                                        final RepositorySystemSession session,
                                                        final DependencyRequest dependencyRequest)
    throws DependencyResolutionException {
    Objects.requireNonNull(repositorySystem);
    Objects.requireNonNull(session);
    Objects.requireNonNull(dependencyRequest);
    final DependencyResult dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest);
    assert dependencyResult != null;
    final List<ArtifactResult> artifactResults = dependencyResult.getArtifactResults();
    assert artifactResults != null;

    // We use an intermediate Set of URIs, not URLs, because the
    // URL#equals(Object) method will trigger DNS lookups otherwise
    // (!).
    Set<URI> uris = new LinkedHashSet<>();
    if (!artifactResults.isEmpty()) {
      for (final ArtifactResult artifactResult : artifactResults) {
        if (artifactResult != null && artifactResult.isResolved()) {
          final Artifact resolvedArtifact = artifactResult.getArtifact();
          if (resolvedArtifact != null) {
            final File f = resolvedArtifact.getFile();
            assert f != null; // guaranteed by isResolved() contract
            assert f.isFile();
            assert f.canRead();
            uris.add(f.toURI());
          }
        }
      }
    }
    final Collection<? extends URL> returnValue;
    if (uris.isEmpty()) {
      returnValue = Collections.emptySet();
    } else {
      returnValue = Collections.unmodifiableCollection(uris.stream()
                                                       .map(uri -> {
                                                           try {
                                                             return uri.toURL();
                                                           } catch (final MalformedURLException malformedUrlException) {
                                                             throw new IllegalArgumentException(malformedUrlException.getMessage(),
                                                                                                malformedUrlException);
                                                           }
                                                         })
                                                       .collect(Collectors.toCollection(ArrayList::new)));
    }
    return returnValue;
  }
  
}
