package me.aki.sbt.bukkit

import me.aki.sbt.bukkit.Keys.{Bukkit, Bungee}

import sbt._
import sbt.Keys._

trait PackagingKeys {
  lazy val packagePlugin = TaskKey[Unit]("package-plugin")

  lazy val dependencyJars = TaskKey[Seq[File]]("dependency-jars", "Jar files that will be packed into the packaged plugin jars")

  lazy val pluginJarName = SettingKey[String]("plugin-jar-name", "Name of packaged jar file")
  lazy val pluginDirFolder = SettingKey[File]("plugin-dir-folder", "Directory where the packaged jar will be stored")
  lazy val packagedPluginFile = SettingKey[File]("packaged-plugin-file")
}

object PackagingSettings extends CommonSettingSpec {
  val settings = Seq()
  def specify(config: Configuration) = new PackagingSettings(config)
}
class PackagingSettings(val config: Configuration) extends SpecificSettingSpec {
  import Keys._

  val settings = Seq[Setting[_]](
    config / pluginJarName := (config / pluginName).value + ".jar",
    config / pluginDirFolder := (config / serverDirectory).value / "plugins",
    config / packagedPluginFile := (config / pluginDirFolder).value / (config / pluginJarName).value,

    config / dependencyJars := libraryDependencyJars.value ++ scalaRuntimeJarOption.value,

    config / packagePlugin := {
      val logger = streams.value.log

      (Compile / compile).value
      val compiledDirs = (products in Compile).value

      logger.info("Extracting dependencies...")
      val extractDir = crossTarget.value / "extracted_libs"
      val dependencies = (config / dependencyJars).value
      IO.delete(extractDir)
      for(jar ← dependencies)IO.unzip(jar, extractDir)

      logger.info("Packaging plugin jar...")
      val zipEntries = for {
        classDirectory ← extractDir +: compiledDirs
        file ← listFilesRecursive(classDirectory) if file.isFile
      } yield (file, IO.relativize(classDirectory, file).get)

      IO.zip(zipEntries, (config / packagedPluginFile).value)
    }
  )

  private lazy val libraryDependencyJars = Def.task {
    val logger = streams.value.log
    val resolution = (dependencyResolution in Compile).value
    val dependencyCacheDir = dependencyCacheDirectory.value
    val dependencyModules = (config / pluginLibraryDependencies).value

    val dependencyJars = dependencyModules.distinct flatMap { moduleId =>
      resolution.retrieve(moduleId, scalaModuleInfo.value, dependencyCacheDir, logger) match {
        case Left(_) => sys.error(s"Retrieval of ${moduleId} failed.")
        case Right(files) => files
      }
    }

    dependencyJars.distinct
  }

  private lazy val scalaRuntimeJarOption = Def.taskDyn[Option[File]] {
    if(autoScalaLibrary.value) {
      for(si ← scalaInstance) yield Some(si.libraryJar)
    } else {
      Def.task(None)
    }
  }

  private def listFilesRecursive(f: File): Array[File] =
    if (f.isDirectory) f +: IO.listFiles(f).flatMap(listFilesRecursive)
    else Array(f)
}
