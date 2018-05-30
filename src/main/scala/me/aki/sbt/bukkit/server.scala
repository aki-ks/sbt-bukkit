package me.aki.sbt.bukkit

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

  lazy val installSbtBukkitPlugin = TaskKey[Unit]("install-sbt-bukit-plugin")

  lazy val startServer = TaskKey[Unit]("start-server", "Compile plugins and start the server")

  lazy val bootServer = TaskKey[Unit]("boot-server")
  lazy val serverJar = TaskKey[Option[Seq[File]]]("server-jar", "Location of server jar and its dependencies")
  lazy val serverMainClass = SettingKey[String]("server-main-class")
  lazy val prepareServer = TaskKey[Unit]("prepare-server", "server-specific task run before server is booted")
  lazy val logLabel = SettingKey[String]("log-label")
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
    Bukkit / logLabel := "bukkit",
    Bukkit / serverMainClass := "org.bukkit.craftbukkit.Main",

    Bungee / serverApi := BungeeApi,
    Bungee / serverVersion := "1.12-SNAPSHOT",
    Bungee / serverDirectory := target.value / "bungee-server",
    Bungee / logLabel := "bungee",
    Bungee / serverMainClass := "net.md_5.bungee.Bootstrap",

    Bukkit / installSbtBukkitPlugin := {
      extractPlugin((Bukkit / serverDirectory).value, "SbtBukkitPlugin.jar")
    },

    Bukkit / prepareServer := {
      (Bukkit / installSbtBukkitPlugin).value

      IO.write((Bukkit / serverDirectory).value / "eula.txt", "eula=true")
    },

    Bungee / prepareServer := {},

    resolvers += Resolver.mavenLocal, // => craftbukkit & spigot by the "BuildTool"
    resolvers += Resolver.sonatypeRepo("snapshots"), // => bungeecord-api
    resolvers += "Bukkit releases" at "https://hub.spigotmc.org/nexus/content/groups/public/" // => bukkit-api & spigot-api
  )

  def extractPlugin(serverDir: File, name: String): Unit = {
    val pluginStream = getClass.getClassLoader.getResourceAsStream(name)
    IO.transfer(pluginStream, serverDir / "plugins" / name)
  }
}
class ServerSettings(val config: Configuration) extends SpecificSettingSpec {
  import Keys._

  val settings = Seq[Setting[_]](
    config / serverApiModule := configuredApiModuleId.value,

    libraryDependencies += (config / serverApiModule).value,

    config / startServer := {
      Def.sequential(
        config / packagePlugin,
        config / prepareServer,
        config / bootServer
      ).value
    },

    config / bootServer := {
      val logger = streams.value.log
      val sLogger = serverLogger.value
      val defaultOption = forkOptions.value
      val serverDir = (config / serverDirectory).value

      for(serverJar ← (config / serverJar).value) {
        serverDir.mkdirs

        val disableJline = "-Djline.terminal=jline.UnsupportedTerminal"
        val bridgeSettings = state.value.get(runningBridgeServer).flatten.map(bridge => Seq(
          s"-Dbridge.project=${thisProject.value.id}",
          s"-Dbridge.host=${bridge.host}",
          s"-Dbridge.port=${bridge.port}"
        )).getOrElse(Nil)

        val options = defaultOption
          .withRunJVMOptions(Vector(disableJline) ++ bridgeSettings)
          .withOutputStrategy(LoggedOutput(sLogger))
          .withWorkingDirectory(serverDir)
          .withConnectInput(true)

        new ForkRun(options).run((config / serverMainClass).value, serverJar, Seq(), sLogger)
      }
    },

    config / serverJar := {
      val logger = streams.value.log
      val resolution = (dependencyResolution in Compile).value
      val dependencyCacheDir = dependencyCacheDirectory.value

      resolution.retrieve(configuredServerModuleId.value, scalaModuleInfo.value, dependencyCacheDir, logger) match {
        case Left(_) => logServerJarError(logger, (config / serverApi).value); None
        case Right(files) => Some(files.distinct)
      }
    }
  )

  def logServerJarError(logger: Logger, server: ServerType): Unit = {
    val (serverName, jarKey) = server match {
      case SpigotApi | Spigot => ("spigot", spigotJar)
      case BukkitApi | CraftBukkit => ("craftbukkit", craftbukkitJar)
      case BungeeApi | BungeeCord => ("bungeecord", bungeecordJar)
    }

    logger.error(s"Could not find a $serverName jar. You have the following options:")
    logger.error(s"- Assign the path of a downloaded $serverName jar to the key '${jarKey.key.label}'")
    logger.error(s"- Add a repository containing the $serverName artifact")
  }

  lazy val serverLogger = Def.task[Logger] {
    new Logger {
      val logger = streams.value.log
      def transformMessage(message: String) = s"[${(config / logLabel).value}] $message"

      override def trace(t: => Throwable): Unit = logger.trace(t)
      override def success(message: => String): Unit = logger.success(transformMessage(message))
      override def log(level: Level.Value, message: => String): Unit = {
        message match {
          case "" | ">" | ">>" if config == Bungee =>
          case _ => logger.log(level, transformMessage(message))
        }
      }
    }
  }

  lazy val configuredServerModuleId = Def.settingDyn {
    (config / serverApi).value match {
      case BukkitApi | CraftBukkit => configuredCraftbukkitModule
      case SpigotApi | Spigot => configuredSpigotModule
      case BungeeApi | BungeeCord => configuredBungeecordModule
    }
  }

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
    (config / key).?.value match {
      case Some(jar) if jar.exists => moduleId.value from jar.toURI.toURL.toString
      case _ => moduleId.value
    }
  }
}
