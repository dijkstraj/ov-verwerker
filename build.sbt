name := "ov-verwerker"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  cache,
  "org.apache.poi" % "poi" % "3.10-FINAL",
  "org.apache.poi" % "poi-ooxml" % "3.10-FINAL",
  "com.github.nscala-time" %% "nscala-time" % "1.4.0",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.15",
  "org.webjars" %% "webjars-play" % "2.3.0",
  "org.webjars" % "jquery" % "2.1.1",
  "org.webjars" % "bootswatch-lumen" % "3.2.0-1",
  "org.webjars" % "angularjs" % "1.3.0-beta.17",
  "org.webjars" % "angular-ui-bootstrap" % "0.11.0-2",
  "org.webjars" % "underscorejs" % "1.6.0-3"
) 