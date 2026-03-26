package es.ubu.lsi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import es.ubu.lsi.common.ChatMessage;

/**
 * Text-based TCP chat client implementation.
 */
public class ChatClientImpl implements ChatClient {

    private static final int DEFAULT_PORT = 1500;
    private static final String DEFAULT_SERVER = "localhost";

    private final String server;
    private final String username;
    private final int port;
    private boolean carryOn = true;
    private final int id;
    private String line;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Socket socket;
    private Scanner scanner;

    /**
     * Creates a client bound to one server endpoint and one nickname.
     *
     * @param server server hostname or IP
     * @param port server port
     * @param username client nickname
     */
    public ChatClientImpl(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
        this.id = username.hashCode();
        this.carryOn = true;
    }

    @Override
    public boolean start() {
        try {
            carryOn = true;
            socket = new Socket(server, port);

            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            output.writeObject(username);

            new Thread(new ChatClientListener(input)).start();
            scanner = new Scanner(System.in);
            while (carryOn) {
                line = scanner.nextLine();

                if (line.equals("logout")) {
                    sendMessage(new ChatMessage(id, ChatMessage.MessageType.LOGOUT, line));
                    disconnect();
                } else {
                    sendMessage(new ChatMessage(id, ChatMessage.MessageType.MESSAGE, line));
                }
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeClient(scanner);
        }
        return !carryOn;
    }

    @Override
    public void sendMessage(ChatMessage msg) {
        try {
            if (output != null && carryOn) {
                output.writeObject(msg);
                output.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeClient(Scanner scanner) {
        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (socket != null) {
                socket.close();
            }

            if (scanner != null) {
                scanner.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        carryOn = false;
        System.out.println("Pulsa cualquier tecla para continuar...");
    }

    /**
     * Launches the client process.
     *
     * @param args command line arguments: [server] nickname
     */
    public static void main(String[] args) {
        if (args.length == 1 || args.length == 2) {
            String server = args.length == 2 ? args[0] : DEFAULT_SERVER;
            String username = args.length == 2 ? args[1] : args[0];
            ChatClientImpl client = new ChatClientImpl(server, DEFAULT_PORT, username);
            client.start();
        } else {
            System.out.println("Uso: java es.ubu.lsi.client.ChatClientImpl [servidor] <nickname>");
        }
    }

    /**
     * Thread that listens to messages received from server.
     */
    public class ChatClientListener implements Runnable {
        private final ObjectInputStream input;

        /**
         * Creates the listener bound to an input stream.
         *
         * @param input input stream from server
         */
        public ChatClientListener(ObjectInputStream input) {
            this.input = input;
        }

        @Override
        public void run() {
            try {
                while (carryOn) {
                    ChatMessage message = (ChatMessage) input.readObject();
                    System.out.println(message.getMessage());

                    if (message.getType() != ChatMessage.MessageType.MESSAGE) {
                        System.out.println("Usuario desconectado: " + username);
                        disconnect();
                        break;
                    }
                }
            } catch (Exception ex) {
                System.out.println("Conexion con " + username + " cerrada.");
                disconnect();
            }
        }
    }
}
