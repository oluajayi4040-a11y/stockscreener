// alertSocket.js - Raw WebSocket version
let socket = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 3000;

export function connectAlertSocket(onAlertReceived) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    console.log("WebSocket already connected");
    return;
  }

  socket = new WebSocket("ws://localhost:8080/alerts");
  
  socket.onopen = () => {
    console.log("✅ Raw WebSocket connected to alerts");
    reconnectAttempts = 0; // Reset reconnect attempts on successful connection
  };
  
  socket.onmessage = (event) => {
    try {
      const alert = JSON.parse(event.data);
      console.log("📨 Alert received:", alert);
      onAlertReceived(alert);
    } catch (error) {
      console.error("Failed to parse alert:", error, event.data);
    }
  };
  
  socket.onerror = (error) => {
    console.error("❌ WebSocket error:", error);
  };
  
  socket.onclose = (event) => {
    console.log(`WebSocket disconnected: ${event.code} - ${event.reason}`);
    
    // Auto-reconnect logic
    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
      reconnectAttempts++;
      console.log(`Reconnecting in ${RECONNECT_DELAY}ms... (Attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})`);
      setTimeout(() => {
        connectAlertSocket(onAlertReceived);
      }, RECONNECT_DELAY);
    } else {
      console.error("Max reconnection attempts reached. Please refresh the page.");
    }
  };
}

export function disconnectAlertSocket() {
  if (socket) {
    socket.close();
    socket = null;
    console.log("WebSocket disconnected manually");
  }
}

export function isSocketConnected() {
  return socket && socket.readyState === WebSocket.OPEN;
}