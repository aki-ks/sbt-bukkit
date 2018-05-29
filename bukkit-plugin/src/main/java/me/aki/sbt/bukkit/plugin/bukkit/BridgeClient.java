package me.aki.sbt.bukkit.plugin.bukkit;

import me.aki.sbt.bukkit.Protocol;
import me.aki.sbt.bukkit.SocketManager;
import me.aki.sbt.bukkit.client.GetSbtStatusPacket;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;

public class BridgeClient {
    private final SbtBukkitPlugin plugin;
    private final SocketManager manager;
    private final BukkitClientPacketHandler handler;

    private Set<String> allProjects;

    public BridgeClient(SbtBukkitPlugin plugin, String host, int port) throws IOException {
        this.plugin = plugin;
        this.handler = new BukkitClientPacketHandler(plugin, this);
        this.manager = new SocketManager(new Socket(host, port), Protocol.SERVER_PROTOCOL, Protocol.CLIENT_PROTOCOL, handler);
        this.manager.start();

        this.manager.writePacket(new GetSbtStatusPacket());
    }

    public SocketManager getManager() {
        return manager;
    }

    public Set<String> getAllProjects() {
        return allProjects;
    }

    public void setAllProjects(Set<String> allProjects) {
        this.allProjects = Collections.unmodifiableSet(allProjects);
    }
}
