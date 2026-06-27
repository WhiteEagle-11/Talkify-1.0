package com.chatapp.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for all message types.
 * Demonstrates OOP: Abstraction, Inheritance, Polymorphism.
 */
public abstract class Message {

    // Shared state for all message types
    protected final String messageId;
    protected final String senderId;
    protected final String senderName;
    protected final LocalDateTime timestamp;
    protected MessageType type;

    /**
     * Message types - drives polymorphic behavior on the frontend
     */
    public enum MessageType {
        CHAT,           // Normal chat message
        SYSTEM,         // Server-side notification (join/leave)
        TYPING,         // Typing indicator
        PRIVATE,        // Direct message
        REACTION        // Emoji reaction
    }

    protected Message(String senderId, String senderName, MessageType type) {
        this.messageId = UUID.randomUUID().toString();
        this.senderId  = Objects.requireNonNull(senderId, "senderId is required");
        this.senderName = Objects.requireNonNull(senderName, "senderName is required");
        this.timestamp = LocalDateTime.now();
        this.type      = type;
    }

    // ─── Abstract method – subclasses must implement ──────────────────────────

    /**
     * Returns the displayable content of this message.
     * Polymorphism: each subclass decides how to render its content.
     */
    public abstract String getContent();

    /**
     * Returns a JSON-serializable summary of this message.
     * Each subclass can enrich this with additional fields.
     */
    public abstract String toJson();

    // ─── Shared getters ──────────────────────────────────────────────────────────

    public String getMessageId()   { return messageId; }
    public String getSenderId()    { return senderId; }
    public String getSenderName()  { return senderName; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public MessageType getType()   { return type; }

    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // ─── Shared JSON helper ──────────────────────────────────────────────────────

    protected String baseJson() {
        return String.format(
            "\"messageId\":\"%s\",\"senderId\":\"%s\",\"senderName\":\"%s\"," +
            "\"timestamp\":\"%s\",\"type\":\"%s\"",
            messageId, senderId, senderName, getFormattedTime(), type
        );
    }
}
