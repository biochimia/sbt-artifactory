sys.props.get("plugin.version") match {
  case Some(version) => {
    addSbtPlugin("io.github.biochimia" %% "sbt-artifactory" % version)
  }
  case _ =>
    sys.error(
      """The system property 'plugin.version' is not defined.
        |Specify this property using scriptedLaunchOpts -D.""".stripMargin)
}
