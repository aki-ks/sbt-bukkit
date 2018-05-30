package me.aki.sbt.bukkit

import scala.collection.immutable.ListMap
import sbt._
import sbt.Keys._
import xsbt.api.Discovery
import sjsonnew.BasicJsonProtocol._

trait MainClassDiscoveryKeys {
  lazy val discoveredPluginMainClasses = TaskKey[Seq[String]]("plugin-classes", "Discovered plugin main classes")
  lazy val pluginMainSuperclass = SettingKey[String]("plugin-main-superclass", "Class inherited by all plugin main classes")
}

object MainClassDiscoverySettings extends CommonSettingSpec {
  import Keys._
  def specify(config: Configuration) = new MainClassDiscoverySettings(config)
  val settings = Seq(
    Bukkit / pluginMainSuperclass := "org.bukkit.plugin.java.JavaPlugin",
    Bungee / pluginMainSuperclass := "net.md_5.bungee.api.plugin.Plugin"
  )
}
class MainClassDiscoverySettings(val config: Configuration) extends SpecificSettingSpec {
  import Keys._
  val settings = Seq[Setting[_]](
    config / discoveredPluginMainClasses := Def.taskDyn {
      val main = (config / pluginMainSuperclass).value
      compile in Compile map { analysis =>
        Discovery(Set(main), Set.empty)(Tests.allDefs(analysis)) collect {
          case (definition, discovered) if discovered.baseClasses contains main => definition.name
        }
      } storeAs (config / discoveredPluginMainClasses) triggeredBy (compile in Compile)
    }.value,

    config / pluginMain in packagePlugin := SelectMainClass(Some(SimpleReader readLine _), (discoveredPluginMainClasses in config).value),
    config / pluginMain := SelectMainClass(None, (discoveredPluginMainClasses in config).value),
  )
}

trait ManifestKeys extends ManifestDsl {
  lazy val generatePluginManifest = TaskKey[Seq[File]]("generate-plugin-manifest")
  lazy val pluginManifest = TaskKey[Map[String, Any]]("plugin-manifest", "Bukkit plugin manifest")
  lazy val pluginConfigName = SettingKey[String]("plugin-config-file", "Name of Yaml configuration file packaged into the plugin")

  lazy val pluginLibraryDependencies = SettingKey[Seq[ModuleID]]("plugin-library-dependencies", "Libraries packed into the generated jar file")

  sealed trait BukkitLoadTime
  case object OnStartup extends BukkitLoadTime
  case object PostWorld extends BukkitLoadTime

  lazy val pluginName = SettingKey[String]("plugin-name", "Name of the bukkit plugin.")
  lazy val pluginVersion = SettingKey[String]("plugin-version", "Version of the bukkit plugin.")
  lazy val pluginDescription = SettingKey[String]("plugin-description", "Description of the functionality the bukkit plugin provides.")
  lazy val pluginLoadTime = SettingKey[BukkitLoadTime]("plugin-load", "When the plugin should be loaded.")
  lazy val pluginAuthor = SettingKey[String]("plugin-author", "Author of the bukkit plugin.")
  lazy val pluginAuthors = SettingKey[Seq[String]]("plugin-authors", "Authors of the bukkit plugin.")
  lazy val pluginWebsite = SettingKey[String]("plugin-website", "The plugin's or author's website.")
  lazy val pluginDependencies = SettingKey[Seq[String]]("plugin-dependencies", "Plugins required to load this plugin")
  lazy val pluginSoftDependencies = SettingKey[Seq[String]]("plugin-soft-dependencies", "Plugins extending the functionality of the plugin.")
  lazy val pluginLoadBefore = SettingKey[Seq[String]]("plugin-load-before", "Plugins that must be loaded before this plugin.")
  lazy val pluginPrefix = SettingKey[String]("plugin-prefix", "Prefix of logging messages")
  lazy val pluginDatabase = SettingKey[Boolean]("plugin-database", "Does the plugin use a database.")
  lazy val pluginMain = TaskKey[Option[String]]("plugin-class", "Bukkit plugin main class")
  lazy val pluginCommands = SettingKey[Seq[BukkitCommand]]("plugin-commands")
  lazy val pluginPermissions = SettingKey[Seq[BukkitPermission]]("plugin-permissions")
}

trait ManifestDsl { self: ManifestKeys =>
  case class BukkitCommand(
    _name: String,
    _description: Option[String] = None,
    _usage: Option[String] = None,
    _aliases: Seq[String] = Seq(),
    _permission: Option[CommandPermission] = None
  ) {
    def description(description: String): BukkitCommand = copy(_description = Some(description))
    def usage(usage: String): BukkitCommand = copy(_usage = Some(usage))
    def alias(aliases: BukkitCommand*): BukkitCommand = copy(_aliases = _aliases ++ aliases.map(_._name))
    def permission(permission: CommandPermission): BukkitCommand = copy(_permission = Some(permission))
  }

  case class CommandPermission(permission: String, message: Option[String] = None) {
    def !(message: String) = copy(message = Some(message))
  }

  case class BukkitPermission(
    _name: String,
    _default: Option[Boolean] = None,
    _description: Option[String] = None,
    _children: Map[String, Boolean] = ListMap()
  ) {
    def default: BukkitPermission = default(true)
    def default(default: Boolean): BukkitPermission = copy(_default = Some(default))
    def description(description: String): BukkitPermission = copy(_description = Some(description))
    def children(children: (String, Boolean)*): BukkitPermission = copy(_children = _children ++ children)
  }

  import scala.language.dynamics
  object / extends Dynamic {
    def selectDynamic(command: String): BukkitCommand = BukkitCommand(command)
  }

  implicit def stringToCommandPermission(permission: String): CommandPermission = CommandPermission(permission)
  implicit def stringToPluginPermission(name: String): BukkitPermission = BukkitPermission(name)
}

object ManifestGeneratorSettings extends CommonSettingSpec {
  import Keys._
  def specify(config: Configuration) = new ManifestGeneratorSettings(config)
  val settings = Seq(
    Bukkit / pluginConfigName := "plugin.yml",
    Bungee / pluginConfigName := "bungee.yml",

    pluginName := name.value,
    pluginVersion := version.value,
    pluginDescription := description.value,

    pluginLibraryDependencies := Seq(),
    pluginCommands := Seq(),
    pluginPermissions := Seq(),
    pluginDependencies := Seq(),
    pluginSoftDependencies := Seq(),
    pluginLoadBefore := Seq(),

    pluginManifest := Map()
  )
}
class ManifestGeneratorSettings(val config: Configuration) extends SpecificSettingSpec {
  import Keys._

  val settings = Seq[Setting[_]](
    Compile / resourceGenerators += config / generatePluginManifest,
    config / generatePluginManifest := {
      val configuration = ScalaYaml dump (config / pluginManifest).value
      val target = resourceManaged.value / (config / pluginConfigName).value
      IO.write(target, configuration)
      Seq[File](target)
    },

    config / pluginManifest ++= {
      val name = "name" -> (config / pluginName).value
      val version = "version" -> (config / pluginVersion).value
      val description = for(description ← (config / pluginDescription).?.value) yield "description" -> description
      val load = (config / pluginLoadTime).?.value map {
        case OnStartup => "load" -> "STARTUP"
        case PostWorld => "load" -> "POSTWORLD"
      }
      val author = for(author ← (config / pluginAuthor).?.value) yield "author" -> author
      val authors = for(authors ← (config / pluginAuthors).?.value) yield "authors" -> authors
      val website = for(website ← (config / pluginWebsite).?.value) yield "website" -> website
      val main = "main" -> ((config / pluginMain).value getOrElse sys.error("No plugin main class detected."))
      val database = for(database ← (config / pluginDatabase).?.value) yield "database" -> database
      val prefix = for(prefix ← (config / pluginPrefix).?.value) yield "prefix" -> prefix
      val depends = for(deps ← (config / pluginDependencies).?.value if !deps.isEmpty) yield "depend" -> deps
      val softdepend = for(softDeps ← (config / pluginSoftDependencies).?.value if !softDeps.isEmpty) yield "softdepend" -> softDeps
      val loadBefore = for(loadBefore ← (config / pluginLoadBefore).?.value if !loadBefore.isEmpty) yield "loadbefore" -> loadBefore

      val commands = {
        val commands = for(cmd ← (config / pluginCommands).value) yield {
          val usage = for(usage ← cmd._usage) yield "usage" -> usage
          val description = for(description ← cmd._description) yield "description" -> description
          val aliases = if (cmd._aliases.isEmpty) None else Some("aliases" -> cmd._aliases)
          val permission = for(permission ← cmd._permission) yield "permission" -> permission.permission
          val permissionMessage = for {
            permission ← cmd._permission
            message ← permission.message
          } yield "permission-message" -> message

          cmd._name -> (ListMap(usage.toSeq : _*) ++ description ++ aliases ++ permission ++ permissionMessage)
        }

        if(commands.isEmpty) None
        else Some("commands" -> ListMap(commands : _*))
      }

      val permissions = {
        val permissions = for(perm ← (config / pluginPermissions).value) yield {
          val description = for(description ← perm._description) yield "description" -> description
          val default = for(default ← perm._default) yield "default" -> default
          val children = if(perm._children.isEmpty) Seq() else Seq("children" -> perm._children)
          perm._name -> (ListMap(description.toSeq : _*) ++ default ++ children)
        }

        if(permissions.isEmpty) None
        else Some("permissions" -> ListMap(permissions : _*))
      }

      (ListMap(name, version) ++ description ++ load ++ author ++ authors ++ website ++ Map(main) ++
        database ++ prefix ++ depends ++ softdepend ++ loadBefore ++ commands ++ permissions)
    }
  )
}
