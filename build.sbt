ThisBuild / organization := "io.github.biochimia"
ThisBuild / description := "An sbt plugin to configure artifactory repositories"
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin, GitVersioning)
  .settings(
    sbtPlugin := true,
    name := "sbt-artifactory",

    publishMavenStyle := false,
    bintrayRepository := "sbt-plugins",
    bintrayOrganization in bintray := None,

    git.baseVersion := "0.1-adv",
    git.formattedShaVersion := {
      git.gitHeadCommit.value map { sha =>
        git.defaultFormatShaVersion(
          Some(git.formattedDateVersion.value),
          sha.slice(0, 8),
          git.makeUncommittedSignifierSuffix(
            git.gitUncommittedChanges.value,
            git.uncommittedSignifier.value))
      }
    },

    resolvers += "jgit-repository" at "https://repo.eclipse.org/content/groups/releases/",
    libraryDependencies ++= Seq(
      ("org.eclipse.jgit" % "org.eclipse.jgit" % "5.6.0.201912101111-r" % Compile),
    ),

    scriptedLaunchOpts ++= Seq("-Dplugin.version=" + version.value),
    scriptedBufferLog := false,
  )
