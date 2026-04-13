import { useEffect, useState, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import "./Dashboard.css";

function Dashboard() {
  const [watchlist, setWatchlist] = useState([]);
  const [newSymbol, setNewSymbol] = useState("");
  const [expanded, setExpanded] = useState({});
  const [breakouts, setBreakouts] = useState([]);

  const previousPrices = useRef({});
  const stompClientRef = useRef(null);

  // Compute Daily % and Intraday %
  const computeMetrics = (item) => {
    const { price, previousClose, marketOpen } = item;

    let dailyChange = 0;
    let intradayChange = 0;

    if (price > 0 && previousClose > 0) {
      dailyChange = ((price - previousClose) / previousClose) * 100;
    }

    if (price > 0 && marketOpen > 0) {
      intradayChange = ((price - marketOpen) / marketOpen) * 100;
    }

    return { dailyChange, intradayChange };
  };

  // Load watchlist from backend
  const loadWatchlist = async () => {
    try {
      const response = await fetch("http://localhost:8080/watchlist");
      const data = await response.json();

      const enriched = data.map((item) => {
        const { dailyChange, intradayChange } = computeMetrics(item);

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
          intradayChange,
          flashClass,
        };
      });

      setWatchlist(enriched);
    } catch (error) {
      console.error("Error loading watchlist:", error);
    }
  };

  // ⭐ STOMP WebSocket for real-time price updates
  useEffect(() => {
    loadWatchlist();

    const socket = new SockJS("http://localhost:8080/ws");
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
    });

    stompClient.onConnect = () => {
      stompClient.subscribe("/topic/prices", (message) => {
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
            const { dailyChange, intradayChange } = computeMetrics(updatedItem);

            return { ...updatedItem, dailyChange, intradayChange };
          })
        );
      });
    };

    stompClient.activate();
    stompClientRef.current = stompClient;

    return () => stompClient.deactivate();
  }, []);

  // ⭐ WebSocket listener for breakout alerts
  useEffect(() => {
    const ws = new WebSocket("ws://localhost:8080/alerts");

    ws.onmessage = (event) => {
      const alert = JSON.parse(event.data);
      setBreakouts((prev) => [...prev, alert]);
    };

    return () => ws.close();
  }, []);

  // Add symbol
  const addSymbol = async () => {
    if (!newSymbol.trim()) return;

    await fetch("http://localhost:8080/watchlist", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ symbol: newSymbol.toUpperCase() }),
    });

    setNewSymbol("");
    await loadWatchlist();
  };

  // Delete symbol
  const deleteSymbol = async (symbol) => {
    await fetch(`http://localhost:8080/watchlist/${symbol}`, {
      method: "DELETE",
    });

    await loadWatchlist();
  };

  const formatPercent = (value) => {
    if (!value || isNaN(value)) return "0.00%";
    return `${value.toFixed(2)}%`;
  };

  const toggleExpand = (symbol) => {
    setExpanded((prev) => ({ ...prev, [symbol]: !prev[symbol] }));
  };

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
          <h3>Premarket Breakouts</h3>
          <p className="stat-number">{breakouts.length}</p>
        </div>

        <div className="stat-card">
          <h3>Alerts Triggered</h3>
          <p className="stat-number">0</p>
        </div>
      </section>

      {/* BREAKOUT LIST */}
      <section className="breakout-section">
        <h3>Premarket Breakouts</h3>

        {breakouts.length === 0 ? (
          <p>No breakouts yet</p>
        ) : (
          <ul className="breakout-list">
            {breakouts.map((b, index) => (
              <li key={index} className="breakout-item">
                <strong>{b.symbol}</strong>{" "}
                {b.type === "HIGH" ? (
                  <span style={{ color: "green", fontWeight: "bold" }}>↑</span>
                ) : (
                  <span style={{ color: "red", fontWeight: "bold" }}>↓</span>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* WATCHLIST TABLE */}
      <section className="table-section">
        <h3>Your Watchlist</h3>

        <table className="watchlist-table">
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Price</th>
              <th>Daily %</th>
              <th>Intraday %</th>
              <th>Premarket</th>
              <th></th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {watchlist.map((item) => (
              <>
                <tr key={item.symbol} className={item.flashClass}>
                  <td className="symbol-cell">{item.symbol}</td>

                  <td>
                    {item.price && item.price > 0
                      ? `$${item.price.toFixed(2)}`
                      : "—"}
                  </td>

                  <td className={item.dailyChange >= 0 ? "green" : "red"}>
                    {item.dailyChange > 0 && "▲ "}
                    {item.dailyChange < 0 && "▼ "}
                    {formatPercent(item.dailyChange)}
                  </td>

                  <td className={item.intradayChange >= 0 ? "green" : "red"}>
                    {item.intradayChange > 0 && "▲ "}
                    {item.intradayChange < 0 && "▼ "}
                    {formatPercent(item.intradayChange)}
                  </td>

                  <td>
                    {item.premarketHigh && item.premarketLow ? (
                      <div className="premarket-box">
                        <span>H: {item.premarketHigh.toFixed(2)}</span>
                        <span>L: {item.premarketLow.toFixed(2)}</span>
                      </div>
                    ) : (
                      "—"
                    )}
                  </td>

                  <td>
                    <button
                      className="expand-btn"
                      onClick={() => toggleExpand(item.symbol)}
                    >
                      {expanded[item.symbol] ? "▲" : "▼"}
                    </button>
                  </td>

                  <td>
                    <button
                      className="delete-btn"
                      onClick={() => deleteSymbol(item.symbol)}
                    >
                      Remove
                    </button>
                  </td>
                </tr>

                {expanded[item.symbol] && (
                  <tr className="expanded-row">
                    <td colSpan="7">
                      <div className="expanded-content">
                        <h4>{item.symbol} Details</h4>

                        <p>
                          <strong>Previous Close:</strong>{" "}
                          {item.previousClose.toFixed(2)}
                        </p>
                        <p>
                          <strong>Market Open:</strong>{" "}
                          {item.marketOpen.toFixed(2)}
                        </p>
                        <p>
                          <strong>Latest Price:</strong>{" "}
                          {item.price.toFixed(2)}
                        </p>

                        <p>
                          <strong>Daily Change:</strong>{" "}
                          {formatPercent(item.dailyChange)}
                        </p>

                        <p>
                          <strong>Intraday Change:</strong>{" "}
                          {formatPercent(item.intradayChange)}
                        </p>
                      </div>
                    </td>
                  </tr>
                )}
              </>
            ))}
          </tbody>
        </table>
      </section>

      {/* ADD STOCK */}
      <section className="add-stock-section">
        <input
          type="text"
          placeholder="Enter stock symbol (e.g., AAPL)"
          className="stock-input"
          value={newSymbol}
          onChange={(e) => setNewSymbol(e.target.value)}
        />
        <button className="add-btn" onClick={addSymbol}>
          Add
        </button>
      </section>
    </div>
  );
}

export default Dashboard;
