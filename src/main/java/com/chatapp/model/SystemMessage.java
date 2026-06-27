package com.chatapp.model;

/**
 * System-generated message (user joined, left, etc.)
 * Demonstrates: Inheritance, Polymorphism.
 */
public class SystemMessage extends Message {

    public enum SystemEvent {
        USER_JOINED("joined the room 🎉"),
        USER_LEFT("left the room 👋"),
        ROOM_CREATED("Chat room is ready ✨"),
        USER_RENAMED("changed their name");

        private final String description;

        SystemEvent(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final SystemEvent event;

    public SystemMessage(String username, SystemEvent event) {
        super("SYSTEM", "System", MessageType.SYSTEM);
        this.event = event;
        // Reassign senderName to be the triggering username for display
        // (using senderId field for the actual username in this context)
        // We override toJson to include the triggering user
        // The 'senderId' here carries the username who triggered the event
    }

    // Factory method pattern for clean creation
    public static SystemMessage userJoined(String username) {
        return new SystemMessage(username, SystemEvent.USER_JOINED) {
            @Override
            public String getSenderId() { return username; }

            @Override
            public String getContent() {
                return username + " " + SystemEvent.USER_JOINED.getDescription();
            }
        };
    }

    public static SystemMessage userLeft(String username) {
        return new SystemMessage(username, SystemEvent.USER_LEFT) {
            @Override
            public String getSenderId() { return username; }

            @Override
            public String getContent() {
                return username + " " + SystemEvent.USER_LEFT.getDescription();
            }
        };
    }

    @Override
    public String getContent() {
        return senderId + " " + event.getDescription();
    }

    @Override
    public String toJson() {
        return String.format("{%s,\"content\":\"%s\",\"event\":\"%s\"}",
            baseJson(),
            getContent(),
            event.name()
        );
    }

    public SystemEvent getEvent() {
        return event;
    }
}
