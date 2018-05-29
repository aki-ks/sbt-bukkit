name := "sbt-bukkit"
organization := "me.aki.sbt"
version := "0.2.0-SNAPSHOT"

scalaVersion := "2.12.6"

lazy val root = (project in file("."))
  .settings(
    sbtPlugin := true,
    libraryDependencies += "org.yaml" % "snakeyaml" % "1.10",
    includeProtocol,

    Compile / resourceGenerators += Def.task {
      Seq[File]((bukkitPlugin / Compile / packageBin).value)
    }
  )

lazy val bukkitPlugin = (project in file("bukkit-plugin"))
  .settings(
    artifactName := { case (_, _, _) => "SbtBukkitPlugin.jar" },

    resolvers += "Bukkit releases" at "https://hub.spigotmc.org/nexus/content/groups/public/",
    libraryDependencies += "org.bukkit" % "bukkit" % "1.12.2-R0.1-SNAPSHOT",

    includeProtocol
  )

lazy val protocol = project in file("protocol")

lazy val includeProtocol =
  Compile / unmanagedSourceDirectories ++= (protocol / Compile / unmanagedSourceDirectories).value
