import { useEffect, useState } from 'react'
import { attemptsApi } from '../api/client'
import type { Attempt } from '../types'
import DifficultyBadge from '../components/DifficultyBadge'
import ImportExportModal from '../components/ImportExportModal'
import { format } from 'date-fns'

const STATUS_BADGE: Record<string, string> = {
  SOLVED: 'badge-solved',
  REVIEW: 'badge-review',
  FAILED: 'badge-failed',
}

const TRUNCATE = 80

export default function AttemptsPage() {
  const [attempts, setAttempts] = useState<Attempt[]>([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState('')
  const [search, setSearch] = useState('')
  const [showImportExport, setShowImportExport] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const [expandedId, setExpandedId] = useState<number | null>(null)

  useEffect(() => {
    setLoading(true)
    attemptsApi.list().then(setAttempts).finally(() => setLoading(false))
  }, [refreshKey])

  const filtered = attempts.filter(a => {
    if (statusFilter && a.status !== statusFilter) return false
    if (search && !a.problemTitle.toLowerCase().includes(search.toLowerCase())) return false
    return true
  })

  const toggle = (id: number) => setExpandedId(prev => prev === id ? null : id)

  return (
    <div className="page">
      <div className="page-header flex-between">
        <div>
          <div className="page-title">Attempts</div>
          <div className="page-subtitle">{attempts.length} total attempts logged</div>
        </div>
        <button className="btn-secondary" onClick={() => setShowImportExport(true)}>
          📥 Import / Export
        </button>
      </div>

      <div className="filters">
        <input
          placeholder="Search problems..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ minWidth: 200 }}
        />
        <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
          <option value="">All Statuses</option>
          <option value="SOLVED">Solved</option>
          <option value="REVIEW">Review</option>
          <option value="FAILED">Failed</option>
        </select>
      </div>

      {loading ? (
        <div className="spinner" />
      ) : (
        <>
          <div className="text-muted text-sm mb-12">{filtered.length} attempts</div>
          <div className="card" style={{ padding: 0 }}>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th style={{ width: 24 }} />
                    <th>Problem</th>
                    <th>Difficulty</th>
                    <th>Status</th>
                    <th>Time</th>
                    <th>Date</th>
                    <th>Notes</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(a => {
                    const isExpanded = expandedId === a.id
                    const hasLongNotes = !!a.notes && a.notes.length > TRUNCATE
                    const isExpandable = hasLongNotes

                    return (
                      <>
                        <tr
                          key={a.id}
                          onClick={() => isExpandable && toggle(a.id)}
                          style={{ cursor: isExpandable ? 'pointer' : 'default' }}
                        >
                          {/* chevron */}
                          <td style={{ color: 'var(--text-muted)', fontSize: 10, paddingRight: 4, paddingLeft: 12, width: 20 }}>
                            {isExpandable && (
                              <span style={{
                                display: 'inline-block',
                                transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)',
                                transition: 'transform 0.15s',
                              }}>▶</span>
                            )}
                          </td>
                          <td style={{ whiteSpace: 'nowrap', maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            <span className="text-bold">{a.problemTitle}</span>
                          </td>
                          <td style={{ whiteSpace: 'nowrap' }}>
                            <DifficultyBadge difficulty={a.problemDifficulty} />
                          </td>
                          <td style={{ whiteSpace: 'nowrap' }}>
                            <span className={`badge ${STATUS_BADGE[a.status] ?? ''}`}>
                              {a.status}
                            </span>
                          </td>
                          <td className="text-muted" style={{ whiteSpace: 'nowrap' }}>
                            {a.timeTakenMinutes ? `${a.timeTakenMinutes}m` : '—'}
                          </td>
                          <td className="text-muted text-sm" style={{ whiteSpace: 'nowrap' }}>
                            {format(new Date(a.solvedAt), 'MMM d, yyyy')}
                          </td>
                          <td className="text-muted text-sm" style={{ maxWidth: 280, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            {a.notes
                              ? <>
                                  {a.notes.split('\n')[0]}
                                  {hasLongNotes && !isExpanded && <span style={{ color: 'var(--accent)' }}> … more</span>}
                                </>
                              : '—'}
                          </td>
                        </tr>

                        {/* expanded detail row */}
                        {isExpanded && (
                          <tr key={`${a.id}-expanded`} style={{ background: 'var(--surface2)' }}>
                            <td />
                            <td colSpan={6} style={{ paddingTop: 0, paddingBottom: 14 }}>
                              <div style={{
                                borderLeft: '3px solid var(--accent)',
                                paddingLeft: 12,
                                marginTop: 4,
                              }}>
                                <div className="text-muted text-sm" style={{ marginBottom: 4, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                                  Notes
                                </div>
                                <div className="text-sm" style={{ whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>
                                  {a.notes}
                                </div>
                              </div>
                            </td>
                          </tr>
                        )}
                      </>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {filtered.length === 0 && (
            <div className="empty-state">
              <div className="icon">📝</div>
              <div>No attempts yet. Start solving some problems or import from your spreadsheet!</div>
            </div>
          )}
        </>
      )}

      {showImportExport && (
        <ImportExportModal
          onClose={() => setShowImportExport(false)}
          onImported={() => setRefreshKey(k => k + 1)}
        />
      )}
    </div>
  )
}
