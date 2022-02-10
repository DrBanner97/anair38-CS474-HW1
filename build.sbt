//ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

val scalacticVersion = "3.2.9"


libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalacticVersion % Test,
  "org.scalatest" %% "scalatest-featurespec" % scalacticVersion % Test)

lazy val root = (project in file("."))
  .settings(
    name := "anair38-474-hw1"
  )
