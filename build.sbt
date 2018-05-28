name := "sbt-bukkit"
organization := "me.aki.sbt"
version := "0.2.0-SNAPSHOT"

scalaVersion := "2.12.6"

lazy val root = (project in file("."))
  .settings(
    sbtPlugin := true,
    libraryDependencies += "org.yaml" % "snakeyaml" % "1.10"
  )