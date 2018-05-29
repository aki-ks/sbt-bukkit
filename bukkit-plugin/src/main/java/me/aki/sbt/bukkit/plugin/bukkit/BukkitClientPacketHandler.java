package me.aki.sbt.bukkit.plugin.bukkit;

import me.aki.sbt.bukkit.ClientPacketHandler;
import me.aki.sbt.bukkit.server.ClosePacket;
import me.aki.sbt.bukkit.server.RecompileResponsePacket;
import me.aki.sbt.bukkit.server.SbtStatusPacket;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.HashSet;

public class BukkitClientPacketHandler implements ClientPacketHandler {
    private SbtBukkitPlugin plugin;
    private BridgeClient client;

    public BukkitClientPacketHandler(SbtBukkitPlugin plugin, BridgeClient client) {
        this.plugin = plugin;
        this.client = client;
    }

    @Override
    public void onClose(ClosePacket packet) {
        String message = packet.getMessage();
        Bukkit.getLogger().info("Bridge server closed connection: " + message);
        client.getManager().closeSocket();
    }

    @Override
    public void onSbtStatusResponse(SbtStatusPacket packet) {
        client.setAllProjects(new HashSet<>(Arrays.asList(packet.getAllProjects())));
    }

    @Override
    public void onRecompileResponse(RecompileResponsePacket packet) {
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getServer().reload());
    }

    @Override
    public void close(String message) {
        client.getManager().writePacket(new me.aki.sbt.bukkit.client.ClosePacket(message));
    }

    @Override
    public void onConnectionLost() {
        Bukkit.getLogger().warning("Lost connection to sbt");
    }
}
