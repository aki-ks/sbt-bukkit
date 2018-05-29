package me.aki.sbt.bukkit;

import java.io.IOException;
import java.net.Socket;

public class SocketManager extends Thread {
    private final Socket socket;
    private final Protocol decodeProtocol;
    private final Protocol encodeProtocol;
    private final PacketHandler handler;

    private final Encoder encoder;
    private final Decoder decoder;

    public SocketManager(Socket socket, Protocol decodeProtocol, Protocol encodeProtocol, PacketHandler handler) throws IOException {
        this.socket = socket;
        this.decodeProtocol = decodeProtocol;
        this.encodeProtocol = encodeProtocol;
        this.handler = handler;

        this.encoder = new Encoder(socket.getOutputStream());
        this.decoder = new Decoder(socket.getInputStream());
    }

    public void run() {
        try {
            while (!isInterrupted()) {
                Packet packet = decodeProtocol.readPacket(decoder);
                packet.handlePacket(handler);
            }
        } catch (Throwable t) {
            if (socket.isClosed()) {
                if(!isInterrupted()) {
                    handler.onConnectionLost();
                }
            } else {
                try {
                    close(t.getClass().getName() + ": " + t.toString() + t.getMessage());
                } catch (Throwable t1) {}

                throw new RuntimeException("Exception in connection " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort(), t);
            }
        }
    }

    public void writePacket(Packet packet) {
        encodeProtocol.writePacket(packet, encoder);
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public void closeSocket() {
        this.interrupt();
        try {
            if(!socket.isClosed())
                this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tell the server and then close the socket
     */
    public void close(String message) {
        if(!isClosed()) {
            try {
                handler.close(message);
            } finally {
                closeSocket();
            }
        }
    }
}
