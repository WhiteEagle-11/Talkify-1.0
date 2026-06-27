package com.chatapp.service;

import com.chatapp.model.*;

import java.util.Collection;
import java.util.Optional;

/**
 * Service interface for chat operations.
 * Demonstrates: Interface Segregation, Abstraction, Dependency Inversion.
 */
public interface ChatService {

    /**
     * Registers a new user into a chat room.
     * @param username display name chosen by the user
     * @param roomId   target room
     * @return newly created User
     */
    User joinRoom(String username, String roomId);

    /**
     * Removes user from a room.
     */
    void leaveRoom(String userId, String roomId);

    /**
     * Sends a chat message to a room and persists it.
     */
    ChatMessage sendMessage(String senderId, String roomId, String content);

    /**
     * Fetches the room, creating it lazily if absent.
     */
    ChatRoom getOrCreateRoom(String roomId);

    /**
     * Returns all users currently in the given room.
     */
    Collection<User> getActiveUsers(String roomId);

    /**
     * Looks up a user by their session ID.
     */
    Optional<User> findUser(String userId, String roomId);

    /**
     * Returns the default general room ID.
     */
    String getDefaultRoomId();
}
