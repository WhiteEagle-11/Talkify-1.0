/**
 * ChatApp Frontend
 * ─────────────────────────────────────────────────────────────────────────────
 * Demonstrates OOP in JavaScript:
 *   - Classes with encapsulation (private-by-convention _prefix)
 *   - Inheritance via extends
 *   - Separation of concerns (ChatClient, UIManager, TypingManager)
 *   - Observer / callback pattern between modules
 */

'use strict';

/* ══════════════════════════════════════════════════════════════════════════════
   ChatClient — WebSocket connection manager
   ══════════════════════════════════════════════════════════════════════════════ */
class ChatClient {
  constructor(callbacks) {
    this._ws = null;
    this._connected = false;
    this._callbacks = callbacks;
    this._pingInterval = null;
    this._reconnectAttempts = 0;
    this._maxReconnect = 5;
  }

  connect() {
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    const url = `${protocol}://${location.host}/ws/chat`;

    console.log('[ChatClient] Connecting to:', url);
    this._ws = new WebSocket(url);

    this._ws.onopen    = () => this._onOpen();
    this._ws.onmessage = (e) => this._onMessage(e.data);
    this._ws.onclose   = (e) => this._onClose(e);
    this._ws.onerror   = (e) => this._onError(e);
  }

  _onOpen() {
    this._connected = true;
    this._reconnectAttempts = 0;
    console.log('[ChatClient] Connected');
    this._startPing();
    this._callbacks.onConnected?.();
  }

  _onMessage(raw) {
    try {
      const data = JSON.parse(raw);
      this._callbacks.onMessage?.(data);
    } catch (err) {
      console.error('[ChatClient] Parse error:', err);
    }
  }

  _onClose(event) {
    this._connected = false;
    this._stopPing();
    console.warn('[ChatClient] Disconnected:', event.code);
    this._callbacks.onDisconnected?.();

    if (this._reconnectAttempts < this._maxReconnect) {
      const delay = Math.min(1000 * 2 ** this._reconnectAttempts, 15000);
      this._reconnectAttempts++;
      console.log(`[ChatClient] Reconnecting in ${delay}ms (attempt ${this._reconnectAttempts})`);
      setTimeout(() => this.connect(), delay);
    }
  }

  _onError(event) {
    console.error('[ChatClient] WebSocket error:', event);
    this._callbacks.onError?.();
  }

  send(payload) {
    if (this._connected && this._ws?.readyState === WebSocket.OPEN) {
      this._ws.send(JSON.stringify(payload));
    } else {
      console.warn('[ChatClient] Cannot send — not connected');
    }
  }

  join(username) {
    this.send({ type: 'JOIN', username });
  }

  sendMessage(content, senderId) {
    this.send({ type: 'CHAT', content, senderId });
  }

  sendTyping(senderId) {
    this.send({ type: 'TYPING', senderId });
  }

  disconnect() {
    this._stopPing();
    this._ws?.close();
  }

  get isConnected() { return this._connected; }

  _startPing() {
    this._pingInterval = setInterval(() => this.send({ type: 'PING' }), 25_000);
  }

  _stopPing() {
    clearInterval(this._pingInterval);
  }
}

/* ══════════════════════════════════════════════════════════════════════════════
   TypingManager — debounced typing indicator logic
   ══════════════════════════════════════════════════════════════════════════════ */
class TypingManager {
  constructor(sendTypingFn, displayTimeout = 3000) {
    this._sendTyping = sendTypingFn;
    this._displayTimeout = displayTimeout;
    this._typingTimer = null;     // for debouncing outgoing signals
    this._hideTimer = null;       // for hiding incoming indicator
    this._typingUsers = new Set();
  }

  /** Called on every keypress in the input field */
  onKeystroke(userId) {
    if (!this._typingTimer) {
      this._sendTyping(userId);
    }
    clearTimeout(this._typingTimer);
    this._typingTimer = setTimeout(() => {
      this._typingTimer = null;
    }, 2500);
  }

  /** Called when a TYPING event arrives from another user */
  showTyping(username) {
    this._typingUsers.add(username);
    this._refreshDisplay();
    clearTimeout(this._hideTimer);
    this._hideTimer = setTimeout(() => {
      this._typingUsers.delete(username);
      this._refreshDisplay();
    }, this._displayTimeout);
  }

  _refreshDisplay() {
    const el   = document.getElementById('typingIndicator');
    const text = document.getElementById('typingText');
    if (this._typingUsers.size === 0) {
      el.classList.add('hidden');
    } else {
      const names = [...this._typingUsers];
      text.textContent = names.length === 1
        ? `${names[0]} is typing`
        : `${names.slice(0, -1).join(', ')} and ${names.at(-1)} are typing`;
      el.classList.remove('hidden');
    }
  }

  clear(username) {
    this._typingUsers.delete(username);
    this._refreshDisplay();
  }
}

/* ══════════════════════════════════════════════════════════════════════════════
   UIManager — all DOM manipulation
   ══════════════════════════════════════════════════════════════════════════════ */
class UIManager {
  constructor() {
    this.$joinScreen  = document.getElementById('joinScreen');
    this.$chatUI      = document.getElementById('chatUI');
    this.$messages    = document.getElementById('messages');
    this.$input       = document.getElementById('messageInput');
    this.$sendBtn     = document.getElementById('sendBtn');
    this.$userList    = document.getElementById('userList');
    this.$userCount   = document.getElementById('userCount');
    this.$headerCount = document.getElementById('headerCount');
    this.$myAvatar    = document.getElementById('myAvatar');
    this.$myName      = document.getElementById('myName');
    this.$connDot     = document.getElementById('connectionDot');
    this.$charCount   = document.getElementById('charCount');
    this.$joinError   = document.getElementById('joinError');
    this.$emojiPicker = document.getElementById('emojiPicker');
  }

  showChat() {
    this.$joinScreen.classList.add('hidden');
    this.$chatUI.classList.remove('hidden');
    this.$input.focus();
  }

  showJoin() {
    this.$chatUI.classList.add('hidden');
    this.$joinScreen.classList.remove('hidden');
  }

  setMyProfile(username, avatarColor, initials) {
    this.$myAvatar.textContent = initials;
    this.$myAvatar.style.background = avatarColor;
    this.$myAvatar.style.color = '#0D1117';
    document.getElementById('myName').textContent = username;
  }

  setConnectionStatus(online) {
    this.$connDot.classList.toggle('offline', !online);
    this.$connDot.title = online ? 'Connected' : 'Disconnected';
  }

  showJoinError(message) {
    this.$joinError.textContent = message;
    this.$joinError.classList.remove('hidden');
  }

  clearJoinError() {
    this.$joinError.classList.add('hidden');
  }

  updateCharCount(remaining) {
    this.$charCount.textContent = remaining;
    this.$charCount.className = 'char-count' +
      (remaining < 50 ? ' danger' : remaining < 150 ? ' warn' : '');
  }

  renderChatMessage(data, myUserId) {
    const isMine = data.senderId === myUserId;
    const el = document.createElement('div');
    el.className = `message${isMine ? ' is-mine' : ''}`;

    const initials = data.senderName
      ? data.senderName.slice(0, 2).toUpperCase()
      : '??';
    const color = data.avatarColor || '#00D4FF';

    el.innerHTML = `
      <div class="msg-avatar" style="background:${color};color:#0D1117">${initials}</div>
      <div class="msg-body">
        <div class="msg-meta">
          <span class="msg-sender" style="color:${color}">${this._escape(data.senderName)}</span>
          <span class="msg-time">${data.timestamp || ''}</span>
        </div>
        <div class="msg-bubble">${this._escape(data.content)}</div>
        ${data.reaction ? `<div class="msg-reaction">${data.reaction}</div>` : ''}
      </div>
    `;
    this.$messages.appendChild(el);
    this._scrollToBottom();
  }

  renderSystemMessage(content) {
    const el = document.createElement('div');
    el.className = 'message-system';
    el.innerHTML = `<span class="system-text">${this._escape(content)}</span>`;
    this.$messages.appendChild(el);
    this._scrollToBottom();
  }

  renderUserList(users, myUserId) {
    this.$userList.innerHTML = '';
    users.forEach(user => {
      const isMine = user.userId === myUserId;
      const el = document.createElement('div');
      el.className = `user-item${isMine ? ' is-me' : ''}`;
      el.innerHTML = `
        <div class="avatar" style="background:${user.avatarColor};color:#0D1117">${user.initials}</div>
        <span class="name">${this._escape(user.username)}</span>
        ${isMine ? '<span class="you-tag">you</span>' : ''}
      `;
      this.$userList.appendChild(el);
    });
    this.$userCount.textContent = users.length;
    this.$headerCount.textContent = users.length;
  }

  toggleEmojiPicker() {
    this.$emojiPicker.classList.toggle('hidden');
  }

  hideEmojiPicker() {
    this.$emojiPicker.classList.add('hidden');
  }

  getInputValue()   { return this.$input.value.trim(); }
  clearInput()      { this.$input.value = ''; this.$input.style.height = 'auto'; }
  focusInput()      { this.$input.focus(); }

  _scrollToBottom() {
    const wrapper = this.$messages.parentElement;
    wrapper.scrollTop = wrapper.scrollHeight;
  }

  _escape(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;')
      .replace(/\n/g, '<br>');
  }
}

/* ══════════════════════════════════════════════════════════════════════════════
   App — root controller, wires everything together
   ══════════════════════════════════════════════════════════════════════════════ */
class App {
  constructor() {
    this._myUserId   = null;
    this._myUsername = null;

    this._ui      = new UIManager();
    this._client  = new ChatClient({
      onConnected:    () => this._handleConnected(),
      onMessage:      (data) => this._handleMessage(data),
      onDisconnected: () => this._handleDisconnected(),
      onError:        () => this._handleError(),
    });
    this._typing  = new TypingManager(
      (uid) => this._client.sendTyping(uid)
    );

    this._bindEvents();
  }

  start() {
    this._client.connect();
  }

  // ─── Event Bindings ────────────────────────────────────────────────────────

  _bindEvents() {
    // Join button
    document.getElementById('joinBtn').addEventListener('click', () => this._tryJoin());

    // Username input — Enter to join
    document.getElementById('usernameInput').addEventListener('keydown', (e) => {
      if (e.key === 'Enter') this._tryJoin();
    });

    // Send button
    document.getElementById('sendBtn').addEventListener('click', () => this._trySend());

    // Message input — Enter to send, Shift+Enter for newline
    this._ui.$input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        this._trySend();
      }
    });

    // Typing indicator
    this._ui.$input.addEventListener('input', () => {
      const remaining = 500 - this._ui.$input.value.length;
      this._ui.updateCharCount(remaining);

      // Auto-grow textarea
      this._ui.$input.style.height = 'auto';
      this._ui.$input.style.height = Math.min(this._ui.$input.scrollHeight, 120) + 'px';

      if (this._myUserId && this._ui.$input.value.trim()) {
        this._typing.onKeystroke(this._myUserId);
      }
    });

    // Emoji picker toggle
    document.getElementById('emojiBtn').addEventListener('click', (e) => {
      e.stopPropagation();
      this._ui.toggleEmojiPicker();
    });

    // Emoji selection — insert into input
    document.getElementById('emojiPicker').addEventListener('click', (e) => {
      if (e.target.tagName === 'SPAN') {
        this._ui.$input.value += e.target.textContent;
        this._ui.$input.focus();
        this._ui.hideEmojiPicker();
      }
    });

    // Close emoji picker when clicking outside
    document.addEventListener('click', () => this._ui.hideEmojiPicker());

    // Leave button
    document.getElementById('leaveBtn').addEventListener('click', () => this._leave());
  }

  // ─── User Actions ──────────────────────────────────────────────────────────

  _tryJoin() {
    const username = document.getElementById('usernameInput').value.trim();
    if (!username || username.length < 2) {
      this._ui.showJoinError('Username must be at least 2 characters.');
      return;
    }
    if (!/^[a-zA-Z0-9_\- ]+$/.test(username)) {
      this._ui.showJoinError('Only letters, numbers, spaces, hyphens and underscores allowed.');
      return;
    }
    this._ui.clearJoinError();
    this._client.join(username);
  }

  _trySend() {
    const content = this._ui.getInputValue();
    if (!content || !this._myUserId) return;
    this._client.sendMessage(content, this._myUserId);
    this._ui.clearInput();
    this._ui.updateCharCount(500);
    this._ui.focusInput();
  }

  _leave() {
    this._client.disconnect();
    this._myUserId   = null;
    this._myUsername = null;
    this._ui.showJoin();
    document.getElementById('usernameInput').value = '';
    document.getElementById('messages').innerHTML = '';
  }

  // ─── WebSocket Handlers ────────────────────────────────────────────────────

  _handleConnected() {
    this._ui.setConnectionStatus(true);
    console.log('[App] WebSocket connected');
  }

  _handleDisconnected() {
    this._ui.setConnectionStatus(false);
  }

  _handleError() {
    this._ui.setConnectionStatus(false);
    this._ui.showJoinError('Connection error. Please refresh the page.');
  }

  _handleMessage(data) {
    switch (data.type) {
      case 'WELCOME':
        // Server is ready — nothing to do, we wait for user to click join
        break;

      case 'JOINED':
        this._myUserId   = data.userId;
        this._myUsername = data.username;
        this._ui.setMyProfile(
          data.username,
          data.avatarColor,
          data.username.slice(0, 2).toUpperCase()
        );
        this._ui.showChat();
        break;

      case 'CHAT':
        this._typing.clear(data.senderName);
        this._ui.renderChatMessage(data, this._myUserId);
        break;

      case 'SYSTEM':
        this._ui.renderSystemMessage(data.content);
        break;

      case 'USER_LIST':
        this._ui.renderUserList(data.users, this._myUserId);
        break;

      case 'TYPING':
        if (data.senderId !== this._myUserId) {
          this._typing.showTyping(data.username);
        }
        break;

      case 'ERROR':
        this._ui.showJoinError(data.message || 'An error occurred');
        break;

      case 'PONG':
        // heartbeat acknowledged
        break;

      default:
        console.warn('[App] Unknown message type:', data.type);
    }
  }
}

/* ══════════════════════════════════════════════════════════════════════════════
   Bootstrap
   ══════════════════════════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
  const app = new App();
  app.start();
  window.__chatApp = app; // expose for debugging in DevTools
});
