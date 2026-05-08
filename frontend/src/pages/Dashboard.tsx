import { useCallback, useEffect, useState } from 'react'
import { recommendationsApi } from '../api/client'
import type { DailyRecommendation, RecommendedProblem } from '../types'
import DifficultyBadge from '../components/DifficultyBadge'
import AttemptModal from '../components/AttemptModal'

const CATEGORY_LABELS: Record<string, string> = {
  NEW: '🆕 New',
  REVIEW: '🔄 Review',
  SPACED_REPETITION: '🧠 Spaced Rep',
}

const STATUS_BADGE: Record<string, string> = {
  SOLVED: 'badge-solved',
  REVIEW: 'badge-review',
  FAILED: 'badge-failed',
}

export default function Dashboard() {
  const [rec, setRec] = useState<DailyRecommendation | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [targetMinutes, setTargetMinutes] = useState(90)
  const [modalProblem, setModalProblem] = useState<RecommendedProblem | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  const load = useCallback(async (target?: number) => {
    setLoading(true)
    setError('')
    try {
      const data = await recommendationsApi.getToday({ targetMinutes: target ?? targetMinutes })
      setRec(data)
    } catch {
      setError('Could not load recommendations. Make sure the backend is running.')
    } finally {
      setLoading(false)
    }
  }, [targetMinutes])

  useEffect(() => { load() }, [refreshKey])

  const regenerate = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await recommendationsApi.generate({ forceRegenerate: true, targetMinutes })
      setRec(data)
    } catch {
      setError('Failed to regenerate.')
    } finally {
      setLoading(false)
    }
  }

  const today = new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })

  const sessionComplete = rec?.problems.every(p =>
    ['SOLVED', 'FAILED', 'REVIEW'].includes(p.latestStatus ?? '')
  )

  return (
    <div className="page">
      <div className="page-header flex-between">
        <div>
          <div className="page-title">Today's Practice</div>
          <div className="page-subtitle">{today}</div>
        </div>
        <div className="flex gap-8">
          <select
            value={targetMinutes}
            onChange={e => setTargetMinutes(Number(e.target.value))}
            style={{ width: 'auto' }}
          >
            {[30, 45, 60, 90, 120, 150].map(m => (
              <option key={m} value={m}>{m} min</option>
            ))}
          </select>
          <button className="btn-secondary" onClick={regenerate} disabled={loading}>
            🔄 New Session
          </button>
        </div>
      </div>

      {loading && <div className="spinner" />}

      {error && (
        <div className="card" style={{ borderColor: 'var(--red)', color: 'var(--red)' }}>
          {error}
        </div>
      )}

      {!loading && rec && (
        <>
          <div className="time-bar">
            <div>
              <div className="time-bar-value">{rec.totalEstimatedMinutes} min</div>
              <div className="time-bar-label">estimated · target {rec.targetMinutes} min</div>
            </div>
            <div style={{ flex: 1 }}>
              <div className="progress-bar-bg">
                <div
                  className="progress-bar-fill"
                  style={{
                    width: `${Math.min(100, (rec.totalEstimatedMinutes / rec.targetMinutes) * 100)}%`,
                    background: sessionComplete ? 'var(--green)' : undefined,
                  }}
                />
              </div>
            </div>
            {sessionComplete && (
              <div style={{ color: 'var(--green)', fontWeight: 700 }}>✓ Session Complete!</div>
            )}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {rec.problems.map((p, i) => (
              <ProblemCard
                key={p.id}
                problem={p}
                index={i + 1}
                onLog={() => setModalProblem(p)}
              />
            ))}
          </div>

          {rec.problems.length === 0 && (
            <div className="empty-state">
              <div className="icon">🎉</div>
              <div>No problems found. Try adjusting the target time or adding more problems.</div>
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

function ProblemCard({
  problem, index, onLog,
}: { problem: RecommendedProblem; index: number; onLog: () => void }) {
  const statusClass = problem.latestStatus?.toLowerCase() ?? ''
  return (
    <div className={`problem-card ${statusClass}`}>
      <div className="flex-between mb-8">
        <div className="flex-center gap-8">
          <span className="text-muted text-sm" style={{ minWidth: 20 }}>#{index}</span>
          <span className="problem-card-title">
            {problem.url ? (
              <a href={problem.url} target="_blank" rel="noreferrer">{problem.title}</a>
            ) : problem.title}
          </span>
          <DifficultyBadge difficulty={problem.difficulty} />
        </div>
        <div className="flex-center gap-8">
          {problem.category && CATEGORY_LABELS[problem.category] && (
            <span className={`badge ${problem.category === 'NEW' ? 'badge-new' : problem.category === 'REVIEW' ? 'badge-review' : 'badge-spaced'}`}>
              {CATEGORY_LABELS[problem.category]}
            </span>
          )}
          {problem.latestStatus && (
            <span className={`badge ${STATUS_BADGE[problem.latestStatus] ?? ''}`}>
              {problem.latestStatus}
            </span>
          )}
          <span className="text-muted text-sm">~{problem.estimatedMinutes}m</span>
        </div>
      </div>
      <div className="flex-between">
        <div>
          {problem.patterns.slice(0, 4).map(p => <span key={p} className="tag">{p}</span>)}
        </div>
        <button className="btn-secondary btn-sm" onClick={onLog}>
          Log Attempt
        </button>
      </div>
    </div>
  )
}
