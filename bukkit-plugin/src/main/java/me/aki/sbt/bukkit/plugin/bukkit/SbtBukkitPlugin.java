package me.aki.sbt.bukkit.plugin.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Optional;

public class SbtBukkitPlugin extends JavaPlugin {
    private Optional<BridgeClient> client;

    @Override
    public void onEnable() {
        getCommand("sbt").setExecutor(new SbtCommandExecutor(this));

        int port = Optional.ofNullable(System.getProperty("bridge.port")).map(Integer::parseInt).orElse(40124);
        String host = Optional.ofNullable(System.getProperty("bridge.host")).orElse("127.0.0.1");

        try {
            getLogger().info("Connecting to bridge server at " + host + ":" + port);
            this.client = Optional.of(new BridgeClient(this, host, port));
        } catch (IOException e) {
            getLogger().warning("An exception occurred while connection to bridge server: " + e.getMessage());
        }
    }

    public Optional<BridgeClient> getClient() {
        return client;
    }

    public void setClient(Optional<BridgeClient> client) {
        this.client = client;
    }

    @Override
    public void onDisable() {
        client.ifPresent(client -> {
            getLogger().info("Closing connection to bridge server");

            try {
                client.getManager().close("Plugin disabled");
            } catch(Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
