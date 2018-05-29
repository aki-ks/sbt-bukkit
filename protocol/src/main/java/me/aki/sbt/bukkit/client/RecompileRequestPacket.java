package me.aki.sbt.bukkit.client;

import me.aki.sbt.bukkit.*;

public class RecompileRequestPacket implements Packet {
    private String projectId;
    private String configuration;

    public RecompileRequestPacket() {}

    public RecompileRequestPacket(String projectId, String configuration) {
        this.projectId = projectId;
        this.configuration = configuration;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public void read(Decoder decoder) {
        this.projectId = decoder.readString();
        this.configuration = decoder.readString();
    }

    public void write(Encoder encoder) {
        encoder.writeString(this.projectId);
        encoder.writeString(this.configuration);
    }

    public void handlePacket(PacketHandler handler) {
        ((ServerPacketHandler)handler).onRecompileRequest(this);
    }
}
