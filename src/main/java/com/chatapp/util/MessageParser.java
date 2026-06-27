package com.chatapp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Utility class for parsing incoming WebSocket JSON messages.
 * Demonstrates: Utility class pattern, Optional usage, Jackson JSON parsing.
 */
public final class MessageParser {

    private static final Logger log = LoggerFactory.getLogger(MessageParser.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    // Prevent instantiation of utility class
    private MessageParser() {
        throw new UnsupportedOperationException("MessageParser is a utility class");
    }

    /**
     * Parsed representation of an incoming client message.
     * Immutable value object.
     */
    public static final class ParsedMessage {
        private final String type;
        private final String content;
        private final String senderId;
        private final String roomId;
        private final String username;

        public ParsedMessage(String type, String content, String senderId, String roomId, String username) {
            this.type = type;
            this.content = content;
            this.senderId = senderId;
            this.roomId = roomId;
            this.username = username;
        }

        public String getType()     { return type; }
        public String getContent()  { return content; }
        public String getSenderId() { return senderId; }
        public String getRoomId()   { return roomId != null ? roomId : "general"; }
        public String getUsername() { return username; }

        public boolean isType(String expected) {
            return expected != null && expected.equalsIgnoreCase(type);
        }

        @Override
        public String toString() {
            return String.format("ParsedMessage{type='%s', sender='%s', room='%s'}", type, senderId, roomId);
        }
    }

    /**
     * Parses raw JSON text from a WebSocket message.
     * @param raw JSON string
     * @return Optional containing ParsedMessage, or empty if parse fails
     */
    public static Optional<ParsedMessage> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode node = mapper.readTree(raw);
            ParsedMessage msg = new ParsedMessage(
                getField(node, "type"),
                getField(node, "content"),
                getField(node, "senderId"),
                getField(node, "roomId"),
                getField(node, "username")
            );
            return Optional.of(msg);
        } catch (Exception e) {
            log.warn("Failed to parse message: {} — {}", raw, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Safely extracts a String field from a JSON node.
     */
    private static String getField(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    /**
     * Validates that a username is acceptable
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) return false;
        String trimmed = username.trim();
        return trimmed.length() >= 2 && trimmed.length() <= 30
            && trimmed.matches("[a-zA-Z0-9_\\- ]+");
    }
}
