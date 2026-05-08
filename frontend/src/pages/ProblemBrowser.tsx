import { useEffect, useState } from 'react'
import { problemsApi } from '../api/client'
import type { Problem } from '../types'
import DifficultyBadge from '../components/DifficultyBadge'
import AttemptModal from '../components/AttemptModal'

const STATUS_BADGE: Record<string, string> = {
  SOLVED: 'badge-solved',
  REVIEW: 'badge-review',
  FAILED: 'badge-failed',
}

export default function ProblemBrowser() {
  const [problems, setProblems] = useState<Problem[]>([])
  const [patterns, setPatterns] = useState<string[]>([])
  const [companies, setCompanies] = useState<string[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const [pattern, setPattern] = useState('')
  const [company, setCompany] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [modalProblem, setModalProblem] = useState<Problem | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    Promise.all([
      problemsApi.getPatterns(),
      problemsApi.getCompanies(),
    ]).then(([p, c]) => {
      setPatterns(p)
      setCompanies(c)
    })
  }, [])

  useEffect(() => {
    setLoading(true)
    problemsApi.list({ difficulty: difficulty || undefined, pattern: pattern || undefined, company: company || undefined })
      .then(setProblems)
      .finally(() => setLoading(false))
  }, [difficulty, pattern, company, refreshKey])

  const filtered = problems.filter(p => {
    if (search && !p.title.toLowerCase().includes(search.toLowerCase())) return false
    if (statusFilter) {
      if (statusFilter === 'NEW' && p.latestStatus) return false
      if (statusFilter !== 'NEW' && p.latestStatus !== statusFilter) return false
    }
    return true
  })

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-title">Problems</div>
        <div className="page-subtitle">{problems.length} problems in set</div>
      </div>

      <div className="filters">
        <input
          placeholder="Search problems..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ minWidth: 200 }}
        />
        <select value={difficulty} onChange={e => setDifficulty(e.target.value)}>
          <option value="">All Difficulties</option>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </select>
        <select value={pattern} onChange={e => setPattern(e.target.value)}>
          <option value="">All Patterns</option>
          {patterns.map(p => <option key={p} value={p}>{p}</option>)}
        </select>
        <select value={company} onChange={e => setCompany(e.target.value)}>
          <option value="">All Companies</option>
          {companies.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
        <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
          <option value="">All Statuses</option>
          <option value="NEW">New</option>
          <option value="SOLVED">Solved</option>
          <option value="REVIEW">Review</option>
          <option value="FAILED">Failed</option>
        </select>
      </div>

      {loading ? (
        <div className="spinner" />
      ) : (
        <>
          <div className="text-muted text-sm mb-12">{filtered.length} problems</div>
          <div className="card" style={{ padding: 0 }}>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Problem</th>
                    <th>Difficulty</th>
                    <th>Patterns</th>
                    <th>Est. Time</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(p => (
                    <tr key={p.id}>
                      <td>
                        {p.url ? (
                          <a href={p.url} target="_blank" rel="noreferrer" className="text-bold">
                            {p.title}
                          </a>
                        ) : (
                          <span className="text-bold">{p.title}</span>
                        )}
                        {p.isPremium && <span className="tag" style={{ marginLeft: 6 }}>💎</span>}
                      </td>
                      <td><DifficultyBadge difficulty={p.difficulty} /></td>
                      <td>
                        {p.patterns.slice(0, 3).map(pat => (
                          <span key={pat} className="tag">{pat}</span>
                        ))}
                        {p.patterns.length > 3 && (
                          <span className="text-muted text-sm"> +{p.patterns.length - 3}</span>
                        )}
                      </td>
                      <td className="text-muted">{p.estimatedMinutes}m</td>
                      <td>
                        {p.latestStatus ? (
                          <span className={`badge ${STATUS_BADGE[p.latestStatus] ?? ''}`}>
                            {p.latestStatus}
                          </span>
                        ) : (
                          <span className="text-muted text-sm">New</span>
                        )}
                      </td>
                      <td>
                        <button className="btn-secondary btn-sm" onClick={() => setModalProblem(p)}>
                          Log
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          {filtered.length === 0 && (
            <div className="empty-state">
              <div className="icon">🔍</div>
              <div>No problems match your filters.</div>
            </div>
          )}
        </>
      )}

      {modalProblem && (
        <AttemptModal
          problem={modalProblem}
          onClose={() => setModalProblem(null)}
          onSaved={() => setRefreshKey(k => k + 1)}
        />
      )}
    </div>
  )
}
