package me.aki.sbt.bukkit

import sbt._

trait AggregateKeys {
  lazy val serverPlugins = SettingKey[Seq[ProjectReference]]("server-plugins")
}

object AggregateConfiguration extends CommonSettingSpec {
  import Keys._
  val settings = Seq(
    serverPlugins := Seq()
  )

  def specify(config: Configuration) = new AggregateConfiguration(config)
}
class AggregateConfiguration(val config: Configuration) extends SpecificSettingSpec {
  import Keys._
  val settings = Seq(
    config / packagePlugin := {
      val jars = Def.sequential(compilePlugins, pluginJars).value

      val pluginsDir = (config / serverDirectory).value / "plugins"
      for(jar ← jars) {
        IO.copyFile(jar, pluginsDir / jar.getName)
      }
    }
  )

  lazy val compilePlugins = Def.taskDyn {
    flattenTasks {
      for (plugin ← (config / serverPlugins).value)
        yield plugin / config / packagePlugin
    }
  }

  lazy val pluginJars = Def.taskDyn {
    flattenSettings {
      for (plugin ← (config / serverPlugins).value)
        yield plugin / config / packagedPluginFile
    }
  }

}