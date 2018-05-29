package me.aki.sbt.bukkit.server;

import me.aki.sbt.bukkit.*;

public class RecompileResponsePacket implements Packet {
    public void read(Decoder decoder) {}

    public void write(Encoder encoder) {}

    public void handlePacket(PacketHandler handler) {
        ((ClientPacketHandler)handler).onRecompileResponse(this);
    }
}
