# ⬡ ChatApp — Real-Time Chat Web Application

A production-quality **real-time chat application** built with Java, WebSockets, and Maven — designed to demonstrate strong OOP fundamentals, clean architecture, and modern frontend practices.

---

## 🚀 Tech Stack

| Layer        | Technology                                |
|--------------|-------------------------------------------|
| Language     | Java 11                                   |
| Build Tool   | Apache Maven 3.x                          |
| Backend      | Java WebSocket API (JSR-356)              |
| Server       | Apache Tomcat 9+ (or any Java EE container) |
| JSON         | Jackson Databind 2.15                     |
| Logging      | SLF4J + Logback                           |
| Testing      | JUnit 5 + Mockito                         |
| Frontend     | HTML5, CSS3, Vanilla JavaScript (ES6+)    |

---

## 🧠 OOP Concepts Demonstrated

### 1. **Encapsulation**
- `User` class: private fields with controlled getter/setter access
- `ChatRoom` returns `unmodifiableCollection` to prevent external mutation
- `SessionManager` owns session state; exposes only safe methods

### 2. **Abstraction**
- `Message` is an **abstract class** defining the contract (`getContent()`, `toJson()`)
- `ChatService` is an **interface** — business logic is decoupled from implementation
- Callers depend on the interface, not `ChatServiceImpl`

### 3. **Inheritance & Polymorphism**
- `ChatMessage extends Message` — overrides `getContent()` and `toJson()`
- `SystemMessage extends Message` — different JSON payload, same base contract
- `MessageType` enum drives frontend rendering polymorphically

### 4. **Design Patterns**
| Pattern   | Where Used                                          |
|-----------|-----------------------------------------------------|
| Singleton | `ChatServiceImpl`, `SessionManager`                 |
| Builder   | `User.Builder` for flexible, readable construction  |
| Factory   | `SystemMessage.userJoined()`, `userLeft()`          |
| Strategy  | `ChatService` interface / `ChatServiceImpl`         |
| Observer  | WebSocket broadcast callbacks in `SessionManager`   |

### 5. **SOLID Principles**
- **S**ingle Responsibility: each class has one job
- **O**pen/Closed: add new message types by extending `Message`
- **L**iskov Substitution: `ChatMessage`/`SystemMessage` are interchangeable as `Message`
- **I**nterface Segregation: `ChatService` exposes only what clients need
- **D**ependency Inversion: `ChatWebSocketServer` depends on `ChatService` interface

---

## 📁 Project Structure

```
ChatApp/
├── pom.xml                         # Maven build configuration
├── README.md
│
├── src/main/java/com/chatapp/
│   ├── model/
│   │   ├── Message.java            # Abstract base class (Abstraction)
│   │   ├── ChatMessage.java        # Concrete message (Inheritance)
│   │   ├── SystemMessage.java      # System events (Polymorphism)
│   │   ├── User.java               # User domain object (Builder pattern)
│   │   └── ChatRoom.java           # Room with bounded history
│   │
│   ├── service/
│   │   ├── ChatService.java        # Service interface (Abstraction)
│   │   └── ChatServiceImpl.java    # Singleton implementation
│   │
│   ├── server/
│   │   ├── ChatWebSocketServer.java # @ServerEndpoint WebSocket handler
│   │   └── SessionManager.java      # Singleton session registry
│   │
│   └── util/
│       └── MessageParser.java      # JSON parsing utility (static class)
│
├── src/main/webapp/
│   ├── index.html                  # Single-page chat UI
│   ├── css/style.css               # Dark-theme, responsive design
│   ├── js/app.js                   # OOP JavaScript (classes)
│   └── WEB-INF/web.xml
│
└── src/test/java/com/chatapp/
    └── model/ModelTest.java        # JUnit 5 unit tests
```

---

## 🛠 Build & Run

### Prerequisites
- Java 11+
- Maven 3.6+
- Apache Tomcat 9+ (or use `tomcat7-maven-plugin`)

### Build the WAR
```bash
mvn clean package
```

### Run locally with Maven Tomcat plugin
```bash
mvn tomcat7:run
```
Then open: **http://localhost:8080**

### Deploy to Tomcat
```bash
cp target/ChatApp.war $TOMCAT_HOME/webapps/
```

### Run Tests
```bash
mvn test
```

---

## 💡 Features

- ✅ Real-time messaging via WebSockets (no polling)
- ✅ Join/leave notifications with animated system messages
- ✅ Live typing indicators (debounced, multi-user)
- ✅ Online users sidebar with avatar colors
- ✅ Character limit counter
- ✅ Emoji quick-picker
- ✅ Auto-reconnect with exponential backoff
- ✅ Heartbeat ping/pong to keep connections alive
- ✅ Thread-safe with `ConcurrentHashMap` throughout
- ✅ Bounded message history (last 100 messages)
- ✅ Input sanitization (XSS prevention, JSON escaping)

---

## 🧪 Running Tests

```bash
mvn test
```

Tests cover:
- `User` builder, initials, equality, message count
- `ChatMessage` content validation, JSON output
- `SystemMessage` factory methods
- `ChatRoom` user management and bounded history

---

## 📐 Architecture Diagram

```
Browser (HTML/CSS/JS)
       │
       │  WebSocket (ws://host/ws/chat)
       │
ChatWebSocketServer.java  (@ServerEndpoint)
       │
       ├── MessageParser.util    (parse JSON frames)
       ├── SessionManager        (track active sessions)
       └── ChatService           (interface)
              │
         ChatServiceImpl         (Singleton)
              │
         ChatRoom  ◄──── User
              │
         Message (abstract)
           ├── ChatMessage
           └── SystemMessage
```

---

## 👨‍💻 Author Notes

Built as a portfolio project to demonstrate:
- Real-world Java architecture (not just Hello World)
- Proper use of interfaces, abstract classes, and design patterns
- WebSocket protocol understanding
- Frontend/Backend separation of concerns
- Testability and clean code


Flagship project
