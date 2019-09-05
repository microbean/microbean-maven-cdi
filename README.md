# microBean Maven CDI

[![Build Status](https://travis-ci.org/microbean/microbean-maven-cdi.svg?branch=master)](https://travis-ci.org/microbean/microbean-maven-cdi)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microbean/microbean-maven-cdi/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microbean/microbean-maven-cdi)

The microBean Maven CDI project embeds the [Maven machinery responsible
for interacting with artifact repositories][maven-resolver] into your CDI 2.0
environment.

## Background and History

The internal Maven machinery for interacting with artifact
repositories has gone through several phases of existence.

Its earliest incarnation is in the form of the
[`maven-artifact-resolver`](http://search.maven.org/classic/#artifactdetails%7Corg.apache.maven.shared%7Cmaven-artifact-resolver%7C1.0%7Cjar)
artifact, version 1.0, _circa_ 2009 (here in 2019 this artifact should
not be used).

This code was subsequently [broken out as a standalone project][sonatype-aether]
into [Sonatype](https://www.sonatype.com/)'s [&AElig;ther
project](https://github.com/sonatype/sonatype-aether) in [August
2010][sonatype-aether].

Sonatype &AElig;ther then moved to the Eclipse foundation in [August
2014][eclipse-aether], but never really caught on as a standalone project outside
of its original Maven-oriented user base, and was [archived in January
2015][eclipse-aether].

The Sonatype &AElig;ther project was handed _back_ to the Maven
project and reincarnated as the [Maven Artifact Resolver
project][maven-resolver] in January 2017, with an overarching artifact
identifier of [`maven-resolver`][maven-resolver] (not `maven-artifact-resolver`).
Somewhat oddly, the package names have not changed, so the classes
provided by the `maven-resolver` project all start with the
`org.eclipse.aether.` prefix.

This project adapts the [`maven-resolver`][maven-resolver] project to CDI
environments.

## Installation

Place this project's `.jar` file on your classpath.

## How It Works

The [`maven-resolver` project][maven-resolver] fortunately was written
with dependency injection in mind.  Specifically, it was written as a
[Guice module](https://github.com/google/guice/wiki/GettingStarted) to
be used from within Maven proper, which, at least in recent versions,
uses [Guice](https://github.com/google/guice/) under the covers.

The microBean Maven CDI project does just enough work to re-express
certain Maven Guice components using CDI constructs so that you may
simply inject the parts of `maven-resolver` that you need.

For example, to work with artifact repositories, you're going to need
a
[`RepositorySystem`](https://maven.apache.org/components/resolver/maven-resolver-api/apidocs/org/eclipse/aether/RepositorySystem.html)
and a
[`RepositorySystemSession`](https://maven.apache.org/components/resolver/maven-resolver-api/apidocs/org/eclipse/aether/RepositorySystemSession.html).
With microBean Maven CDI, you simply do this:

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private RepositorySystemSession repositorySystemSession;

&hellip;and the [`MavenExtension` portable
extension](apidocs/org/microbean/maven/cdi/MavenExtension.html) takes
care of setting up the dozens of supporting objects for you behind the
scenes.

microBean Maven CDI also natively understands your user-level
[`~/.m2/settings.xml` file](https://maven.apache.org/settings.html),
and can use it so that you can inject the right remote repositories,
even taking its [local
repository](https://maven.apache.org/settings.html#Simple_Values) and
[mirrors](https://maven.apache.org/guides/mini/guide-mirror-settings.html)
settings into consideration:

    @Inject
    @Resolution // for dependency resolution, as opposed to, say, deployment
    private List<RemoteRepository> remoteRepositories;

## Usage

Here is some pseudocode showing how you might go about resolving the
(arbitrarily chosen for this example) [`org.slf4j:slf4j-api:1.7.24`
artifact](http://search.maven.org/#artifactdetails%7Corg.slf4j%7Cslf4j-api%7C1.7.24%7Cjar)
from within your CDI bean:

    import org.eclipse.aether.RepositorySystemSession;
    import org.eclipse.aether.RepositorySystem;

    import org.eclipse.aether.artifact.Artifact;
    import org.eclipse.aether.artifact.DefaultArtifact;

    import org.eclipse.aether.collection.CollectRequest;

    import org.eclipse.aether.repository.RemoteRepository;

    import org.eclipse.aether.resolution.ArtifactResult;
    import org.eclipse.aether.resolution.DependencyRequest;
    import org.eclipse.aether.resolution.DependencyResolutionException;
    import org.eclipse.aether.resolution.DependencyResult;

    import org.eclipse.aether.util.artifact.JavaScopes;

    import org.eclipse.aether.util.filter.DependencyFilterUtils;

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private RepositorySystemSession session;

    @Inject
    @Resolution
    private List<RemoteRepository> remoteRepositories;

    public void resolve() throws DependencyResolutionException {
      final CollectRequest collectRequest = new CollectRequest();
      final Artifact artifact = new DefaultArtifact("org.slf4j", "slf4j-api", "jar", "1.7.24");
      collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
      collectRequest.setRepositories(remoteRepositories);

      final DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE));
      final DependencyResult dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest);
      final List<ArtifactResult> artifactResults = dependencyResult.getArtifactResults();
    }

[maven-resolver]: http://maven.apache.org/resolver
[sonatype-aether]: http://blog.sonatype.com/2010/08/introducing-aether/
[eclipse-aether]: https://projects.eclipse.org/projects/technology.aether

