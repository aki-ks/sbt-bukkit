package me.aki.sbt.bukkit

import scala.collection.JavaConverters._
import org.yaml.snakeyaml.{DumperOptions, Yaml}
import org.yaml.snakeyaml.nodes.{CollectionNode, Node, SequenceNode, Tag}
import org.yaml.snakeyaml.representer.{Represent, Representer}

object ScalaYaml extends Yaml(ScalaRepresenter, {
  val options = new DumperOptions
  options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
  options
})

object ScalaRepresenter extends ScalaRepresenter
class ScalaRepresenter extends Representer {
  multiRepresenters.put(classOf[scala.collection.Map[_, _]], MapRepresenter)
  multiRepresenters.put(classOf[Iterable[_]], IterableRepresenter)

  object IterableRepresenter extends Represent {
    override def representData(data: scala.Any): Node = {
      val jIter = data.asInstanceOf[Iterable[_]].asJava
      representSequence(getTag(data.getClass, Tag.SEQ), jIter, null)
    }
  }

  object MapRepresenter extends Represent {
    override def representData(data: scala.Any): Node = {
      val jmap = data.asInstanceOf[scala.collection.Map[_, AnyRef]].asJava
      representMapping(getTag(data.getClass, Tag.MAP), jmap, null)
    }
  }
}