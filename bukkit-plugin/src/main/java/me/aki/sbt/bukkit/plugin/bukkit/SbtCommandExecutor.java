package me.aki.sbt.bukkit.plugin.bukkit;

import me.aki.sbt.bukkit.client.RecompileRequestPacket;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SbtCommandExecutor implements CommandExecutor {
    private final SbtBukkitPlugin plugin;

    public SbtCommandExecutor(SbtBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) {
            printUsage(sender);
        } else {
            switch (args[0].toLowerCase()) {
                case "reconnect": {
                    Optional<BridgeClient> clientOpt = plugin.getClient();
                    if(clientOpt.isPresent()) {
                        Socket socket = clientOpt.get().getManager().getSocket();
                        connect(sender, socket.getInetAddress().getHostAddress(), socket.getPort());
                    } else {
                        sender.sendMessage("This server has never been connected to sbt");
                    }
                    break;
                }

                case "connect": {
                    if (args.length == 1) {
                        sender.sendMessage("Usage: /sbt connect [host] <port>");
                    } else {
                        boolean hasHost = args.length > 2;
                        String host = hasHost ? args[1] : "127.0.0.1";
                        String portString = hasHost ? args[2] : args[1];

                        int port;
                        try {
                            port = Integer.parseInt(portString);
                        } catch (NumberFormatException e) {
                            sender.sendMessage("The specified port (" + portString + ") is not a number!");
                            break;
                        }

                        connect(sender, host, port);
                    }
                    break;
                }

                case "disconnect": {
                    Optional<BridgeClient> currentClient = plugin.getClient();
                    if (!currentClient.isPresent() || currentClient.get().getManager().isClosed()) {
                        sender.sendMessage("Current connection is already closed");
                    } else {
                        closeCurrentConnection(sender);
                    }
                    break;
                }

                case "recompile":
                    Optional<BridgeClient> clientOpt = plugin.getClient();
                    BridgeClient client;
                    if(clientOpt.isPresent() && !(client = clientOpt.get()).getManager().isClosed()) {
                        String projectName = args.length == 1 ?
                                System.getProperty("bridge.project") :
                                args[1];

                        if(projectName == null) {
                            sender.sendMessage("Usage: /sbt recompile [project]");
                            break;
                        }

                        Set<String> allProjects = client.getAllProjects();
                        if(allProjects.contains(projectName)) {
                            client.getManager().writePacket(new RecompileRequestPacket(projectName, "bukkit"));
                        } else {
                            String joinedNames = allProjects.stream().collect(Collectors.joining(", "));
                            sender.sendMessage("There's no such project " + projectName);
                            sender.sendMessage("There are following projects: " + joinedNames);
                        }
                    } else {
                        sender.sendMessage("You're not connected to a sbt instance");
                    }
                    break;
            }
        }
        return true;
    }

    private void connect(CommandSender sender, String host, int port) {
        closeCurrentConnection(sender);

        try {
            BridgeClient newClient = new BridgeClient(plugin, host, port);
            plugin.setClient(Optional.of(newClient));
            sender.sendMessage("Established connection to sbt");
        } catch (IOException e) {
            sender.sendMessage("Could not connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeCurrentConnection(CommandSender sender) {
        Optional<BridgeClient> currentClient = plugin.getClient();
        currentClient.ifPresent(client -> {
            try {
                client.getManager().close("Closed by user");
                sender.sendMessage("Closed current sbt connection");
            } catch (Throwable t) {
                sender.sendMessage("Could not close current sbt connection: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    private void printUsage(CommandSender sender) {
        sender.sendMessage("Usage:");
        sender.sendMessage("/sbt recompile [project] - recompile a project and reload the server");
        sender.sendMessage("/sbt connect [host] <port> - connect to a running bridge server");
        sender.sendMessage("/sbt disconnect - disconnect from sbt");
        sender.sendMessage("/sbt reconnect - reconnect to sbt");
    }
}
