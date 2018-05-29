package me.aki.sbt.bukkit;

public interface PacketHandler {
    void close(String message);
    void onConnectionLost();
}
