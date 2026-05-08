import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import ProblemBrowser from './pages/ProblemBrowser'
import AttemptsPage from './pages/AttemptsPage'
import StatsPage from './pages/StatsPage'

const navItems = [
  { to: '/', label: 'Today', icon: '📅' },
  { to: '/problems', label: 'Problems', icon: '📚' },
  { to: '/attempts', label: 'Attempts', icon: '📝' },
  { to: '/stats', label: 'Stats', icon: '📊' },
]

export default function App() {
  return (
    <BrowserRouter>
      <div className="layout">
        <nav className="sidebar">
          <div className="sidebar-logo">
            <span>🧠</span> LC Trainer
          </div>
          {navItems.map(({ to, label, icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
            >
              <span>{icon}</span> {label}
            </NavLink>
          ))}
        </nav>
        <main className="main">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/problems" element={<ProblemBrowser />} />
            <Route path="/attempts" element={<AttemptsPage />} />
            <Route path="/stats" element={<StatsPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
