package me.aki.sbt.bukkit

import sbt._
import sbt.Keys._

abstract class AbstractPluginPlugin extends AutoSpecPlugin {
  val autoImport = Keys
  val specs = Seq(MainClassDiscoverySettings, ManifestGeneratorSettings, PackagingSettings, ServerSettings)
}

object BukkitPlugin extends AbstractPluginPlugin {
  import autoImport._
  val configs = Seq(Bukkit)
}

object BungeePlugin extends AbstractPluginPlugin {
  import autoImport._
  val configs = Seq(Bungee)
}

object AggregatePlugin extends AutoSpecPlugin {
  val autoImport = Keys
  import autoImport._

  override val specs = Seq(AggregateConfiguration, BridgeSettings, ServerSettings)
  override val configs = Seq(Bukkit, Bungee)
}