import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client = null;

export function connectAlertSocket(onAlertReceived) {
  client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws-alerts'),
    reconnectDelay: 5000,
    onConnect: () => {
      console.log("Connected to WebSocket");

      client.subscribe('/topic/alerts', (message) => {
        const alert = JSON.parse(message.body);
        onAlertReceived(alert);
      });
    },
    onStompError: (frame) => {
      console.error("WebSocket error:", frame);
    }
  });

  client.activate();
}

export function disconnectAlertSocket() {
  if (client) {
    client.deactivate();
  }
}
