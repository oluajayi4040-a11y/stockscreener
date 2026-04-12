import "./Home.css";
import { useNavigate } from "react-router-dom";

function Home() {
  const navigate = useNavigate();

  return (
    <div className="home-container">

      {/* HERO SECTION */}
      <section className="hero">
        <div className="hero-content">
          <h1>Your Personal Stock Screener</h1>
          <p>
            Track your watchlist, monitor breakouts, and stay ahead of the market — all in one place.
          </p>
          <button
            className="cta-button"
            onClick={() => navigate("/dashboard")}
          >
            Go to Dashboard
          </button>
        </div>
      </section>

      {/* FEATURES SECTION */}
      <section className="features">
        <div className="feature-card">
          <h3>📊 Watchlist Management</h3>
          <p>Add, remove, and monitor your favorite stocks effortlessly.</p>
        </div>

        <div className="feature-card">
          <h3>⚡ Real-Time Market Data</h3>
          <p>Live prices, trends, and premarket signals at your fingertips.</p>
        </div>

        <div className="feature-card">
          <h3>🔔 Smart Alerts</h3>
          <p>Get notified instantly when your stocks move.</p>
        </div>
      </section>

      {/* CTA SECTION */}
      <section className="cta-section">
        <h2>Ready to explore your dashboard?</h2>
        <button
          className="cta-button large"
          onClick={() => navigate("/dashboard")}
        >
          Open Dashboard
        </button>
      </section>

      {/* FOOTER */}
      <footer className="footer">
        © 2026 Stock Screener — Built by Olu
      </footer>

    </div>
  );
}

export default Home;
