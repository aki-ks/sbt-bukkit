package me.aki.sbt.bukkit.client;

import me.aki.sbt.bukkit.*;

public class GetSbtStatusPacket implements Packet {
    public void read(Decoder decoder) {}

    public void write(Encoder encoder) {}

    public void handlePacket(PacketHandler handler) {
        ((ServerPacketHandler)handler).onSbtStatusRequest(this);
    }
}
