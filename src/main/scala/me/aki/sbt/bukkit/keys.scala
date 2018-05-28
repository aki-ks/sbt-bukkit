package me.aki.sbt.bukkit

import sbt._

trait Configurations {
  val Bukkit = config("bukkit")
  val Bungee = config("bungee")
}

object Keys extends Keys
trait Keys extends Configurations
  with MainClassDiscoveryKeys
  with ManifestKeys
  with PackagingKeys
  with ServerKeys
