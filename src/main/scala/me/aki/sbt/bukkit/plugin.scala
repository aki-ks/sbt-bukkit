package me.aki.sbt.bukkit

import sbt.{Def, _}
import sbt.Keys._

abstract class AbstractPluginPlugin extends AutoSpecPlugin {
  val autoImport = Keys
  val specs = Seq(MainClassDiscoverySettings, ManifestGeneratorSettings, PackagingSettings, ServerSettings)
}

object BukkitPlugin extends AbstractPluginPlugin {
  import autoImport._
  val config = Bukkit
}

object BungeePlugin extends AbstractPluginPlugin {
  import autoImport._
  val config = Bungee
}
