# `sbt-artifactory`

The `sbt-artifactory` plugin offers syntactic sugar to help configure enterprise
Artifactory repositories in `sbt` projects.

## Installation

Enable the plugin globally by adding the snippet below to
`~/.sbt/1.0/plugins/build.sbt`.

```
resolvers += Resolver.bintrayIvyRepo("biochimia", "sbt-plugins"),
addSbtPlugin("io.github.biochimia" % "sbt-artifactory" % "0.1-adv-XXX")
```

This allows every project to use the enterprise repositories for plugins and
dependencies. It does not, by itself, add repositories or credentials to builds,
to avoid polluting other projects.

## Configuration

Override the default configuration globally by setting `artifactory.context` and
`artifactory.dockerRegistry` in `~/.sbt/1.0/build.sbt`:

```
artifactory.context := "https://artifactory.my-enterprise.com/artifactory"
artifactory.dockerRegistry := "containers.my-enterprise.com"
```

### Credentials

In a continuous integration setup, credentials for the enterprise repository can
be picked up from environment variables:

- `ARTIFACTORY_CONTEXT`, overrides `artifactory.context`;
- `ARTIFACTORY_DOCKER_REGISTRY`, overrides `artifactory.dockerRegistry`;
- `ARTIFACTORY_USER`, sets the authentication user;
- `ARTIFACTORY_PWD`, sets the authentication password.

Developers may want to keep repository credentials in `~/.netrc` by editing and
adding this snippet to the file:

```
machine artifactory.my-enterprise.com
  login your.email@my-enterprise.com
  password ARTIFACTORY-API-KEY
```

(The `~/.netrc` file is read up by other tools (e.g., `git`, Python's `pip`), to
similarly interact with authenticated resources.)


## Usage

Setting up credentials:

```
credentials += Credentials.artifactoryCreds

// or
credentials += artifactory.creds
```

Adding a package repository:

```
resolvers += Resolver.artifactoryRepo("libs-release")

// or
resolvers += artifactory.Repo("libs-release")
```

Accessing the credentials, in case you need to pass them along to a docker
container:

```
val buildConfigMap = settingKey[Map[String, String]]("Define environment variables for the build container")

buildConfigMap := {
  val creds = Credentials.toDirect(artifactory.creds)

  // or
  val alternateCreds = Credentials.toDirect(Credentials.artifactoryCreds)

  Map(
    "ARTIFACTORY_CONTEXT" -> artifactory.context.value,
    "ARTIFACTORY_DOCKER_REGISTRY" -> artifactory.dockerRegistry.value,
    "ARTIFACTORY_USER" -> creds.userName,
    "ARTIFACTORY_PWD" -> alternateCreds.passwd,
  )
}
```
