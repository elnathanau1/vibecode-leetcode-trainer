import { useEffect, useRef, useState } from 'react'
import CalendarHeatmap from 'react-calendar-heatmap'
import type { ReactCalendarHeatmapValue } from 'react-calendar-heatmap'
import 'react-calendar-heatmap/dist/styles.css'

type HeatmapValue = ReactCalendarHeatmapValue<string> & { count: number; easy: number; medium: number; hard: number }
import { statsApi } from '../api/client'
import type { CategoryProgress, HeatmapEntry, StatsProgress } from '../types'
import { subDays, format, parseISO } from 'date-fns'

interface TooltipState {
  x: number
  y: number
  date: string
  count: number
  easy: number
  medium: number
  hard: number
}

export default function StatsPage() {
  const [heatmap, setHeatmap] = useState<HeatmapEntry[]>([])
  const [progress, setProgress] = useState<StatsProgress | null>(null)
  const [loading, setLoading] = useState(true)
  const [tooltip, setTooltip] = useState<TooltipState | null>(null)
  const wrapperRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    Promise.all([statsApi.heatmap(365), statsApi.progress()])
      .then(([h, p]) => {
        setHeatmap(h)
        setProgress(p)
      })
      .finally(() => setLoading(false))
  }, [])

  const today = new Date()
  const startDate = subDays(today, 364)

  const heatmapValues = heatmap.map(e => ({ date: e.date, count: e.count, easy: e.easy, medium: e.medium, hard: e.hard }))

  const maxCount = Math.max(...heatmap.map(e => e.count), 1)

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const getColor = (value: any) => {
    if (!value || !(value as HeatmapValue).count) return 'color-empty'
    const ratio = (value as HeatmapValue).count / maxCount
    if (ratio < 0.25) return 'color-scale-1'
    if (ratio < 0.5) return 'color-scale-2'
    if (ratio < 0.75) return 'color-scale-3'
    return 'color-scale-4'
  }

  const totalSolved = heatmap.reduce((sum, e) => sum + e.count, 0)
  const streak = computeStreak(heatmap)
  const activeDays = heatmap.filter(e => e.count > 0).length

  if (loading) return <div className="page"><div className="spinner" /></div>

  return (
    <div className="page">
      {tooltip && (
        <div style={{
          position: 'fixed',
          left: tooltip.x + 14,
          top: tooltip.y - 10,
          background: 'var(--surface2)',
          border: '1px solid var(--border)',
          borderRadius: 8,
          padding: '8px 12px',
          fontSize: 12,
          pointerEvents: 'none',
          zIndex: 1000,
          minWidth: 140,
          boxShadow: '0 4px 16px rgba(0,0,0,0.4)',
        }}>
          <div style={{ fontWeight: 700, marginBottom: 6 }}>
            {format(parseISO(tooltip.date), 'EEEE, MMM d, yyyy')}
          </div>
          {tooltip.count === 0 ? (
            <div style={{ color: 'var(--text-muted)' }}>No submissions</div>
          ) : (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
                <span style={{ color: 'var(--green)' }}>Easy</span>
                <span style={{ fontWeight: 600 }}>{tooltip.easy}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
                <span style={{ color: 'var(--yellow)' }}>Medium</span>
                <span style={{ fontWeight: 600 }}>{tooltip.medium}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
                <span style={{ color: 'var(--red)' }}>Hard</span>
                <span style={{ fontWeight: 600 }}>{tooltip.hard}</span>
              </div>
              <div style={{ borderTop: '1px solid var(--border)', marginTop: 6, paddingTop: 6, display: 'flex', justifyContent: 'space-between', gap: 16 }}>
                <span style={{ color: 'var(--text-muted)' }}>Total</span>
                <span style={{ fontWeight: 700 }}>{tooltip.count}</span>
              </div>
            </>
          )}
        </div>
      )}
      <div className="page-header">
        <div className="page-title">Stats & Progress</div>
        <div className="page-subtitle">Your LeetCode journey at a glance</div>
      </div>

      <div className="grid-3 mb-16" style={{ marginBottom: 24 }}>
        <div className="stat-card">
          <div className="stat-card-value">{progress?.solvedProblems ?? 0}</div>
          <div className="stat-card-label">Problems Solved</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-value">{streak}</div>
          <div className="stat-card-label">Day Streak 🔥</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-value">{activeDays}</div>
          <div className="stat-card-label">Active Days (year)</div>
        </div>
      </div>

      <div className="card mb-16" style={{ marginBottom: 24 }}>
        <div className="text-bold mb-12" style={{ marginBottom: 12 }}>Activity — Past Year</div>
        <div
          ref={wrapperRef}
          style={{ overflowX: 'auto', position: 'relative' }}
          onMouseOver={e => {
            const el = e.target as Element
            const date = el.getAttribute('data-date')
            if (!date) return
            setTooltip({
              x: e.clientX,
              y: e.clientY,
              date,
              count:  Number(el.getAttribute('data-count')),
              easy:   Number(el.getAttribute('data-easy')),
              medium: Number(el.getAttribute('data-medium')),
              hard:   Number(el.getAttribute('data-hard')),
            })
          }}
          onMouseMove={e => {
            if (tooltip) setTooltip(t => t ? { ...t, x: e.clientX, y: e.clientY } : null)
          }}
          onMouseOut={e => {
            if ((e.target as Element).getAttribute('data-date')) setTooltip(null)
          }}
        >
          <CalendarHeatmap
            startDate={startDate}
            endDate={today}
            values={heatmapValues}
            classForValue={getColor}
            showWeekdayLabels={false}
            tooltipDataAttrs={(value: any) => ({
              'data-date':   value?.date ?? '',
              'data-count':  value?.count ?? 0,
              'data-easy':   value?.easy ?? 0,
              'data-medium': value?.medium ?? 0,
              'data-hard':   value?.hard ?? 0,
            } as any)}
          />
        </div>
        <div className="flex-center gap-8 mt-8" style={{ marginTop: 8, fontSize: 11, color: 'var(--text-muted)' }}>
          <span>Less</span>
          <span style={{ display:'inline-block',width:10,height:10,background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:2 }}/>
          <span style={{ display:'inline-block',width:10,height:10,background:'#0e4429',borderRadius:2 }}/>
          <span style={{ display:'inline-block',width:10,height:10,background:'#006d32',borderRadius:2 }}/>
          <span style={{ display:'inline-block',width:10,height:10,background:'#26a641',borderRadius:2 }}/>
          <span style={{ display:'inline-block',width:10,height:10,background:'#39d353',borderRadius:2 }}/>
          <span>More</span>
        </div>
      </div>

      <div className="card">
        <div className="text-bold mb-12" style={{ marginBottom: 16 }}>Progress by Pattern</div>
        {progress?.categories.length === 0 && (
          <div className="text-muted text-sm">No attempts logged yet.</div>
        )}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {progress?.categories.map(cat => (
            <CategoryRow key={cat.pattern} cat={cat} />
          ))}
        </div>
      </div>
    </div>
  )
}

function CategoryRow({ cat }: { cat: CategoryProgress }) {
  return (
    <div>
      <div className="flex-between mb-8" style={{ marginBottom: 4 }}>
        <span style={{ fontSize: 13 }}>{cat.pattern}</span>
        <span className="text-muted text-sm">{cat.solved}/{cat.total} ({cat.percentage}%)</span>
      </div>
      <div className="progress-bar-bg">
        <div
          className="progress-bar-fill"
          style={{
            width: `${cat.percentage}%`,
            background: cat.percentage === 100 ? 'var(--green)' : cat.percentage > 50 ? 'var(--accent)' : 'var(--yellow)',
          }}
        />
      </div>
    </div>
  )
}

function computeStreak(entries: HeatmapEntry[]): number {
  const dateSet = new Set(entries.filter(e => e.count > 0).map(e => e.date))
  let streak = 0
  let day = new Date()
  while (true) {
    const dateStr = format(day, 'yyyy-MM-dd')
    if (!dateSet.has(dateStr)) break
    streak++
    day = subDays(day, 1)
  }
  return streak
}
