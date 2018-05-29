package me.aki.sbt.bukkit;

import me.aki.sbt.bukkit.client.ClosePacket;
import me.aki.sbt.bukkit.client.GetSbtStatusPacket;
import me.aki.sbt.bukkit.client.RecompileRequestPacket;

public interface ServerPacketHandler extends PacketHandler {
    void onClose(ClosePacket packet);
    void onSbtStatusRequest(GetSbtStatusPacket packet);
    void onRecompileRequest(RecompileRequestPacket packet);
}
