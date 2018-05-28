package me.aki.sbt

import sbt._

package object bukkit {
  trait CommonSettingSpec {
    def settings: Seq[Setting[_]]
    def specify(config: Configuration) : SpecificSettingSpec
  }
  trait SpecificSettingSpec {
    def config: Configuration
    def settings: Seq[Setting[_]]
  }

  trait AutoSpecPlugin extends AutoPlugin {
    val specs: Seq[CommonSettingSpec]
    val configs: Seq[Configuration]

    override def projectSettings: Seq[Def.Setting[_]] =
      specs.flatMap(spec => spec.settings ++ configs.flatMap(spec.specify(_).settings))
  }

  def flattenSettings[A](tasks: Seq[Def.Initialize[A]]): Def.Initialize[Task[List[A]]] =
    tasks.toList match {
      case Nil => Def.task { Nil }
      case x :: xs => Def.settingDyn {
        flattenSettings(xs) map (x.value :: _)
      }
    }

  def flattenTasks[A](tasks: Seq[Def.Initialize[Task[A]]]): Def.Initialize[Task[List[A]]] =
    tasks.toList match {
      case Nil => Def.task { Nil }
      case x :: xs => Def.taskDyn {
        flattenTasks(xs) map (x.value :: _)
      }
    }
}
