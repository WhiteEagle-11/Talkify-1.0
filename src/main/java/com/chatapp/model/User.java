package com.chatapp.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a User in the chat application.
 * Demonstrates OOP concepts: Encapsulation, Builder Pattern, Immutability.
 */
public class User {

    // Encapsulated fields - private with controlled access
    private final String userId;
    private String username;
    private String avatarColor;
    private LocalDateTime joinedAt;
    private UserStatus status;
    private int messageCount;

    /**
     * Enum for user status - demonstrates use of Enum as a type-safe alternative to constants
     */
    public enum UserStatus {
        ONLINE, AWAY, OFFLINE, TYPING
    }

    // Private constructor - forces use of Builder
    private User(Builder builder) {
        this.userId = builder.userId;
        this.username = builder.username;
        this.avatarColor = builder.avatarColor;
        this.joinedAt = builder.joinedAt;
        this.status = builder.status;
        this.messageCount = 0;
    }

    // ─── Getters ────────────────────────────────────────────────────────────────

    public String getUserId()       { return userId; }
    public String getUsername()     { return username; }
    public String getAvatarColor()  { return avatarColor; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public UserStatus getStatus()   { return status; }
    public int getMessageCount()    { return messageCount; }

    // ─── Setters (controlled mutation) ───────────────────────────────────────────

    public void setStatus(UserStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        this.username = username.trim();
    }

    public void incrementMessageCount() {
        this.messageCount++;
    }

    /**
     * Returns initials for avatar display (e.g., "John Doe" → "JD")
     */
    public String getInitials() {
        if (username == null || username.isBlank()) return "?";
        String[] parts = username.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0))).toUpperCase();
    }

    // ─── Builder Pattern ─────────────────────────────────────────────────────────

    public static class Builder {
        private String userId;
        private String username;
        private String avatarColor;
        private LocalDateTime joinedAt;
        private UserStatus status;

        public Builder(String username) {
            this.userId = UUID.randomUUID().toString();
            this.username = username;
            this.avatarColor = generateColor(username);
            this.joinedAt = LocalDateTime.now();
            this.status = UserStatus.ONLINE;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder avatarColor(String color) {
            this.avatarColor = color;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public User build() {
            if (username == null || username.isBlank()) {
                throw new IllegalStateException("Username is required to build a User");
            }
            return new User(this);
        }

        /**
         * Deterministically generates a color from a username hash
         */
        private static String generateColor(String username) {
            String[] colors = {
                "#00D4FF", "#7C3AED", "#10B981", "#F59E0B",
                "#EF4444", "#EC4899", "#3B82F6", "#8B5CF6"
            };
            int index = Math.abs(username.hashCode()) % colors.length;
            return colors[index];
        }
    }

    // ─── Object overrides ────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return String.format("User{id='%s', username='%s', status=%s, messages=%d}",
                userId, username, status, messageCount);
    }
}
