package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

/**
 * Contract for a chat server.
 */
public interface ChatServer {

    /**
     * Starts the server and begins accepting incoming clients.
     */
    void startup();

    /**
     * Shuts down the server and closes all active client connections.
     */
    void shutdown();

    /**
     * Sends a message to all currently connected clients.
     *
     * @param message message to broadcast
     */
    void broadcast(ChatMessage message);

    /**
     * Removes a client from the active client list.
     *
     * @param id client identifier
     */
    void remove(int id);
}
