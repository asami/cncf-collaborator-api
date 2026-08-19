ThisBuild / organization := "org.goldenport"
name := "cncf-collaborator-api"
ThisBuild / version := "0.2.0"
ThisBuild / scalaVersion := "3.3.8"

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
  "17"
)

libraryDependencies += "com.github.sbt.junit" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test

Test / fork := true
Test / parallelExecution := false
Test / doc / sources := Seq.empty
