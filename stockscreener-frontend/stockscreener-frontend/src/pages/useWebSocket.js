// hooks/useWebSocket.js
import { useEffect, useRef, useState } from "react";

export function useWebSocket(url, onMessage) {
  const [isConnected, setIsConnected] = useState(false);
  const socketRef = useRef(null);

  useEffect(() => {
    socketRef.current = new WebSocket(url);

    socketRef.current.onopen = () => {
      console.log(`Connected to ${url}`);
      setIsConnected(true);
    };

    socketRef.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage(data);
      } catch (error) {
        console.error("Failed to parse message:", error);
      }
    };

    socketRef.current.onerror = (error) => {
      console.error("WebSocket error:", error);
      setIsConnected(false);
    };

    socketRef.current.onclose = () => {
      console.log(`Disconnected from ${url}`);
      setIsConnected(false);
    };

    return () => {
      if (socketRef.current) {
        socketRef.current.close();
      }
    };
  }, [url, onMessage]);

  return { isConnected };
}