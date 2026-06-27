package com.chatapp.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for domain model classes.
 * Demonstrates: JUnit 5, builder pattern testing, OOP contract verification.
 */
@DisplayName("ChatApp Domain Model Tests")
class ModelTest {

    // ─── User Tests ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("User builder should create valid user")
    void testUserBuilderCreatesValidUser() {
        User user = new User.Builder("Alice").build();

        assertNotNull(user.getUserId(), "UserId should be auto-generated");
        assertEquals("Alice", user.getUsername());
        assertEquals(User.UserStatus.ONLINE, user.getStatus());
        assertNotNull(user.getAvatarColor());
        assertEquals(0, user.getMessageCount());
    }

    @Test
    @DisplayName("User should produce correct initials")
    void testUserInitials() {
        User single = new User.Builder("Alice").build();
        assertEquals("A", single.getInitials());

        User full = new User.Builder("John Doe").build();
        assertEquals("JD", full.getInitials());

        User multi = new User.Builder("Mary Jane Watson").build();
        assertEquals("MW", multi.getInitials());
    }

    @Test
    @DisplayName("User should reject blank username")
    void testUserRejectsBlankUsername() {
        assertThrows(IllegalArgumentException.class,
            () -> new User.Builder("").build()
        );
    }

    @Test
    @DisplayName("User message count increments correctly")
    void testUserMessageCountIncrement() {
        User user = new User.Builder("Bob").build();
        assertEquals(0, user.getMessageCount());

        user.incrementMessageCount();
        user.incrementMessageCount();
        assertEquals(2, user.getMessageCount());
    }

    @Test
    @DisplayName("Users with same ID should be equal")
    void testUserEquality() {
        User u1 = new User.Builder("Alice").userId("same-id").build();
        User u2 = new User.Builder("Alice Renamed").userId("same-id").build();

        assertEquals(u1, u2, "Users with same ID should be equal regardless of name");
    }

    // ─── ChatMessage Tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("ChatMessage should store content correctly")
    void testChatMessageContent() {
        ChatMessage msg = new ChatMessage("user-1", "Alice", "Hello, World!");

        assertEquals("Hello, World!", msg.getContent());
        assertEquals("Alice", msg.getSenderName());
        assertEquals(Message.MessageType.CHAT, msg.getType());
        assertNotNull(msg.getMessageId());
        assertNotNull(msg.getTimestamp());
    }

    @Test
    @DisplayName("ChatMessage should reject empty content")
    void testChatMessageRejectsEmptyContent() {
        assertThrows(IllegalArgumentException.class,
            () -> new ChatMessage("user-1", "Alice", "   ")
        );
    }

    @Test
    @DisplayName("ChatMessage toJson should produce valid JSON")
    void testChatMessageToJson() {
        ChatMessage msg = new ChatMessage("u1", "Alice", "Hi there!", "#00D4FF");
        String json = msg.toJson();

        assertTrue(json.startsWith("{"), "Should start with {");
        assertTrue(json.endsWith("}"), "Should end with }");
        assertTrue(json.contains("\"type\":\"CHAT\""), "Should include message type");
        assertTrue(json.contains("Hi there!"), "Should include content");
    }

    @Test
    @DisplayName("ChatMessage should trim whitespace from content")
    void testChatMessageTrimsContent() {
        ChatMessage msg = new ChatMessage("u1", "Alice", "  hello  ");
        assertEquals("hello", msg.getContent());
    }

    // ─── SystemMessage Tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("SystemMessage userJoined factory should produce correct content")
    void testSystemMessageJoined() {
        SystemMessage msg = SystemMessage.userJoined("Charlie");

        assertEquals(Message.MessageType.SYSTEM, msg.getType());
        assertTrue(msg.getContent().contains("Charlie"));
        assertTrue(msg.getContent().contains("joined"));
    }

    @Test
    @DisplayName("SystemMessage userLeft factory should produce correct content")
    void testSystemMessageLeft() {
        SystemMessage msg = SystemMessage.userLeft("Dave");

        assertTrue(msg.getContent().contains("Dave"));
        assertTrue(msg.getContent().contains("left"));
    }

    // ─── ChatRoom Tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("ChatRoom should manage users correctly")
    void testChatRoomUserManagement() {
        ChatRoom room = new ChatRoom("test-room", "Test");
        User alice = new User.Builder("Alice").build();
        User bob   = new User.Builder("Bob").build();

        assertEquals(0, room.getUserCount());

        room.addUser(alice);
        room.addUser(bob);
        assertEquals(2, room.getUserCount());
        assertTrue(room.hasUser(alice.getUserId()));

        room.removeUser(alice.getUserId());
        assertEquals(1, room.getUserCount());
        assertFalse(room.hasUser(alice.getUserId()));
    }

    @Test
    @DisplayName("ChatRoom message history should be bounded")
    void testChatRoomBoundedHistory() {
        ChatRoom room = new ChatRoom("test", "Test");
        User user = new User.Builder("Alice").build();
        room.addUser(user);

        // Add more than MAX_HISTORY messages
        for (int i = 0; i < 110; i++) {
            room.addMessage(new ChatMessage(user.getUserId(), "Alice", "Message " + i));
        }

        // History should be capped at 100
        assertTrue(room.getAllMessages().size() <= 100,
            "Message history should be bounded to 100 messages");
    }
}
