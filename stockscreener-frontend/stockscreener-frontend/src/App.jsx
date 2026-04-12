import "./App.css";
import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import Home from "./pages/Home";
import Dashboard from "./pages/Dashboard";
import Alerts from "./pages/Alerts";

function App() {
  return (
    <Router>
      <div className="dashboard">
        {/* Sidebar */}
        <aside className="sidebar">
          <h2>Stock Screener</h2>
          <nav>
            <ul>
              <li>
                <Link to="/">Home</Link>
              </li>
              <li>
                <Link to="/dashboard">Dashboard</Link>
              </li>
              <li>
                <Link to="/alerts">Alerts</Link>
              </li>
              <li>Settings</li>
            </ul>
          </nav>
        </aside>

        {/* Main Content */}
        <main className="main">
          <header className="header">
            <h1>Stock Screener</h1>
          </header>

          <section className="content">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/alerts" element={<Alerts />} />
            </Routes>
          </section>
        </main>
      </div>
    </Router>
  );
}

export default App;
