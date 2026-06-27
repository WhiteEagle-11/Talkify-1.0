package com.chatapp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active WebSocket sessions.
 *
 * OOP Concepts:
 *   - Singleton (one manager per application)
 *   - Encapsulation (private session storage)
 *   - Iterator pattern (for broadcasting)
 */
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    // Singleton
    private static final SessionManager INSTANCE = new SessionManager();
    private SessionManager() {}
    public static SessionManager getInstance() { return INSTANCE; }

    // sessionId → Session
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    // sessionId → userId (links WebSocket session to domain user)
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    // sessionId → roomId
    private final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();

    // ─── Session Lifecycle ────────────────────────────────────────────────────────

    public void register(Session session) {
        sessions.put(session.getId(), session);
        log.debug("Session registered: {}", session.getId());
    }

    public void unregister(Session session) {
        sessions.remove(session.getId());
        sessionUserMap.remove(session.getId());
        sessionRoomMap.remove(session.getId());
        log.debug("Session unregistered: {}", session.getId());
    }

    public void bindUser(String sessionId, String userId, String roomId) {
        sessionUserMap.put(sessionId, userId);
        sessionRoomMap.put(sessionId, roomId);
    }

    // ─── Lookups ──────────────────────────────────────────────────────────────────

    public Optional<String> getUserId(String sessionId) {
        return Optional.ofNullable(sessionUserMap.get(sessionId));
    }

    public Optional<String> getRoomId(String sessionId) {
        return Optional.ofNullable(sessionRoomMap.get(sessionId));
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    // ─── Broadcasting ─────────────────────────────────────────────────────────────

    /**
     * Broadcasts a message to all sessions in a given room.
     */
    public void broadcastToRoom(String roomId, String jsonPayload) {
        int sent = 0, failed = 0;

        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            Session session  = entry.getValue();

            // Only send to sessions in the same room
            String targetRoom = sessionRoomMap.get(sessionId);
            if (!roomId.equals(targetRoom)) continue;

            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(jsonPayload);
                    sent++;
                } catch (IOException e) {
                    log.error("Broadcast to session {} failed: {}", sessionId, e.getMessage());
                    failed++;
                }
            }
        }

        log.debug("Broadcast to room '{}': {} sent, {} failed", roomId, sent, failed);
    }

    /**
     * Sends a message to a single specific session.
     */
    public void sendTo(String sessionId, String jsonPayload) {
        Session session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(jsonPayload);
            } catch (IOException e) {
                log.error("Direct send to session {} failed: {}", sessionId, e.getMessage());
            }
        }
    }

    /**
     * Returns all session IDs currently in a room.
     */
    public Set<String> getSessionsInRoom(String roomId) {
        Set<String> result = new HashSet<>();
        sessionRoomMap.forEach((sessionId, room) -> {
            if (roomId.equals(room)) result.add(sessionId);
        });
        return result;
    }
}
