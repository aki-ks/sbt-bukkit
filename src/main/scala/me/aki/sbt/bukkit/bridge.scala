package me.aki.sbt.bukkit

import sbt._
import sbt.Keys._
import java.net.ServerSocket
import java.io.IOException

import me.aki.sbt.bukkit.server.{RecompileResponsePacket, SbtStatusPacket}

import scala.util.Try

trait BridgeKeys {
  lazy val runningBridgeServer = AttributeKey[Option[BridgeServer]]("running-bridge-server")
}

object BridgeSettings extends CommonSettingSpec {
  import Keys._
  import sbt.complete.DefaultParsers._

  override def settings: Seq[Setting[_]] = Seq(
    commands += Command("bridge")(_ => token(Space) ~> (token("start") | token("stop") )) {
      case (state, "start") =>
        for(oldServer ← state.get(runningBridgeServer).flatten) {
          oldServer.close
          state.log.info(s"Stopped bridge server running on port ${oldServer.port}")
        }

        val server = new BridgeServer(state)
        state.put(runningBridgeServer, Some(server))

      case (state, "stop") =>
        state.get(runningBridgeServer).flatten match {
          case Some(bridge) => bridge.close
          case None => state.log.info("bukkit bridge is not running")
        }
        state.put(runningBridgeServer, None)

      case _ => throw new IllegalStateException("Unreachable")
    }
  )

  override def specify(config: Configuration) = new BridgeSettings(config)
}
class BridgeSettings(val config: Configuration) extends SpecificSettingSpec {
  val settings = Seq()
}

/**
  * Start a server that allows controlling sbt.
  * E.g. recompiling a project.
  *
  * It must be initialized with a state gather from a command
  */
class BridgeServer(state: State) {
  private val serverSocket = Try(new ServerSocket(40124)) getOrElse new ServerSocket(0)

  def host = serverSocket.getInetAddress.getHostAddress
  def port = serverSocket.getLocalPort

  private var clients = Set[SocketManager]()

  val worker = new Thread {
    override def run: Unit = {
      state.log.info(s"Bridge server started on port $port")

      try {
        while (!isInterrupted) {
          val clientSocket = serverSocket.accept()
          state.log.info(s"Bridge client connected from ${clientSocket.getInetAddress.getHostAddress}:${clientSocket.getPort}")

          val handler = new SbtServerPacketHandler(null, state)
          val manager = new SocketManager(clientSocket, Protocol.CLIENT_PROTOCOL, Protocol.SERVER_PROTOCOL, handler)
          handler.manager = manager
          clients += manager

          manager.start
        }
      } catch {
        case _: IOException if serverSocket.isClosed =>
        case e: Throwable => e.printStackTrace()
      }

      for(client ← clients)
        try client.close("Sbt server shutdown")
        catch { case t: Throwable => t.printStackTrace() }
    }
  }

  worker.start

  def close = {
    worker.interrupt
    serverSocket.close
  }
}

class SbtServerPacketHandler(var manager: SocketManager, state: State) extends ServerPacketHandler {
  import Keys._
  val allProjects = Project.extract(state).structure.allProjects
  val configurations = Bukkit :: Bungee :: Nil

  override def onClose(packet: client.ClosePacket): Unit = {
    val socket = manager.getSocket
    val address = s"${socket.getInetAddress.getHostAddress}:${socket.getPort}"
    val message = packet.getMessage

    state.log.info(s"Bridge client $address closed connection: $message")
    manager.closeSocket()
  }

  override def onSbtStatusRequest(packet: client.GetSbtStatusPacket): Unit = {
    val projects = Project.extract(state).structure.allProjects.map(_.id).toArray

    manager.writePacket(new SbtStatusPacket(projects))
  }

  override def onRecompileRequest(packet: client.RecompileRequestPacket): Unit = {
    val config = configurations.find(_.name == packet.getConfiguration) getOrElse
      { throw new IllegalArgumentException("No such configuration " + packet) }

    val project = allProjects.find(_.id == packet.getProjectId) getOrElse
      { throw new IllegalArgumentException("No such project " + packet) }

    Project.runTask(LocalProject(project.id) / config / Keys.packagePlugin, state)

    manager.writePacket(new RecompileResponsePacket())
  }

  override def close(message: String): Unit = {
    val socket = manager.getSocket
    state.log.info(s"Closing connection to bridge client ${socket.getInetAddress.getHostAddress}:${socket.getPort}: $message")

    manager.writePacket(new server.ClosePacket(message))
  }

  override def onConnectionLost(): Unit = {
    val socket = manager.getSocket
    state.log.info(s"Lost connection to bridge client ${socket.getInetAddress.getHostAddress}:${socket.getPort}")
  }
}
