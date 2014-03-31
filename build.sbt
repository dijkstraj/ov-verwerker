name := "ov-verwerker"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.apache.poi" % "poi" % "3.10-FINAL",
  "org.apache.poi" % "poi-ooxml" % "3.10-FINAL",
  "com.github.nscala-time" %% "nscala-time" % "0.8.0"
)     

play.Project.playScalaSettings
