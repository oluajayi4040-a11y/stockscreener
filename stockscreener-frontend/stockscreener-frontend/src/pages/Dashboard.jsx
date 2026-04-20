import React, { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import axios from "axios";
import "./Dashboard.css";

export default function Dashboard() {
  const [buySignals, setBuySignals] = useState([]);
  const [sellSignals, setSellSignals] = useState([]);
  const [theme, setTheme] = useState("dark");

  // Load theme from localStorage
  useEffect(() => {
    const saved = localStorage.getItem("theme");
    if (saved) setTheme(saved);
  }, []);

  // Apply theme + save
  useEffect(() => {
    document.body.className = theme === "dark" ? "dark-mode" : "light-mode";
    localStorage.setItem("theme", theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === "dark" ? "light" : "dark"));
  };

  // Load existing signals
  useEffect(() => {
    axios
      .get("/api/scan/signals")
      .then((res) => {
        const buys = res.data.filter((s) => s.direction === "BREAKOUT_UP");
        const sells = res.data.filter((s) => s.direction === "BREAKOUT_DOWN");
        setBuySignals(buys);
        setSellSignals(sells);
      })
      .catch((err) => console.error("Error loading signals:", err));
  }, []);

  // WebSocket
  useEffect(() => {
    const client = new Client({
      brokerURL: "ws://localhost:8080/ws",
      reconnectDelay: 5000,
    });

    client.onConnect = () => {
      client.subscribe("/topic/signals", (message) => {
        const signal = JSON.parse(message.body);
        const key = (s) => `${s.symbol}-${s.direction}`;

        const enhanced = { ...signal, _new: true };

        if (signal.direction === "BREAKOUT_UP") {
          setBuySignals((prev) => {
            if (prev.some((s) => key(s) === key(signal))) return prev;
            return [enhanced, ...prev];
          });
        } else {
          setSellSignals((prev) => {
            if (prev.some((s) => key(s) === key(signal))) return prev;
            return [enhanced, ...prev];
          });
        }
      });
    };

    client.activate();
    return () => client.deactivate();
  }, []);

  // Helpers
  const formatPrice = (v) =>
    v === null || v === undefined ? "-" : `$${v.toFixed(2)}`;

  const formatTime = (ts) =>
    ts ? new Date(ts).toLocaleTimeString() : "";

  // Render card
  const renderSignalCard = (signal, type) => {
    const isBuy = type === "BUY";

    const glowClass = signal._new
      ? isBuy
        ? "glow-green"
        : "glow-red"
      : "";

    const slideClass = signal._new ? "slide-in" : "";

    // Strength color coding
    const strengthColor =
      signal.strength >= 85
        ? "strength-strong"
        : signal.strength >= 60
        ? "strength-medium"
        : "strength-weak";

    return (
      <div
        key={`${signal.symbol}-${signal.timestamp}-${signal.direction}`}
        className={`signal-card 
          ${isBuy ? "signal-buy" : "signal-sell"} 
          ${glowClass} 
          ${slideClass}`}
      >
        <div className="signal-card-header">
          <div className="signal-symbol">{signal.symbol}</div>
          <div className={isBuy ? "signal-direction-buy" : "signal-direction-sell"}>
            {isBuy ? "▲ Breakout Up" : "▼ Breakout Down"}
          </div>
        </div>

        <div className="signal-data-row">
          <span>{formatPrice(signal.lastPrice)}</span>
          <span className="signal-divider">|</span>

          <span>VWAP {formatPrice(signal.vwap)}</span>
          <span className="signal-divider">|</span>

          <span>PMH {formatPrice(signal.premarketHigh)}</span>
          <span className="signal-divider">|</span>

          <span>PML {formatPrice(signal.premarketLow)}</span>
          <span className="signal-divider">|</span>

          <span>{formatTime(signal.timestamp)}</span>
        </div>

        {/* Strength Row */}
        <div className="signal-strength-row">
          <span className="strength-label">Strength:</span>
          <span className={`strength-value ${strengthColor}`}>
            {signal.strength ?? "-"}
          </span>
        </div>
      </div>
    );
  };

  // Top Strength Signals
  const topStrengthSignals = [...buySignals, ...sellSignals]
    .filter((s) => s.strength !== null && s.strength !== undefined)
    .sort((a, b) => b.strength - a.strength)
    .slice(0, 5);

  // Layout
  return (
    <div className="dashboard-container">
      <div className="dashboard-inner">

        {/* Header */}
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-header-title">Breakout Scanner Dashboard</h1>
            <p className="dashboard-header-subtitle">
              Real-time breakout signals (8:30–8:50 AM CST)
            </p>
          </div>

          {/* Dark Mode Toggle */}
          <div className="toggle-wrapper" onClick={toggleTheme}>
            <div className={`toggle-switch ${theme === "dark" ? "toggle-dark" : "toggle-light"}`}>
              <div className="toggle-circle"></div>
            </div>
            <span className="toggle-label">
              {theme === "dark" ? "🌙 Dark Mode" : "☀️ Light Mode"}
            </span>
          </div>
        </div>

        {/* Top Strength Signals */}
        <div style={{ marginBottom: "35px" }}>
          <h2 className="section-title-buy">🔥 Top Strength Signals</h2>
          {topStrengthSignals.length === 0 ? (
            <div className="empty-state">No strong signals yet.</div>
          ) : (
            topStrengthSignals.map((s) =>
              renderSignalCard(s, s.direction === "BREAKOUT_UP" ? "BUY" : "SELL")
            )
          )}
        </div>

        {/* ⭐ SIDE-BY-SIDE BUY & SELL COLUMNS */}
        <div className="signal-columns">
          {/* BUY COLUMN */}
          <div className="signal-column">
            <h2 className="section-title-buy">BUY SIGNALS (▲)</h2>
            {buySignals.length === 0 ? (
              <div className="empty-state">No buy signals yet.</div>
            ) : (
              buySignals.map((s) => renderSignalCard(s, "BUY"))
            )}
          </div>

          {/* SELL COLUMN */}
          <div className="signal-column">
            <h2 className="section-title-sell">SELL SIGNALS (▼)</h2>
            {sellSignals.length === 0 ? (
              <div className="empty-state">No sell signals yet.</div>
            ) : (
              sellSignals.map((s) => renderSignalCard(s, "SELL"))
            )}
          </div>
        </div>

      </div>
    </div>
  );
}
