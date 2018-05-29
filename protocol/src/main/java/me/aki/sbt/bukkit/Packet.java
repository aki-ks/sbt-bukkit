package me.aki.sbt.bukkit;

public interface Packet {
    void read(Decoder decoder);
    void write(Encoder encoder);
    void handlePacket(PacketHandler handler);
}
