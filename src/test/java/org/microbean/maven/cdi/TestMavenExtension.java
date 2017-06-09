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

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;

import javax.enterprise.event.Observes;

import org.apache.maven.settings.Settings;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystem;

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

import org.junit.Test;

import org.eclipse.aether.transfer.TransferListener;

import org.microbean.main.Main;

import org.microbean.maven.cdi.annotation.Resolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ApplicationScoped
public class TestMavenExtension {

  
  /*
   * Static fields.
   */

  
  /**
   * The number of instances of this class that have been created (in
   * the context of JUnit execution; any other usage is undefined).
   */
  private static int instanceCount;


  /*
   * Constructors
   */
  
  
  public TestMavenExtension() {
    super();
    instanceCount++;
  }


  /*
   * Instance methods.
   */

  private final void onStartup(@Observes @Initialized(ApplicationScoped.class) final Object event,
                               final RepositorySystem repositorySystem,
                               final RepositorySystemSession session,
                               @Resolution final List<RemoteRepository> remoteRepositories,
                               final TransferListener transferListener)
    throws DependencyResolutionException {
    assertNotNull(repositorySystem);
    assertNotNull(session);
    assertNotNull(remoteRepositories);
    assertNotNull(session.getDependencyManager());
    assertNotNull(transferListener);
    
    final CollectRequest collectRequest = new CollectRequest();
    final Artifact artifact = new DefaultArtifact("org.microbean", "microbean-configuration-cdi", "jar", "0.1.5");
    collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
    collectRequest.setRepositories(remoteRepositories);

    final DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE));
    final DependencyResult dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest);
    assertNotNull(dependencyResult);
    final List<ArtifactResult> artifactResults = dependencyResult.getArtifactResults();
    assertNotNull(artifactResults);
  }
  
  @Test
  public void testContainerStartup() {
    final int oldInstanceCount = instanceCount;
    Main.main(null);
    assertEquals(oldInstanceCount + 1, instanceCount);
  }
  
  
}
