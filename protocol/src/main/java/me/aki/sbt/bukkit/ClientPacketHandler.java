package me.aki.sbt.bukkit;

import me.aki.sbt.bukkit.server.ClosePacket;
import me.aki.sbt.bukkit.server.RecompileResponsePacket;
import me.aki.sbt.bukkit.server.SbtStatusPacket;

public interface ClientPacketHandler extends PacketHandler {
    void onClose(ClosePacket packet);
    void onRecompileResponse(RecompileResponsePacket packet);
    void onSbtStatusResponse(SbtStatusPacket packet);
}
