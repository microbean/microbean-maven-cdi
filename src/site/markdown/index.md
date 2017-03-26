# MicroBean Maven CDI

The MicroBean Maven CDI project embeds the [Maven machinery responsible
for interacting with artifact repositories][2] into your CDI 2.0
environment.

## Background and History

The internal Maven machinery for interacting with artifact
repositories has gone through several phases of existence.

Its earliest incarnation is in the form of
the [`maven-artifact-resolver`][3] artifact, version 1.0, _circa_ 2009
(here in 2017 this artifact should not be used).

This code was subsequently [broken out as a standalone project][6]
into [Sonatype][4]'s [&AElig;ther project][5] in [August 2010][6].

Sonatype &AElig;ther then moved to the Eclipse foundation
in [August 2014][7], but never really caught on as a standalone
project outside of its original Maven-oriented user base, and
was [archived in January 2015][7].

The Sonatype &AElig;ther project was handed _back_ to the Maven
project and reincarnated as the [Maven Artifact Resolver project][2]
in January 2017, with an overarching artifact identifier
of [`maven-resolver`][8] (not `maven-artifact-resolver`).  Somewhat
oddly, the package names have not changed, so the classes provided by
the `maven-resolver` project all start with the `org.eclipse.aether.`
prefix.

This project adapts the [`maven-resolver`][8] project to CDI
environments.

## Installation

Place this project's `.jar` file on your classpath.

## How It Works

The [`maven-resolver` project][2] fortunately was written with
dependency injection in mind.  Specifically, it was written as
a [Guice module][9] to be used from within Maven proper, which, at
least in recent versions, uses [Guice][10] under the covers.

It also depends on certain parts of Maven that are expressed as [Plexus
components][11].  (Plexus was one of the first dependency injection
containers and still has its fingerprints in the Maven innards.)

The MicroBean Maven CDI project does just enough work to re-express
certain Plexus components using CDI constructs so that you may simply
inject the parts of `maven-resolver` that you need.

For example, to work with artifact repositories, you're going to need
a [`RepositorySystem`][12] and a [`RepositorySystemSession`][13].
With MicroBean Maven CDI, you simply do this:

    @Inject
    private RepositorySystem repositorySystem;
    
    @Inject
    private RepositorySystemSession repositorySystemSession;
    
&hellip;and the [`MavenExtension` portable extension][14] takes care
of setting up the dozens of supporting objects for you behind the
scenes.

MicroBean Maven CDI also natively understands your
user-level [`~/.m2/settings.xml` file][15], and can use it so that you
can inject the right remote repositories, even taking
its [local repository][17] and [mirrors][16] settings into
consideration:

    @Inject
    @Resolution // for dependency resolution, as opposed to, say, deployment
    private List<RemoteRepository> remoteRepositories;
    
## Usage

Here is some pseudocode showing how you might go about resolving the
(arbitrarily chosen for this
example) [`org.slf4j:slf4j-api:1.7.24` artifact][18] from within your
CDI bean:

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


[1]: http://maven.apache.org/
[2]: http://maven.apache.org/resolver
[3]: http://search.maven.org/#artifactdetails%7Corg.apache.maven.shared%7Cmaven-artifact-resolver%7C1.0%7Cjar 
[4]: https://www.sonatype.com/
[5]: https://github.com/sonatype/sonatype-aether
[6]: http://blog.sonatype.com/2010/08/introducing-aether/
[7]: https://projects.eclipse.org/projects/technology.aether
[8]: http://search.maven.org/#artifactdetails%7Corg.apache.maven.resolver%7Cmaven-resolver%7C1.0.3%7Cpom
[9]: https://github.com/google/guice/wiki/GettingStarted
[10]: https://github.com/google/guice/
[11]: https://codehaus-plexus.github.io/plexus-components/
[12]: https://maven.apache.org/components/resolver/maven-resolver-api/apidocs/org/eclipse/aether/RepositorySystem.html
[13]: https://maven.apache.org/components/resolver/maven-resolver-api/apidocs/org/eclipse/aether/RepositorySystemSession.html
[14]: apidocs/org/microbean/maven/cdi/MavenExtension.html
[15]: https://maven.apache.org/settings.html
[16]: https://maven.apache.org/guides/mini/guide-mirror-settings.html
[17]: https://maven.apache.org/settings.html#Simple_Values
[18]: http://search.maven.org/#artifactdetails%7Corg.slf4j%7Cslf4j-api%7C1.7.24%7Cjar
[19]: https://maven.apache.org/resolver/apidocs/org/eclipse/aether/collection/CollectRequest.html
