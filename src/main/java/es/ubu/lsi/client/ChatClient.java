package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * Contract for a chat client.
 */
public interface ChatClient {

    /**
     * Starts the client lifecycle and blocks until disconnection.
     *
     * @return {@code true} if the client finishes disconnected
     */
    boolean start();

    /**
     * Sends a message to the server.
     *
     * @param msg message to send
     */
    void sendMessage(ChatMessage msg);

    /**
     * Disconnects the client from the server.
     */
    void disconnect();
}
