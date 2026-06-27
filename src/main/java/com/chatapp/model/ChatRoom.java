package com.chatapp.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a Chat Room.
 * Demonstrates: Encapsulation, Collections, Java Streams, Thread-safety with ConcurrentHashMap.
 */
public class ChatRoom {

    private final String roomId;
    private final String roomName;
    private final LocalDateTime createdAt;

    // Thread-safe map of connected users
    private final Map<String, User> activeUsers;

    // Message history (bounded to last N messages)
    private final Deque<Message> messageHistory;
    private static final int MAX_HISTORY = 100;

    public ChatRoom(String roomId, String roomName) {
        this.roomId         = Objects.requireNonNull(roomId);
        this.roomName       = Objects.requireNonNull(roomName);
        this.createdAt      = LocalDateTime.now();
        this.activeUsers    = new ConcurrentHashMap<>();
        this.messageHistory = new ArrayDeque<>(MAX_HISTORY);
    }

    // ─── User Management ─────────────────────────────────────────────────────────

    public void addUser(User user) {
        activeUsers.put(user.getUserId(), user);
    }

    public void removeUser(String userId) {
        activeUsers.remove(userId);
    }

    public Optional<User> findUser(String userId) {
        return Optional.ofNullable(activeUsers.get(userId));
    }

    public Collection<User> getActiveUsers() {
        return Collections.unmodifiableCollection(activeUsers.values());
    }

    public int getUserCount() {
        return activeUsers.size();
    }

    public boolean hasUser(String userId) {
        return activeUsers.containsKey(userId);
    }

    // ─── Message Management ───────────────────────────────────────────────────────

    public synchronized void addMessage(Message message) {
        if (messageHistory.size() >= MAX_HISTORY) {
            messageHistory.pollFirst(); // remove oldest
        }
        messageHistory.addLast(message);
    }

    public List<Message> getRecentMessages(int count) {
        return messageHistory.stream()
            .skip(Math.max(0, messageHistory.size() - count))
            .collect(Collectors.toList());
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messageHistory);
    }

    // ─── JSON serialization ───────────────────────────────────────────────────────

    /**
     * Returns JSON representation of all active users for broadcast
     */
    public String getUserListJson() {
        String usersJson = activeUsers.values().stream()
            .map(u -> String.format(
                "{\"userId\":\"%s\",\"username\":\"%s\",\"avatarColor\":\"%s\",\"status\":\"%s\",\"initials\":\"%s\"}",
                u.getUserId(), u.getUsername(), u.getAvatarColor(), u.getStatus(), u.getInitials()
            ))
            .collect(Collectors.joining(","));
        return String.format("{\"type\":\"USER_LIST\",\"users\":[%s],\"count\":%d}", usersJson, activeUsers.size());
    }

    // ─── Getters ──────────────────────────────────────────────────────────────────

    public String getRoomId()       { return roomId; }
    public String getRoomName()     { return roomName; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return String.format("ChatRoom{id='%s', name='%s', users=%d}", roomId, roomName, getUserCount());
    }
}
