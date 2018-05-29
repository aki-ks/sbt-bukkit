package me.aki.sbt.bukkit.server;

import me.aki.sbt.bukkit.*;

public class ClosePacket implements Packet {
    private String message;

    public ClosePacket() {}

    public ClosePacket(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void read(Decoder decoder) {}

    public void write(Encoder encoder) {}

    public void handlePacket(PacketHandler handler) {
        ((ClientPacketHandler)handler).onClose(this);
    }
}
