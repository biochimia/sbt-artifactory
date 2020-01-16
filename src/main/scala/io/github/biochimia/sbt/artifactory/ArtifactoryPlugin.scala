//  Copyright 2020 João Abecasis
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package io.github.biochimia.sbt.artifactory

import sbt._
import Keys._

import org.eclipse.jgit.transport.NetRC

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object ArtifactoryPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  val DefaultArtifactoryContext = "https://artifactory.mpi-internal.com/artifactory"
  val DefaultArtifactoryDockerRegistry = "containers.mpi-internal.com"
  val DefaultArtifactoryRealm = "Artifactory Realm"

  object ArtifactoryKeys {
    val artifactoryContext = settingKey[String]("Artifactory context URL")
    val artifactoryDockerRegistry = settingKey[String]("Docker registry domain for Artifactory")
    val artifactoryCredentials = settingKey[Credentials]("Credentials for Artifactory")
  }

  object Macros {
    def artifactoryCreds_impl(c: Context): c.Expr[Credentials] = {
      import c.universe._
      reify {
        ArtifactoryKeys.artifactoryCredentials.value
      }
    }

    def artifactoryRepo_impl(c: Context)(repoName: c.Expr[String]): c.Expr[MavenRepository] = {
      import c.universe._
      reify {
        buildRepository(ArtifactoryKeys.artifactoryContext.value, repoName.splice)
      }
    }
  }

  object artifactory {
    val context = ArtifactoryKeys.artifactoryContext
    val dockerRegistry = ArtifactoryKeys.artifactoryDockerRegistry

    def creds: Credentials = macro Macros.artifactoryCreds_impl
    def repo(repoName: String): MavenRepository = macro Macros.artifactoryRepo_impl
  }

  object autoImport {
    val artifactory = ArtifactoryPlugin.artifactory

    implicit class ArtifactoryCredentialsSyntax(val credentials: Credentials.type) extends AnyVal {
      def artifactoryCreds: Credentials = macro Macros.artifactoryCreds_impl
    }

    implicit class ArtifactoryResolverSyntax(val resolver: Resolver.type) extends AnyVal {
      def artifactoryRepo(repoName: String): MavenRepository = macro Macros.artifactoryRepo_impl
    }
  }

  def buildRepository(context: String, repoName: String): MavenRepository = {
    DefaultArtifactoryRealm at s"${context}/${repoName}"
  }

  private[this] def buildCredentials(context: String, log: Logger): Credentials = {
    val host = new URL(context).getAuthority
    fetchCredentialsFromEnv orElse {
      fetchCredentialsFromNetRC(host)
    } match {
      case Some((userName, passwd)) =>
        Credentials(DefaultArtifactoryRealm, host, userName, passwd)
      case _ => {
        issueWarning(context, host, log)
        Credentials(IO.withTemporaryFile("artifactory-credentials-", ".not-found") { file => file })
      }
    }
  }

  private[this] def fetchCredentialsFromEnv: Option[(String, String)] =
    (sys.env.get("ARTIFACTORY_USER"), sys.env.get("ARTIFACTORY_PWD")) match {
      case (Some(userName), Some(passwd)) => Some((userName, passwd))
      case _ => None
    }

  private[this] def fetchCredentialsFromNetRC(host: String): Option[(String, String)] = {
    new NetRC().getEntry(host) match {
      case entry: NetRC.NetRCEntry => Some((entry.login, new String(entry.password)))
      case _ => None
    }
  }

  override def buildSettings = Seq(
    ArtifactoryKeys.artifactoryContext :=
      sys.env.getOrElse("ARTIFACTORY_CONTEXT", DefaultArtifactoryContext).stripSuffix("/"),
    ArtifactoryKeys.artifactoryDockerRegistry :=
      sys.env.getOrElse("ARTIFACTORY_DOCKER_REGISTRY", DefaultArtifactoryDockerRegistry),
    ArtifactoryKeys.artifactoryCredentials :=
      buildCredentials(ArtifactoryKeys.artifactoryContext.value, sLog.value),
  )

  @volatile
  private[this] var alreadyWarned = false

  private[this] def issueWarning(context: String, host: String, log: Logger) = this.synchronized {
    if (!alreadyWarned) {
      log.warn(s"""
        |**************** Unable to resolve artifactory credentials ****************

        |    In a continuous integration system, be sure to setup the
        |    environment variables ARTIFACTORY_CONTEXT, ARTIFACTORY_USER, and
        |    ARTIFACTORY_PWD. (In Travis, credentials will only be available to
        |    non-fork builds.)

        |    For local development, set up your credentials in ~/.netrc, by
        |    using using the following the template:

        |      # Generate / Revoke your personal Artifactory API key from
        |      # ${context}/webapp/#/profile
        |      machine ${host}
        |        login your.email@example.com
        |        password ARTIFACTORY-API-KEY

        |***************************************************************************
        """.stripMargin.trim)

      alreadyWarned = true
    }
  }
}
