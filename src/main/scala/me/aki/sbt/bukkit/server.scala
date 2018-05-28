package me.aki.sbt.bukkit

import me.aki.sbt.bukkit.Keys.{Bukkit, Bungee}
import sbt._
import sbt.Keys._
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName

trait ServerKeys {
  sealed trait ServerType
  sealed trait ServerApi extends ServerType
  sealed trait ServerImplementation extends ServerType
  case object BukkitApi extends ServerApi
  case object SpigotApi extends ServerApi
  case object BungeeApi extends ServerApi
  case object CraftBukkit extends ServerImplementation
  case object Spigot extends ServerImplementation
  case object BungeeCord extends ServerImplementation

  lazy val serverApi = SettingKey[ServerType]("server-api", "What api should your plugin be compiled against")
  lazy val serverVersion = SettingKey[String]("server-version", "Which api version should be used")
  lazy val serverApiModule = SettingKey[ModuleID]("server-api-module")

  lazy val serverDirectory = SettingKey[File]("server-directory")

  lazy val craftbukkitJar = SettingKey[File]("craft-bukkit-jar", "Location of a local craftbukkit server jar")
  lazy val spigotJar = SettingKey[File]("spigot-jar", "Location of a local spigot server jar")
  lazy val bungeecordJar = SettingKey[File]("bungeecord-jar", "Location of a local bungeecord server jar")
}

object ServerSettings extends CommonSettingSpec {
  import Keys._
  def specify(config: Configuration) = new ServerSettings(config)
  val settings = Seq(
    // https://hub.spigotmc.org/nexus/content/groups/public/org/spigotmc/spigot-api/
    // https://oss.sonatype.org/content/groups/public/net/md-5/bungeecord-api/
    Bukkit / serverApi := BukkitApi,
    Bukkit / serverVersion := "1.12.2-R0.1-SNAPSHOT",
    Bukkit / serverDirectory := target.value / "bukkit-server",

    Bungee / serverApi := BungeeApi,
    Bungee / serverVersion := "1.12-SNAPSHOT",
    Bungee / serverDirectory := target.value / "bungee-server",

    resolvers += Resolver.mavenLocal, // => craftbukkit & spigot by the "BuildTool"
    resolvers += Resolver.sonatypeRepo("snapshots"), // => bungeecord-api
    resolvers += "Bukkit releases" at "https://hub.spigotmc.org/nexus/content/groups/public/" // => bukkit-api & spigot-api
  )
}
class ServerSettings(val config: Configuration) extends SpecificSettingSpec {
  import Keys._

  val settings = Seq[Setting[_]](
    config / serverApiModule := configuredApiModuleId.value,
    libraryDependencies += (config / serverApiModule).value
  )

  lazy val configuredApiModuleId = Def.settingDyn {
    (config / serverApi).value match {
      case BukkitApi => unconfiguredBukkitApiModule
      case SpigotApi => unconfiguredSpigotApiModule
      case BungeeApi => unconfiguredBungeeApiModule
      case CraftBukkit => configuredCraftbukkitModule
      case Spigot => configuredSpigotModule
      case BungeeCord => configuredBungeecordModule
    }
  }

  lazy val unconfiguredBukkitApiModule = unconfigured("org.bukkit" % "bukkit")
  lazy val unconfiguredSpigotApiModule = unconfigured("org.spigotmc" % "spigot-api")
  lazy val unconfiguredBungeeApiModule = unconfigured("net.md-5" % "bungeecord-api")

  lazy val unconfiguredCraftbukkitModule = unconfigured("org.bukkit" % "craftbukkit")
  lazy val unconfiguredSpigotModule = unconfigured("org.spigotmc" % "spigot")
  lazy val unconfiguredBungeecordModule = unconfigured("net.md-5" % "bungeecord-bootstrap")

  lazy val configuredCraftbukkitModule = configured(unconfiguredCraftbukkitModule, craftbukkitJar)
  lazy val configuredSpigotModule = configured(unconfiguredSpigotModule, spigotJar)
  lazy val configuredBungeecordModule = configured(unconfiguredBungeecordModule, bungeecordJar)

  private def unconfigured(name: OrganizationArtifactName) = Def.setting {
    name % (config / serverVersion).value
  }

  private def configured(moduleId: Def.Initialize[ModuleID], key: SettingKey[File]) = Def.setting {
    (config / key).value match {
      case jar if jar.exists => moduleId.value from jar.toURI.toURL.toString
      case _ => moduleId.value
    }
  }
}
