package com.chatapp.server;

import com.chatapp.model.*;
import com.chatapp.service.ChatService;
import com.chatapp.service.ChatServiceImpl;
import com.chatapp.util.MessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Optional;

/**
 * WebSocket Server Endpoint for the chat application.
 *
 * OOP Concepts:
 *   - Composition (uses ChatService, SessionManager, MessageParser)
 *   - Single Responsibility (only handles WebSocket lifecycle)
 *   - Dependency on interfaces (ChatService), not implementations
 *
 * The @ServerEndpoint annotation registers this class as a WebSocket handler at /ws/chat.
 */
@ServerEndpoint("/ws/chat")
public class ChatWebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketServer.class);

    // Injected via composition (not inheritance)
    private final ChatService chatService = ChatServiceImpl.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // ─── WebSocket Lifecycle ─────────────────────────────────────────────────────

    @OnOpen
    public void onOpen(Session session) {
        sessionManager.register(session);
        log.info("WebSocket opened: sessionId={}", session.getId());

        // Send a welcome prompt asking for username
        String welcome = "{\"type\":\"WELCOME\",\"message\":\"Please enter your username\"}";
        sessionManager.sendTo(session.getId(), welcome);
    }

    @OnMessage
    public void onMessage(String rawMessage, Session session) {
        log.debug("Received from {}: {}", session.getId(), rawMessage);

        Optional<MessageParser.ParsedMessage> parsed = MessageParser.parse(rawMessage);
        if (parsed.isEmpty()) {
            log.warn("Unparseable message from {}", session.getId());
            return;
        }

        MessageParser.ParsedMessage msg = parsed.get();

        switch (msg.getType()) {
            case "JOIN":
                handleJoin(session, msg);
                break;
            case "CHAT":
                handleChat(session, msg);
                break;
            case "TYPING":
                handleTyping(session, msg);
                break;
            case "PING":
                handlePing(session);
                break;
            default:
                log.warn("Unknown message type: {}", msg.getType());
                break;
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        log.info("WebSocket closed: sessionId={}, reason={}", session.getId(), reason.getReasonPhrase());

        // Find which room this session was in
        Optional<String> roomIdOpt = sessionManager.getRoomId(session.getId());
        Optional<String> userIdOpt = sessionManager.getUserId(session.getId());

        if (roomIdOpt.isPresent() && userIdOpt.isPresent()) {
            String roomId = roomIdOpt.get();
            String userId = userIdOpt.get();

            // Find the user's name before removing them
            Optional<User> user = chatService.findUser(userId, roomId);
            String username = user.map(User::getUsername).orElse("Someone");

            // Remove from service and session manager
            chatService.leaveRoom(userId, roomId);
            sessionManager.unregister(session);

            // Notify room
            SystemMessage leaveMsg = SystemMessage.userLeft(username);
            sessionManager.broadcastToRoom(roomId, leaveMsg.toJson());

            // Update user list
            ChatRoom room = chatService.getOrCreateRoom(roomId);
            sessionManager.broadcastToRoom(roomId, room.getUserListJson());
        } else {
            sessionManager.unregister(session);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket error for session {}: {}", session.getId(), error.getMessage());
    }

    // ─── Message Handlers ─────────────────────────────────────────────────────────

    /**
     * Handles user joining a room with a chosen username.
     */
    private void handleJoin(Session session, MessageParser.ParsedMessage msg) {
        String username = msg.getUsername();

        if (!MessageParser.isValidUsername(username)) {
            String error = "{\"type\":\"ERROR\",\"message\":\"Invalid username. Use 2-30 alphanumeric characters.\"}";
            sessionManager.sendTo(session.getId(), error);
            return;
        }

        String roomId = chatService.getDefaultRoomId();
        User user = chatService.joinRoom(username, roomId);

        // Bind this session to the user and room
        sessionManager.bindUser(session.getId(), user.getUserId(), roomId);

        // Send confirmation back to the joining user
        String joined = String.format(
            "{\"type\":\"JOINED\",\"userId\":\"%s\",\"username\":\"%s\",\"avatarColor\":\"%s\",\"roomId\":\"%s\"}",
            user.getUserId(), user.getUsername(), user.getAvatarColor(), roomId
        );
        sessionManager.sendTo(session.getId(), joined);

        // Broadcast system message to all in the room
        SystemMessage joinMsg = SystemMessage.userJoined(username);
        sessionManager.broadcastToRoom(roomId, joinMsg.toJson());

        // Broadcast updated user list
        ChatRoom room = chatService.getOrCreateRoom(roomId);
        sessionManager.broadcastToRoom(roomId, room.getUserListJson());

        log.info("User '{}' joined room '{}'", username, roomId);
    }

    /**
     * Handles an incoming chat message.
     */
    private void handleChat(Session session, MessageParser.ParsedMessage msg) {
        Optional<String> userIdOpt = sessionManager.getUserId(session.getId());
        Optional<String> roomIdOpt = sessionManager.getRoomId(session.getId());

        if (userIdOpt.isEmpty() || roomIdOpt.isEmpty()) {
            sessionManager.sendTo(session.getId(), "{\"type\":\"ERROR\",\"message\":\"Not joined to a room\"}");
            return;
        }

        String content = msg.getContent();
        if (content == null || content.isBlank() || content.length() > 500) {
            return; // silently ignore empty/oversized messages
        }

        ChatMessage chatMsg = chatService.sendMessage(userIdOpt.get(), roomIdOpt.get(), content);
        sessionManager.broadcastToRoom(roomIdOpt.get(), chatMsg.toJson());
    }

    /**
     * Handles typing indicator — broadcasts without persisting.
     */
    private void handleTyping(Session session, MessageParser.ParsedMessage msg) {
        Optional<String> roomIdOpt = sessionManager.getRoomId(session.getId());
        Optional<String> userIdOpt = sessionManager.getUserId(session.getId());

        if (roomIdOpt.isEmpty() || userIdOpt.isEmpty()) return;

        chatService.findUser(userIdOpt.get(), roomIdOpt.get()).ifPresent(user -> {
            String payload = String.format(
                "{\"type\":\"TYPING\",\"senderId\":\"%s\",\"username\":\"%s\"}",
                user.getUserId(), user.getUsername()
            );

            // Send typing indicator to everyone else in the room
            for (String sid : sessionManager.getSessionsInRoom(roomIdOpt.get())) {
                if (!sid.equals(session.getId())) {
                    sessionManager.sendTo(sid, payload);
                }
            }
        });
    }

    /**
     * Handles a heartbeat ping to keep the connection alive.
     */
    private void handlePing(Session session) {
        sessionManager.sendTo(session.getId(), "{\"type\":\"PONG\"}");
    }
}
