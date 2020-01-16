ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.12.10"

credentials += artifactory.creds
credentials += Credentials.artifactoryCreds

resolvers += artifactory.repo("libs-release")
resolvers += Resolver.artifactoryRepo("libs-release")

val buildConfigMap = settingKey[Map[String, String]]("Define environment variables for the build container")

buildConfigMap := {
  val creds = Credentials.toDirect(artifactory.creds)
  val alternateCreds = Credentials.toDirect(Credentials.artifactoryCreds)

  Map(
    "ARTIFACTORY_CONTEXT" -> artifactory.context.value,
    "ARTIFACTORY_DOCKER_REGISTRY" -> artifactory.dockerRegistry.value,
    "ARTIFACTORY_USER" -> creds.userName,
    "ARTIFACTORY_PWD" -> alternateCreds.passwd,
  )
}

// TODO: Configure a repository and get something from there
// addSbtPlugin("com.example" %% "sbt-plugin" % "0.1.0")
