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
    val config: Configuration

    override def projectSettings: Seq[Def.Setting[_]] =
      specs.flatMap(_.settings) ++
      specs.flatMap(_.specify(config).settings)
  }
}
