package com.chatapp.service;

import com.chatapp.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton implementation of ChatService.
 *
 * OOP Concepts demonstrated:
 *   - Interface Implementation (ChatService)
 *   - Singleton Pattern (thread-safe double-checked locking)
 *   - Composition (holds ChatRoom objects)
 *   - Encapsulation (private state, controlled access)
 */
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final String DEFAULT_ROOM_ID = "general";

    // ─── Singleton ───────────────────────────────────────────────────────────────
    private static volatile ChatServiceImpl instance;

    private ChatServiceImpl() {
        // Eagerly create the default room
        rooms.put(DEFAULT_ROOM_ID, new ChatRoom(DEFAULT_ROOM_ID, "General"));
        log.info("ChatService initialized with default room '{}'", DEFAULT_ROOM_ID);
    }

    public static ChatServiceImpl getInstance() {
        if (instance == null) {
            synchronized (ChatServiceImpl.class) {
                if (instance == null) {
                    instance = new ChatServiceImpl();
                }
            }
        }
        return instance;
    }

    // ─── State ───────────────────────────────────────────────────────────────────

    // roomId → ChatRoom; thread-safe
    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();

    // userId → roomId (tracks which room a user session belongs to)
    private final Map<String, String> userRoomIndex = new ConcurrentHashMap<>();

    // ─── Interface Implementation ─────────────────────────────────────────────────

    @Override
    public User joinRoom(String username, String roomId) {
        ChatRoom room = getOrCreateRoom(roomId);

        User user = new User.Builder(username).build();
        room.addUser(user);
        userRoomIndex.put(user.getUserId(), roomId);

        log.info("User '{}' ({}) joined room '{}'", username, user.getUserId(), roomId);
        return user;
    }

    @Override
    public void leaveRoom(String userId, String roomId) {
        ChatRoom room = rooms.get(roomId);
        if (room != null) {
            room.removeUser(userId);
            userRoomIndex.remove(userId);
            log.info("User {} left room '{}'", userId, roomId);
        }
    }

    @Override
    public ChatMessage sendMessage(String senderId, String roomId, String content) {
        ChatRoom room = getOrCreateRoom(roomId);
        Optional<User> userOpt = room.findUser(senderId);

        String senderName = userOpt.map(User::getUsername).orElse("Anonymous");
        String avatarColor = userOpt.map(User::getAvatarColor).orElse("#00D4FF");

        ChatMessage message = new ChatMessage(senderId, senderName, content, avatarColor);
        room.addMessage(message);

        // Increment user's message counter
        userOpt.ifPresent(u -> {
            u.incrementMessageCount();
            log.debug("Message #{} from '{}'", u.getMessageCount(), senderName);
        });

        return message;
    }

    @Override
    public ChatRoom getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, id -> {
            log.info("Creating new room: '{}'", id);
            return new ChatRoom(id, id.substring(0, 1).toUpperCase() + id.substring(1));
        });
    }

    @Override
    public Collection<User> getActiveUsers(String roomId) {
        ChatRoom room = rooms.get(roomId);
        return room != null ? room.getActiveUsers() : Collections.emptyList();
    }

    @Override
    public Optional<User> findUser(String userId, String roomId) {
        ChatRoom room = rooms.get(roomId);
        return room != null ? room.findUser(userId) : Optional.empty();
    }

    @Override
    public String getDefaultRoomId() {
        return DEFAULT_ROOM_ID;
    }

    // ─── Utility ──────────────────────────────────────────────────────────────────

    public Optional<ChatRoom> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public int getTotalActiveUsers() {
        return rooms.values().stream()
            .mapToInt(ChatRoom::getUserCount)
            .sum();
    }
}
