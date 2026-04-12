import { useEffect, useState } from "react";
import { connectAlertSocket, disconnectAlertSocket } from "./alertSocket";
import "./Alerts.css"; // ⭐ NEW: fade-in animation

export default function Alerts() {
  const [alerts, setAlerts] = useState([]);

  useEffect(() => {
    // Fetch existing alerts on page load
    fetch("http://localhost:8080/alerts")
      .then((res) => res.json())
      .then((data) => setAlerts(data));

    // Connect WebSocket
    connectAlertSocket((newAlert) => {
      console.log("New alert received:", newAlert);

      // Add new alert to top of list
      setAlerts((prev) => [newAlert, ...prev]);
    });

    // Cleanup on unmount
    return () => {
      disconnectAlertSocket();
    };
  }, []);

  return (
    <div style={{ padding: "20px" }}>
      <h1>Real-Time Alerts</h1>

      {alerts.length === 0 && <p>No alerts yet...</p>}

      {alerts.map((alert, index) => (
        <div
          key={index}
          className="alert-item" // ⭐ NEW: animation class
          style={{
            padding: "12px",
            marginBottom: "10px",
            borderRadius: "6px",
            background: alert.direction === "HIGH" ? "#d4f7d4" : "#ffd4d4",
            border: "1px solid #ccc",
          }}
        >
          <strong>{alert.symbol}</strong> — {alert.direction} BREAKOUT
          <br />
          Price: {alert.price}
          <br />
          Premarket High: {alert.premarketHigh}
          <br />
          Premarket Low: {alert.premarketLow}
          <br />
          <small>{new Date(alert.createdAt).toLocaleString()}</small>
        </div>
      ))}
    </div>
  );
}
