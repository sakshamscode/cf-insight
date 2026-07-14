import { useState, useEffect } from 'react'
import './App.css'

function App() {
  const [handleInput, setHandleInput] = useState('')
  const [stats, setStats] = useState(null)
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const API_BASE = 'http://localhost:8080/api/cf'

  useEffect(() => {
    fetchHistory()
  }, [])

  const fetchHistory = async () => {
    try {
      const response = await fetch(`${API_BASE}/history`)
      if (response.ok) {
        const data = await response.json()
        setHistory(data)
      }
    } catch (err) {
      console.error('Error fetching search history:', err)
    }
  }

  const handleSearch = async (searchHandle) => {
    if (!searchHandle || !searchHandle.trim()) return

    setLoading(true)
    setError('')
    try {
      const response = await fetch(`${API_BASE}/stats/${searchHandle.trim()}`)
      if (!response.ok) {
        if (response.status === 404) {
          throw new Error(`Codeforces handle "${searchHandle}" not found.`)
        } else if (response.status === 502) {
          throw new Error('Codeforces API is currently rate-limiting or down. Please try again in a few seconds.')
        } else {
          throw new Error('Something went wrong. Please check your network and try again.')
        }
      }
      const data = await response.json()
      setStats(data)
      setHandleInput('')
      fetchHistory()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const getRankColor = (rank) => {
    if (!rank) return '#9ca3af' // default gray
    const r = rank.toLowerCase()
    if (r.includes('newbie')) return '#808080' // gray
    if (r.includes('pupil')) return '#008000' // green
    if (r.includes('specialist')) return '#03a89e' // cyan
    if (r.includes('expert')) return '#0000ff' // blue
    if (r.includes('candidate master')) return '#aa00aa' // violet
    if (r.includes('master')) return '#ff8c00' // orange
    if (r.includes('grandmaster')) return '#ff0000' // red
    return '#ff0000' // default red
  }

  const getRatingColor = (rating) => {
    if (rating < 1200) return '#808080' // gray
    if (rating < 1400) return '#008000' // green
    if (rating < 1600) return '#03a89e' // cyan
    if (rating < 1900) return '#0000ff' // blue
    if (rating < 2100) return '#aa00aa' // violet
    if (rating < 2400) return '#ff8c00' // orange
    return '#ff0000' // red (Grandmaster+)
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    try {
      const date = new Date(dateStr)
      return date.toLocaleString()
    } catch (e) {
      return dateStr
    }
  }

  // Pre-configured list of popular CF handles to recommend
  const recommendedHandles = ['Tourist', 'Benq', 'Radewoosh', 'ecnerwala', 'mike_mirzayanov']

  return (
    <div className="app-container">
      {/* Header Banner */}
      <header className="app-header">
        <div className="logo-container">
          <span className="logo-box logo-cf-red">C</span>
          <span className="logo-box logo-cf-blue">F</span>
          <h1 className="logo-text">INSIGHT</h1>
        </div>
        <p className="subtitle">Visualizing Codeforces Performance, Solved Tags, and Difficulty Levels</p>
      </header>

      {/* Search Console */}
      <section className="search-section">
        <form
          className="search-form"
          onSubmit={(e) => {
            e.preventDefault()
            handleSearch(handleInput)
          }}
        >
          <div className="input-group">
            <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input
              type="text"
              placeholder="Paste Codeforces handle (e.g. Tourist)..."
              value={handleInput}
              onChange={(e) => setHandleInput(e.target.value)}
              disabled={loading}
              className="search-input"
            />
          </div>
          <button type="submit" className="search-button" disabled={loading}>
            {loading ? 'Searching...' : 'Analyze'}
          </button>
        </form>

        <div className="quick-links">
          <span className="quick-label">Try:</span>
          {recommendedHandles.map((h) => (
            <button
              key={h}
              onClick={() => handleSearch(h)}
              disabled={loading}
              className="quick-link-btn"
            >
              {h}
            </button>
          ))}
        </div>
      </section>

      {/* Loading Skeleton */}
      {loading && (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Analyzing Codeforces submissions... Please wait.</p>
        </div>
      )}

      {/* Error Alert */}
      {error && !loading && (
        <div className="error-alert">
          <svg className="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span className="error-message">{error}</span>
        </div>
      )}

      {/* Main Dashboard Layout */}
      {!loading && stats && (
        <div className="dashboard-grid">
          {/* Left Column: User Summary & Search History */}
          <div className="column-left">
            {/* User Profile Card */}
            <div className="dashboard-card profile-card">
              <div className="profile-header">
                <img
                  src={stats.avatar || 'https://userpic.codeforces.org/no-avatar.jpg'}
                  alt={stats.handle}
                  className="profile-avatar"
                  onError={(e) => {
                    e.target.onerror = null
                    e.target.src = 'https://userpic.codeforces.org/no-avatar.jpg'
                  }}
                />
                <div className="profile-title">
                  <h2 className="profile-handle" style={{ color: getRankColor(stats.rank) }}>{stats.handle}</h2>
                  <span className="profile-rank" style={{ color: getRankColor(stats.rank), borderColor: getRankColor(stats.rank) }}>
                    {stats.rank || 'Unrated'}
                  </span>
                </div>
              </div>

              <hr className="divider" />

              <div className="profile-metrics">
                <div className="metric-box">
                  <span className="metric-value">{stats.totalSolved}</span>
                  <span className="metric-label">Problems Solved</span>
                </div>
                {Object.keys(stats.difficultyStats).length > 0 && (
                  <div className="api-discrepancy-note">
                    * Showing {Object.values(stats.difficultyStats).reduce((a, b) => a + b, 0)} rated problems in analytics below.
                  </div>
                )}
              </div>

              <div className="profile-footer">
                <span>Last updated: {formatDate(stats.lastUpdated)}</span>
              </div>
            </div>

            {/* History Panel */}
            {history.length > 0 && (
              <div className="dashboard-card history-card">
                <h3 className="card-title">Recent Searches</h3>
                <div className="history-list">
                  {history.map((h) => (
                    <button
                      key={h.handle}
                      onClick={() => handleSearch(h.handle)}
                      disabled={loading}
                      className="history-item-btn"
                    >
                      <img src={h.avatar} alt="" className="history-avatar" />
                      <div className="history-item-info">
                        <span className="history-handle" style={{ color: getRankColor(h.rank) }}>{h.handle}</span>
                        <span className="history-count">{h.totalSolved} solved</span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Right Column: Statistics Charts */}
          <div className="column-right">
            {/* Difficulty Chart Card */}
            <div className="dashboard-card chart-card">
              <h3 className="card-title">Problems Solved by Difficulty</h3>
              <p className="card-subtitle">Aggregated by Codeforces problem difficulty rating (800 - 3500+)</p>
              
              {Object.keys(stats.difficultyStats).length === 0 ? (
                <div className="no-data">No rated problems solved yet.</div>
              ) : (
                <div className="difficulty-chart-container">
                  <div className="difficulty-chart">
                    {Object.entries(stats.difficultyStats).map(([rating, count]) => {
                      const maxDifficultyCount = Math.max(...Object.values(stats.difficultyStats), 1);
                      const heightPercent = (count / maxDifficultyCount) * 80 + 10; // Keep minimum height for visual alignment
                      return (
                        <div key={rating} className="difficulty-bar-container">
                          <div className="difficulty-bar-wrapper">
                            <div
                              className="difficulty-bar"
                              style={{
                                height: `${heightPercent}%`,
                                backgroundColor: getRatingColor(parseInt(rating))
                              }}
                            >
                              <span className="bar-count-label">{count}</span>
                            </div>
                          </div>
                          <span className="difficulty-bar-label">{rating}</span>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )}
            </div>

            {/* Tags Chart Card */}
            <div className="dashboard-card chart-card">
              <h3 className="card-title">Problems Solved by Tag</h3>
              <p className="card-subtitle">Grouping of unique solved problems by topic tags (e.g. dp, math)</p>

              {Object.keys(stats.tagStats).length === 0 ? (
                <div className="no-data">No tags statistics available.</div>
              ) : (
                <div className="tag-list">
                  {Object.entries(stats.tagStats)
                    .sort((a, b) => b[1] - a[1])
                    .map(([tag, count]) => {
                      const maxTagCount = Math.max(...Object.values(stats.tagStats), 1);
                      const widthPercent = (count / maxTagCount) * 100;
                      return (
                        <div key={tag} className="tag-row">
                          <div className="tag-info">
                            <span className="tag-name">{tag}</span>
                            <span className="tag-count">{count} problems</span>
                          </div>
                          <div className="tag-progress-bar-bg">
                            <div
                              className="tag-progress-bar-fill"
                              style={{ width: `${widthPercent}%` }}
                            ></div>
                          </div>
                        </div>
                      )
                    })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Welcome Display (when no user is selected yet) */}
      {!stats && !loading && !error && (
        <section className="welcome-section">
          <div className="welcome-card">
            <h2>Track Codeforces Solved Stats Instantly</h2>
            <p>
              Official Codeforces displays user submissions, but lacks aggregated insights on 
              <strong> solved problem topics (tags)</strong> and <strong>difficulty ratings distribution</strong>.
            </p>
            <p>
              Paste any Codeforces handle above to immediately generate interactive visual stats, 
              automatically cached in our system to bypass API rate limiting.
            </p>
          </div>

          {history.length > 0 && (
            <div className="welcome-history">
              <h3>Recently Checked Handles</h3>
              <div className="welcome-history-grid">
                {history.slice(0, 6).map((h) => (
                  <button
                    key={h.handle}
                    onClick={() => handleSearch(h.handle)}
                    className="welcome-history-card"
                  >
                    <img src={h.avatar} alt="" className="welcome-history-avatar" />
                    <span className="welcome-history-handle" style={{ color: getRankColor(h.rank) }}>{h.handle}</span>
                    <span className="welcome-history-rank">{h.rank || 'Unrated'}</span>
                    <span className="welcome-history-count">{h.totalSolved} Solved</span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </section>
      )}

      <footer className="app-footer-bar">
        <p>CF Insight &copy; 2026. Made for Competitive Programmers.</p>
      </footer>
    </div>
  )
}

export default App
