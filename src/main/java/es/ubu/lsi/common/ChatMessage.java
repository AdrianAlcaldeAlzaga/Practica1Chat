package es.ubu.lsi.common;

import java.io.Serializable;

/**
 * Message exchanged by chat clients and server.
 */
public class ChatMessage implements Serializable {

    /** Serialization identifier. */
    private static final long serialVersionUID = 7467237896682458959L;

    /**
     * Supported message types.
     */
    public enum MessageType {
        /** Standard text message. */
        MESSAGE,
        /** Server shutdown message. */
        SHUTDOWN,
        /** Client logout message. */
        LOGOUT;
    }

    /** Type of this message. */
    private MessageType type;
    /** Body text associated to this message. */
    private String message;
    /** Sender identifier. */
    private int id;

    /**
     * Builds a new chat message.
     *
     * @param id sender identifier
     * @param type message type
     * @param message body text
     */
    public ChatMessage(int id, MessageType type, String message) {
        this.setId(id);
        this.setType(type);
        this.setMessage(message);
    }

    /**
     * Returns the message type.
     *
     * @return message type
     */
    public MessageType getType() {
        return type;
    }

    private void setType(MessageType type) {
        this.type = type;
    }

    /**
     * Returns the message body.
     *
     * @return message body
     */
    public String getMessage() {
        return message;
    }

    /**
     * Updates the message body.
     *
     * @param message new message body
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns sender identifier.
     *
     * @return sender identifier
     */
    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }
}
