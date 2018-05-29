package me.aki.sbt.bukkit;

import java.util.HashMap;
import java.util.Map;

public abstract class Protocol {
    public final static Protocol CLIENT_PROTOCOL = new Protocol() {
        {
            addPacket(0, me.aki.sbt.bukkit.client.ClosePacket.class);
            addPacket(1, me.aki.sbt.bukkit.client.GetSbtStatusPacket.class);
            addPacket(2, me.aki.sbt.bukkit.client.RecompileRequestPacket.class);
        }
    };
    public final static Protocol SERVER_PROTOCOL = new Protocol() {
        {
            addPacket(0, me.aki.sbt.bukkit.server.ClosePacket.class);
            addPacket(1, me.aki.sbt.bukkit.server.SbtStatusPacket.class);
            addPacket(2, me.aki.sbt.bukkit.server.RecompileResponsePacket.class);
        }
    };

    private Map<Integer, Class<? extends Packet>> idToPacket = new HashMap<>();
    private Map<Class<? extends Packet>, Integer> packetToId = new HashMap<>();

    public void addPacket(int id, Class<? extends Packet> packetType) {
        try {
            packetType.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Packet " + packetType.getSimpleName() + " has no empty constructor");
        }

        idToPacket.put(id, packetType);
        packetToId.put(packetType, id);
    }

    public void writePacket(Packet packet, Encoder encoder) {
        Integer id = packetToId.get(packet.getClass());
        if(id == null)throw new RuntimeException("Unregistered Packet " + packet.getClass());

        encoder.writeInt(id);
        packet.write(encoder);
    }

    public Packet readPacket(Decoder decoder) {
        int id = decoder.readInt();
        Class<? extends Packet> packetType = idToPacket.get(id);

        if(packetType == null)
            throw new IllegalStateException("Illegal packet id " + id);

        try {
            Packet packet = packetType.newInstance();
            packet.read(decoder);
            return packet;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Cannot initialize packet");
        }
    }
}
