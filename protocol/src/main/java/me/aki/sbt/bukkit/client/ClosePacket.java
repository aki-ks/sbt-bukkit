package me.aki.sbt.bukkit.client;

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

    public void read(Decoder decoder) {
        this.message = decoder.readString();
    }

    public void write(Encoder encoder) {
        encoder.writeString(this.message);
    }

    public void handlePacket(PacketHandler handler) {
        ((ServerPacketHandler)handler).onClose(this);
    }
}
