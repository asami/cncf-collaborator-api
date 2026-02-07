ThisBuild / organization := "org.goldenport"
name := "cncf-collaborator-api"
ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "3.3.7"

ThisBuild / publishArtifact := true

publishMavenStyle := true

publishTo := {
  val repo = sys.env.get("SIMPLEMODELING_MAVEN_LOCAL")
    .map(file)
    .getOrElse(baseDirectory.value / "maven-local")

  Some(
    Resolver.file(
      "local-simplemodeling-maven",
      repo
    )
  )
}

autoScalaLibrary := false

crossPaths := false

Compile / javacOptions ++= Seq(
  "--release",
  "21"
)

libraryDependencies += "org.junit.jupiter" % "junit-jupiter" % "5.10.0" % Test

Test / fork := true
Test / parallelExecution := false
Test / testFrameworks += new TestFramework("org.junit.platform.surefire.provider.JUnitPlatform")
Test / doc / sources := Seq.empty
