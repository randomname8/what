lazy val root = Project(id = "what", base = file(".")).
  settings(commonSettings: _*).
  settings(
    name := "what",
    libraryDependencies ++= Seq(
      "io.aeron" % "aeron-driver" % "1.4.1",
      "org.jitsi" % "ice4j" % "1.0"
    )
  )

lazy val commonSettings = Seq(
  organization := "nani",
  version := "0.1.0",
  scalaVersion := "2.12.3",
  scalacOptions ++= Seq("-deprecation", "-feature", "-Ypartial-unification", "-Xlint", "-opt:_", "-opt-warnings:_"),
  fork := true,
  resolvers ++= Seq(
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
  )
)
