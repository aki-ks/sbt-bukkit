package me.aki.sbt.bukkit.server;

import me.aki.sbt.bukkit.*;

public class SbtStatusPacket implements Packet  {
    private String[] allProjects;

    public SbtStatusPacket() {}

    public SbtStatusPacket(String[] allProjects) {
        this.allProjects = allProjects;
    }

    public String[] getAllProjects() {
        return allProjects;
    }

    public void setAllProjects(String[] allProjects) {
        this.allProjects = allProjects;
    }

    public void read(Decoder decoder) {
        this.allProjects = decoder.readArray(Decoder::readString, String.class);
    }

    public void write(Encoder encoder) {
        encoder.writeArray(Encoder::writeString, this.allProjects);
    }

    public void handlePacket(PacketHandler handler) {
        ((ClientPacketHandler)handler).onSbtStatusResponse(this);
    }
}
