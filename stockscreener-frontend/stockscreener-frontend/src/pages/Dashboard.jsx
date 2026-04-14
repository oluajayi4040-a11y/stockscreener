import React, { useEffect, useState, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import "./Dashboard.css";

function Dashboard() {
  const [watchlist, setWatchlist] = useState([]);
  const [newSymbol, setNewSymbol] = useState("");
  const [expanded, setExpanded] = useState({});
  const [breakouts, setBreakouts] = useState([]);
  const [connectionStatus, setConnectionStatus] = useState("Connecting...");

  const previousPrices = useRef({});
  const stompClientRef = useRef(null);

  // Compute Daily % only
  const computeMetrics = (item) => {
    const { price, previousClose } = item;
    let dailyChange = 0;
    if (price > 0 && previousClose > 0) {
      dailyChange = ((price - previousClose) / previousClose) * 100;
    }
    return { dailyChange };
  };

  // Load watchlist from backend
  const loadWatchlist = async () => {
    try {
      const response = await fetch("http://localhost:8080/watchlist");
      const data = await response.json();

      const enriched = data.map((item) => {
        const { dailyChange } = computeMetrics(item);
        const prev = previousPrices.current[item.symbol];
        let flashClass = "";

        if (prev !== undefined && item.price > 0) {
          if (item.price > prev) flashClass = "flash-green";
          if (item.price < prev) flashClass = "flash-red";
        }

        if (item.price > 0) {
          previousPrices.current[item.symbol] = item.price;
        }

        return {
          ...item,
          premarketHigh: item.premarketHigh,
          premarketLow: item.premarketLow,
          dailyChange,
          flashClass,
        };
      });

      setWatchlist(enriched);
      console.log("📊 Watchlist loaded:", enriched.length, "symbols");
    } catch (error) {
      console.error("Error loading watchlist:", error);
    }
  };

  // Load existing alerts from backend
  const loadAlerts = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/alerts/today");
      const data = await response.json();
      const sortedAlerts = data.sort((a, b) => {
        const timeA = new Date(a.triggeredAt || a.createdAt || 0);
        const timeB = new Date(b.triggeredAt || b.createdAt || 0);
        return timeA - timeB;
      });
      setBreakouts(sortedAlerts);
      console.log("📋 Loaded alerts:", sortedAlerts.length);
    } catch (error) {
      console.error("Error loading alerts:", error);
    }
  };

  // STOMP WebSocket for real-time price updates
  useEffect(() => {
    loadWatchlist();
    loadAlerts();

    console.log("🔌 Connecting to STOMP WebSocket...");
    
    const socket = new SockJS("http://localhost:8080/ws");
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: (str) => console.log("STOMP Debug:", str),
    });

    stompClient.onConnect = () => {
      console.log("✅ STOMP WebSocket connected successfully!");
      setConnectionStatus("Connected");
      
      stompClient.subscribe("/topic/prices", (message) => {
        try {
          console.log("📨 Price update received:", message.body);
          const msg = JSON.parse(message.body);
          const { symbol, price } = msg;

          setWatchlist((current) =>
            current.map((item) => {
              if (item.symbol !== symbol) return item;

              const prevPrice = previousPrices.current[symbol];
              let flashClass = "";

              if (prevPrice !== undefined && price > 0) {
                if (price > prevPrice) flashClass = "flash-green";
                if (price < prevPrice) flashClass = "flash-red";
              }

              if (price > 0) {
                previousPrices.current[symbol] = price;
              }

              const updatedItem = { ...item, price, flashClass };
              const { dailyChange } = computeMetrics(updatedItem);

              console.log(`🔄 ${symbol} price updated: $${price} (${dailyChange.toFixed(2)}% daily)`);
              return { ...updatedItem, dailyChange };
            })
          );
        } catch (error) {
          console.error("❌ Error processing price update:", error);
        }
      });
    };

    stompClient.onStompError = (frame) => {
      console.error("❌ STOMP Error:", frame);
      setConnectionStatus("STOMP Error");
    };

    stompClient.onWebSocketError = (error) => {
      console.error("❌ WebSocket Error:", error);
      setConnectionStatus("WebSocket Error");
    };

    stompClient.onDisconnect = () => {
      console.log("🔌 STOMP WebSocket disconnected");
      setConnectionStatus("Disconnected");
    };

    stompClient.activate();
    stompClientRef.current = stompClient;

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, []);

  // WebSocket listener for breakout alerts
  useEffect(() => {
    console.log("🔌 Connecting to raw WebSocket for alerts...");
    const ws = new WebSocket("ws://localhost:8080/alerts");

    ws.onopen = () => {
      console.log("✅ Raw WebSocket connected for alerts");
    };

    ws.onmessage = (event) => {
      try {
        const alert = JSON.parse(event.data);
        console.log("🚨 Breakout alert received:", alert);
        
        setBreakouts((prev) => {
          const newAlert = { ...alert };
          if (!newAlert.timestamp && newAlert.triggeredAt) {
            newAlert.timestamp = newAlert.triggeredAt;
          }
          if (!newAlert.timestamp) {
            newAlert.timestamp = new Date().toISOString();
          }
          
          const newList = [...prev, newAlert];
          newList.sort((a, b) => {
            const timeA = new Date(a.timestamp || a.triggeredAt || a.createdAt || 0);
            const timeB = new Date(b.timestamp || b.triggeredAt || b.createdAt || 0);
            return timeA - timeB;
          });
          return newList;
        });
      } catch (error) {
        console.error("❌ Error parsing alert:", error);
      }
    };

    ws.onerror = (error) => {
      console.error("❌ Raw WebSocket error:", error);
    };

    ws.onclose = () => {
      console.log("🔌 Raw WebSocket disconnected");
    };

    return () => ws.close();
  }, []);

  // Add symbol
  const addSymbol = async () => {
    if (!newSymbol.trim()) return;

    try {
      console.log("➕ Adding symbol:", newSymbol.toUpperCase());
      await fetch("http://localhost:8080/watchlist", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ symbol: newSymbol.toUpperCase() }),
      });

      setNewSymbol("");
      await loadWatchlist();
      console.log("✅ Symbol added successfully");
    } catch (error) {
      console.error("Error adding symbol:", error);
    }
  };

  // Delete symbol
  const deleteSymbol = async (symbol) => {
    try {
      console.log("🗑️ Deleting symbol:", symbol);
      const response = await fetch(`http://localhost:8080/watchlist/${symbol}`, {
        method: "DELETE",
      });
      
      if (!response.ok) {
        console.error(`Failed to delete ${symbol}: ${response.status}`);
      }
      
      setBreakouts((prev) => prev.filter(b => b.symbol !== symbol));
      
      await loadWatchlist();
      console.log("✅ Symbol deleted successfully");
    } catch (error) {
      console.error("Error deleting symbol:", error);
    }
  };

  const formatPercent = (value) => {
    if (!value || isNaN(value)) return "0.00%";
    return `${value.toFixed(2)}%`;
  };

  const toggleExpand = (symbol) => {
    setExpanded((prev) => ({ ...prev, [symbol]: !prev[symbol] }));
  };

  // Filter breakouts by candle type
  const oneMinBuy = breakouts.filter(b => b.candleType === "ONE_MIN" && b.type === "HIGH");
  const oneMinSell = breakouts.filter(b => b.candleType === "ONE_MIN" && b.type === "LOW");
  const fiveMinBuy = breakouts.filter(b => b.candleType === "FIVE_MIN" && b.type === "HIGH");
  const fiveMinSell = breakouts.filter(b => b.candleType === "FIVE_MIN" && b.type === "LOW");
  const fifteenMinBuy = breakouts.filter(b => b.candleType === "FIFTEEN_MIN" && b.type === "HIGH");
  const fifteenMinSell = breakouts.filter(b => b.candleType === "FIFTEEN_MIN" && b.type === "LOW");

  const totalBreakouts = breakouts.length;

  return (
    <div className="dashboard-container">
      {/* TOP NAV */}
      <header className="top-nav">
        <h2 className="logo">Stock Screener</h2>
        <nav className="nav-links">
          <span className="active">Dashboard</span>
          <span>Watchlist</span>
          <span>Alerts</span>
        </nav>
        <div className="profile-icon">👤</div>
      </header>

      {/* STATS */}
      <section className="stats-section">
        <div className="stat-card">
          <h3>Total Watchlist</h3>
          <p className="stat-number">{watchlist.length}</p>
        </div>
        <div className="stat-card">
          <h3>Opening Movers</h3>
          <p className="stat-number">{totalBreakouts}</p>
        </div>
        <div className="stat-card">
          <h3>Alerts Triggered</h3>
          <p className="stat-number">0</p>
        </div>
      </section>

      {/* 1-MINUTE BREAKOUTS (8:30-8:40 AM CST) */}
      <div className="breakouts-container">
        <div className="breakout-section-buy">
          <h3>📈 BUY (1min)</h3>
          {oneMinBuy.length === 0 ? (
            <p className="empty-breakouts">No buy signals yet</p>
          ) : (
            <ul className="breakout-list">
              {oneMinBuy.map((b, index) => (
                <li key={`buy-1min-${b.symbol}-${index}`} className="breakout-item-buy">
                  <span className="breakout-symbol">{b.symbol}</span>
                  <span className="breakout-direction breakout-direction-buy">▲ HIGH</span>
                  <div className="breakout-details">Price: ${b.price} | H: {b.premarketHigh}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="breakout-section-sell">
          <h3>📉 SELL (1min)</h3>
          {oneMinSell.length === 0 ? (
            <p className="empty-breakouts">No sell signals yet</p>
          ) : (
            <ul className="breakout-list">
              {oneMinSell.map((b, index) => (
                <li key={`sell-1min-${b.symbol}-${index}`} className="breakout-item-sell">
                  <span className="breakout-symbol">{b.symbol}</span>
                  <span className="breakout-direction breakout-direction-sell">▼ LOW</span>
                  <div className="breakout-details">Price: ${b.price} | L: {b.premarketLow}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* 5-MINUTE BREAKOUTS (8:30-8:40 AM CST) */}
      <div className="breakouts-container">
        <div className="breakout-section-buy">
          <h3>📈 BUY (5min)</h3>
          {fiveMinBuy.length === 0 ? (
            <p className="empty-breakouts">No buy signals yet</p>
          ) : (
            <ul className="breakout-list">
              {fiveMinBuy.map((b, index) => (
                <li key={`buy-5min-${b.symbol}-${index}`} className="breakout-item-buy">
                  <span className="breakout-symbol">{b.symbol}</span>
                  <span className="breakout-direction breakout-direction-buy">▲ HIGH</span>
                  <div className="breakout-details">Price: ${b.price} | H: {b.premarketHigh}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="breakout-section-sell">
          <h3>📉 SELL (5min)</h3>
          {fiveMinSell.length === 0 ? (
            <p className="empty-breakouts">No sell signals yet</p>
          ) : (
            <ul className="breakout-list">
              {fiveMinSell.map((b, index) => (
                <li key={`sell-5min-${b.symbol}-${index}`} className="breakout-item-sell">
                  <span className="breakout-symbol">{b.symbol}</span>
                  <span className="breakout-direction breakout-direction-sell">▼ LOW</span>
                  <div className="breakout-details">Price: ${b.price} | L: {b.premarketLow}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* 15-MINUTE BREAKOUTS (After 8:45 AM CST) */}
      <div className="breakouts-container">
        <div className="breakout-section-buy">
          <h3>📈 BUY (15min)</h3>
          {fifteenMinBuy.length === 0 ? (
            <p className="empty-breakouts">No buy signals yet</p>
          ) : (
            <ul className="breakout-list">
              {fifteenMinBuy.map((b, index) => (
                <li key={`buy-15min-${b.symbol}-${index}`} className="breakout-item-buy">
                  <span className="breakout-symbol">{b.symbol}</span>
                  <span className="breakout-direction breakout-direction-buy">▲ HIGH</span>
                  <div className="breakout-details">Price: ${b.price} | H: {b.premarketHigh}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="breakout-section-sell">
          <h3>📉 SELL (15min)</h3>
          {fifteenMinSell.length === 0 ? (
            <p className="empty-breakouts">No sell signals yet</p>
          ) : (
            <ul className="breakout-list">
              {fifteenMinSell.map((b, index) => (
                <li key={`sell-15min-${b.symbol}-${index}`} className="breakout-item-sell">
                  <span className="breakout-symbol">{b.symbol}</span>
                  <span className="breakout-direction breakout-direction-sell">▼ LOW</span>
                  <div className="breakout-details">Price: ${b.price} | L: {b.premarketLow}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* WATCHLIST TABLE */}
      <section className="table-section">
        <h3>Your Watchlist</h3>
        <table className="watchlist-table">
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Company Name</th>
              <th>Price</th>
              <th>Daily %</th>
              <th>Premarket</th>
              <th></th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {watchlist.map((item) => (
              <React.Fragment key={item.symbol}>
                <tr className={item.flashClass}>
                  <td className="symbol-cell">{item.symbol}</td>
                  <td>{item.companyName || item.symbol}</td>
                  <td>{item.price && item.price > 0 ? `$${item.price.toFixed(2)}` : "—"}</td>
                  <td className={item.dailyChange >= 0 ? "green" : "red"}>
                    {item.dailyChange > 0 && "▲ "}
                    {item.dailyChange < 0 && "▼ "}
                    {formatPercent(item.dailyChange)}
                  </td>
                  <td>
                    {item.premarketHigh && item.premarketLow && (item.premarketHigh > 0 || item.premarketLow > 0) ? (
                      <div className="premarket-box">
                        <span>H: {item.premarketHigh.toFixed(2)}</span>
                        <span>L: {item.premarketLow.toFixed(2)}</span>
                      </div>
                    ) : "—"}
                  </td>
                  <td><button className="expand-btn" onClick={() => toggleExpand(item.symbol)}>
                    {expanded[item.symbol] ? "▲" : "▼"}
                  </button></td>
                  <td><button className="delete-btn" onClick={() => deleteSymbol(item.symbol)}>Remove</button></td>
                </tr>
                {expanded[item.symbol] && (
                  <tr className="expanded-row">
                    <td colSpan="7">
                      <div className="expanded-content">
                        <h4>{item.symbol} - {item.companyName || item.symbol} Details</h4>
                        <p><strong>Previous Close:</strong> {item.previousClose > 0 ? item.previousClose.toFixed(2) : "—"}</p>
                        <p><strong>Market Open:</strong> {item.marketOpen > 0 ? item.marketOpen.toFixed(2) : "—"}</p>
                        <p><strong>Latest Price:</strong> {item.price > 0 ? item.price.toFixed(2) : "—"}</p>
                        <p><strong>Daily Change:</strong> {formatPercent(item.dailyChange)}</p>
                      </div>
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </section>

      {/* ADD STOCK */}
      <section className="add-stock-section">
        <input type="text" placeholder="Enter stock symbol (e.g., AAPL)" className="stock-input" value={newSymbol} onChange={(e) => setNewSymbol(e.target.value)} />
        <button className="add-btn" onClick={addSymbol}>Add</button>
      </section>

      {/* CONNECTION STATUS */}
      <div className="connection-footer">
        <p style={{ textAlign: "center", fontSize: "12px", color: connectionStatus === "Connected" ? "#16a34a" : "#dc2626", padding: "10px 20px", margin: "0", borderTop: "1px solid #eee", background: "#fafafa" }}>
          🔌 WebSocket Status: {connectionStatus}
        </p>
      </div>
    </div>
  );
}

export default Dashboard;