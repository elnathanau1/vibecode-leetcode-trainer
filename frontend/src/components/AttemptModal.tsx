import { useState } from 'react'
import { attemptsApi } from '../api/client'
import type { AttemptStatus, RecommendedProblem } from '../types'

interface Props {
  problem: RecommendedProblem | { id: number; title: string; difficulty: string }
  onClose: () => void
  onSaved: () => void
}

export default function AttemptModal({ problem, onClose, onSaved }: Props) {
  const [status, setStatus] = useState<AttemptStatus>('SOLVED')
  const [timeTaken, setTimeTaken] = useState('')
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      await attemptsApi.create({
        problemId: problem.id,
        status,
        timeTakenMinutes: timeTaken ? parseInt(timeTaken, 10) : undefined,
        notes: notes || undefined,
      })
      onSaved()
      onClose()
    } catch {
      setError('Failed to save attempt. Please try again.')
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-title">Log Attempt</div>
        <div className="text-muted mb-16" style={{ fontSize: 13 }}>{problem.title}</div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Result</label>
            <div className="flex gap-8" style={{ marginTop: 4 }}>
              {(['SOLVED', 'REVIEW', 'FAILED'] as AttemptStatus[]).map(s => (
                <button
                  key={s}
                  type="button"
                  className={`btn-secondary ${status === s ? 'btn-primary' : ''}`}
                  style={{
                    flex: 1,
                    background: status === s
                      ? s === 'SOLVED' ? 'var(--green)' : s === 'REVIEW' ? 'var(--yellow)' : 'var(--red)'
                      : undefined,
                    color: status === s ? '#fff' : undefined,
                    borderColor: status === s ? 'transparent' : undefined,
                  }}
                  onClick={() => setStatus(s)}
                >
                  {s === 'SOLVED' ? '✅ Solved' : s === 'REVIEW' ? '🔄 Review' : '❌ Failed'}
                </button>
              ))}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Time Taken (minutes)</label>
            <input
              type="number"
              min="1"
              max="300"
              placeholder="e.g. 25"
              value={timeTaken}
              onChange={e => setTimeTaken(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Notes (optional)</label>
            <textarea
              rows={3}
              placeholder="Key insights, approaches tried, things to remember..."
              value={notes}
              onChange={e => setNotes(e.target.value)}
              style={{ resize: 'vertical' }}
            />
          </div>

          {error && <div className="text-sm" style={{ color: 'var(--red)', marginBottom: 12 }}>{error}</div>}

          <div className="flex-between" style={{ marginTop: 8 }}>
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Attempt'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
