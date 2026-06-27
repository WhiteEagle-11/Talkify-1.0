package com.chatapp.model;

/**
 * Concrete subclass of Message for regular chat messages.
 * Demonstrates: Inheritance, Method Overriding.
 */
public class ChatMessage extends Message {

    private final String content;
    private String avatarColor;
    private String reaction;

    public ChatMessage(String senderId, String senderName, String content) {
        super(senderId, senderName, MessageType.CHAT);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Chat message content cannot be empty");
        }
        this.content = content.trim();
    }

    public ChatMessage(String senderId, String senderName, String content, String avatarColor) {
        this(senderId, senderName, content);
        this.avatarColor = avatarColor;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public String toJson() {
        return String.format("{%s,\"content\":\"%s\",\"avatarColor\":\"%s\",\"reaction\":\"%s\"}",
            baseJson(),
            escapeJson(content),
            avatarColor != null ? avatarColor : "#00D4FF",
            reaction != null ? reaction : ""
        );
    }

    public void setReaction(String emoji) {
        this.reaction = emoji;
    }

    public String getReaction() {
        return reaction;
    }

    public String getAvatarColor() {
        return avatarColor;
    }

    /**
     * Escapes special JSON characters to prevent injection
     */
    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
