package es.ubu.lsi.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.ubu.lsi.common.ChatMessage;

/**
 * TCP server for the distributed chat.
 */
public class ChatServerImpl implements ChatServer {

    private static final int DEFAULT_PORT = 1500;
    private static final String SPONSOR_NAME = "Adrian";
    private static final String SPONSOR_PREFIX = SPONSOR_NAME + " patrocina el mensaje: ";

    private final int port;
    private boolean alive;
    private int clientId;
    private ServerSocket serverSocket;

    private final List<ServerThreadForClient> clients = new ArrayList<>();
    private final Map<String, Set<String>> blockedUsersByUser = new HashMap<>();

    /**
     * Creates a chat server instance.
     *
     * @param port TCP listening port
     */
    public ChatServerImpl(int port) {
        this.port = port;
        this.alive = false;
        this.clientId = 0;
    }

    @Override
    public void startup() {
        try {
            alive = true;
            serverSocket = new ServerSocket(port);
            System.out.println("Servidor inicializado en el puerto " + port);

            while (alive) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

                    String username = input.readObject().toString();
                    blockedUsersByUser.computeIfAbsent(username, k -> new HashSet<>());

                    ServerThreadForClient clientThread =
                            new ServerThreadForClient(clientId++, username, socket, output, input);
                    synchronized (clients) {
                        clients.add(clientThread);
                    }
                    clientThread.start();
                } catch (SocketException e) {
                    if (alive) {
                        System.out.println("Socket del servidor cerrado de forma inesperada: " + e.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    if (alive) {
                        System.out.println("Error al aceptar la conexion: " + e.getMessage());
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("Error al leer el nombre de usuario: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("No se ha podido arrancar el servidor: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    @Override
    public void shutdown() {
        if (!alive && (serverSocket == null || serverSocket.isClosed())) {
            return;
        }

        alive = false;
        closeServerSocket();

        ChatMessage shutdownMessage =
                new ChatMessage(0, ChatMessage.MessageType.SHUTDOWN, "El servidor se ha apagado");

        synchronized (clients) {
            for (ServerThreadForClient client : clients) {
                client.send(shutdownMessage);
                client.closeClientConnections();
            }
            clients.clear();
        }
        System.out.println("Servidor apagado correctamente");
    }

    @Override
    public void broadcast(ChatMessage message) {
        synchronized (clients) {
            Iterator<ServerThreadForClient> iterator = clients.iterator();
            while (iterator.hasNext()) {
                ServerThreadForClient client = iterator.next();
                if (!client.send(message)) {
                    client.closeClientConnections();
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void remove(int id) {
        synchronized (clients) {
            Iterator<ServerThreadForClient> iterator = clients.iterator();
            while (iterator.hasNext()) {
                ServerThreadForClient client = iterator.next();
                if (client.id == id) {
                    client.closeClientConnections();
                    iterator.remove();
                    System.out.println("El usuario " + client.username + " se ha desconectado");
                    break;
                }
            }
        }
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Error al cerrar el socket del servidor: " + e.getMessage());
            }
        }
    }

    private String sponsorText(String message) {
        return SPONSOR_PREFIX + message;
    }

    private void logSponsored(String message) {
        System.out.println(sponsorText(message));
    }

    private boolean isBlockedForRecipient(String recipient, String sender) {
        Set<String> blocked = blockedUsersByUser.get(recipient);
        return blocked != null && blocked.contains(sender);
    }

    private void broadcastFromSender(int senderId, String senderUsername, String originalMessage) {
        String payload = senderUsername + ": " + sponsorText(originalMessage);
        ChatMessage message = new ChatMessage(senderId, ChatMessage.MessageType.MESSAGE, payload);

        synchronized (clients) {
            Iterator<ServerThreadForClient> iterator = clients.iterator();
            while (iterator.hasNext()) {
                ServerThreadForClient client = iterator.next();
                if (isBlockedForRecipient(client.username, senderUsername)) {
                    continue;
                }
                if (!client.send(message)) {
                    client.closeClientConnections();
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Entry point to launch the server.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        ChatServerImpl server = new ChatServerImpl(DEFAULT_PORT);
        server.startup();
    }

    /**
     * Thread that handles one connected client.
     */
    public class ServerThreadForClient extends Thread {
        private final int id;
        private final String username;
        private final Socket socket;
        private final ObjectOutputStream output;
        private final ObjectInputStream input;

        /**
         * Creates a dedicated server thread for one client connection.
         *
         * @param id server-side identifier of this client
         * @param username client nickname
         * @param socket client socket
         * @param output output stream to client
         * @param input input stream from client
         */
        public ServerThreadForClient(
                int id,
                String username,
                Socket socket,
                ObjectOutputStream output,
                ObjectInputStream input) {
            this.id = id;
            this.username = username;
            this.socket = socket;
            this.output = output;
            this.input = input;
        }

        @Override
        public void run() {
            try {
                System.out.println(username + " se ha conectado con id: " + id);

                while (alive && !socket.isClosed()) {
                    ChatMessage msg = (ChatMessage) input.readObject();

                    if (msg.getType() == ChatMessage.MessageType.LOGOUT) {
                        remove(id);
                        break;
                    }

                    if (msg.getType() == ChatMessage.MessageType.SHUTDOWN) {
                        shutdown();
                        break;
                    }

                    handleTextMessage(msg.getMessage());
                }
            } catch (SocketException e) {
                System.out.println("Conexion cerrada con " + username);
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error de conexion con " + username + ": " + e.getMessage());
            } finally {
                remove(id);
            }
        }

        private void handleTextMessage(String message) {
            if (message == null || message.isBlank()) {
                return;
            }

            if (message.startsWith("ban ")) {
                handleBan(message.substring(4).trim());
                return;
            }

            if (message.startsWith("unban ")) {
                handleUnban(message.substring(6).trim());
                return;
            }

            logSponsored(username + ": " + message);
            broadcastFromSender(id, username, message);
        }

        private void handleBan(String userToBan) {
            if (userToBan.isBlank() || userToBan.equals(username)) {
                return;
            }

            blockedUsersByUser.computeIfAbsent(username, k -> new HashSet<>()).add(userToBan);
            logSponsored(username + " ha baneado a " + userToBan);
        }

        private void handleUnban(String userToUnban) {
            if (userToUnban.isBlank() || userToUnban.equals(username)) {
                return;
            }

            Set<String> blocked = blockedUsersByUser.computeIfAbsent(username, k -> new HashSet<>());
            blocked.remove(userToUnban);
            logSponsored(username + " ha desbaneado a " + userToUnban);
        }

        /**
         * Sends one message to this client.
         *
         * @param message message to send
         * @return {@code true} if sent successfully
         */
        public boolean send(ChatMessage message) {
            try {
                output.writeObject(message);
                output.flush();
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * Closes socket and stream resources associated to this client.
         */
        public void closeClientConnections() {
            try {
                input.close();
            } catch (IOException ignored) {
            }
            try {
                output.close();
            } catch (IOException ignored) {
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
