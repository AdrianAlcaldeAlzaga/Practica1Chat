package es.ubu.lsi.common;

import java.io.Serializable;

public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 7467237896682458959L;

    public enum MessageType {
        MESSAGE,
        SHUTDOWN,
        LOGOUT;
    }

    private MessageType type;
    private String message;
    private int id;

    public ChatMessage(int id, MessageType type, String message) {
        this.setId(id);
        this.setType(type);
        this.setMessage(message);
    }

    public MessageType getType() {
        return type;
    }

    private void setType(MessageType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }
}
