import { useRef, useState } from 'react'
import { spreadsheetApi, type ImportResult } from '../api/client'

interface Props {
  onClose: () => void
  onImported: () => void
}

type Tab = 'import' | 'export'
type ImportMode = 'file' | 'url'

export default function ImportExportModal({ onClose, onImported }: Props) {
  const [tab, setTab] = useState<Tab>('import')
  const [importMode, setImportMode] = useState<ImportMode>('file')
  const [sheetUrl, setSheetUrl] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<ImportResult | null>(null)
  const [error, setError] = useState('')
  const [dragging, setDragging] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  const handleFileSelect = (file: File) => {
    if (!file.name.endsWith('.csv')) {
      setError('Please select a .csv file')
      return
    }
    setSelectedFile(file)
    setError('')
    setResult(null)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFileSelect(file)
  }

  const handleImport = async () => {
    setLoading(true)
    setError('')
    setResult(null)
    try {
      let res: ImportResult
      if (importMode === 'file') {
        if (!selectedFile) { setError('Select a CSV file first'); setLoading(false); return }
        res = await spreadsheetApi.importFile(selectedFile)
      } else {
        if (!sheetUrl.trim()) { setError('Enter a Google Sheet URL'); setLoading(false); return }
        res = await spreadsheetApi.importUrl(sheetUrl.trim())
      }
      setResult(res)
      if (res.imported > 0) onImported()
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } }; message?: string })
        ?.response?.data?.message ?? (e as { message?: string })?.message ?? 'Import failed'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  const handleExport = () => {
    window.open(spreadsheetApi.exportUrl, '_blank')
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 520 }} onClick={e => e.stopPropagation()}>
        {/* Tabs */}
        <div className="flex" style={{ borderBottom: '1px solid var(--border)', marginBottom: 20, gap: 0 }}>
          {(['import', 'export'] as Tab[]).map(t => (
            <button
              key={t}
              onClick={() => { setTab(t); setResult(null); setError('') }}
              style={{
                background: 'none',
                border: 'none',
                borderBottom: tab === t ? '2px solid var(--accent)' : '2px solid transparent',
                borderRadius: 0,
                padding: '8px 20px',
                color: tab === t ? 'var(--accent)' : 'var(--text-muted)',
                fontWeight: tab === t ? 600 : 400,
                fontSize: 14,
                cursor: 'pointer',
                marginBottom: -1,
              }}
            >
              {t === 'import' ? '📥 Import' : '📤 Export'}
            </button>
          ))}
        </div>

        {tab === 'import' && (
          <>
            <div className="modal-title">Import Attempts</div>
            <p className="text-muted text-sm mb-16" style={{ marginBottom: 16 }}>
              Import from your Google Sheet or a downloaded CSV. Expects columns:{' '}
              <code style={{ background: 'var(--surface2)', padding: '1px 4px', borderRadius: 3 }}>
                Problem, Date, Time, Level, Description, Solution, Tags
              </code>
            </p>

            {/* Mode toggle */}
            <div className="flex gap-8 mb-16" style={{ marginBottom: 16 }}>
              {(['file', 'url'] as ImportMode[]).map(m => (
                <button
                  key={m}
                  className={importMode === m ? 'btn-primary btn-sm' : 'btn-secondary btn-sm'}
                  onClick={() => { setImportMode(m); setResult(null); setError('') }}
                >
                  {m === 'file' ? '📁 Upload CSV' : '🔗 Google Sheet URL'}
                </button>
              ))}
            </div>

            {importMode === 'file' ? (
              <>
                <div
                  onDragOver={e => { e.preventDefault(); setDragging(true) }}
                  onDragLeave={() => setDragging(false)}
                  onDrop={handleDrop}
                  onClick={() => fileRef.current?.click()}
                  style={{
                    border: `2px dashed ${dragging ? 'var(--accent)' : 'var(--border)'}`,
                    borderRadius: 'var(--radius)',
                    padding: '28px',
                    textAlign: 'center',
                    cursor: 'pointer',
                    background: dragging ? 'rgba(88,166,255,0.05)' : 'var(--surface2)',
                    transition: 'all 0.15s',
                    marginBottom: 12,
                  }}
                >
                  <div style={{ fontSize: 28, marginBottom: 8 }}>📄</div>
                  {selectedFile ? (
                    <div>
                      <div style={{ fontWeight: 600 }}>{selectedFile.name}</div>
                      <div className="text-muted text-sm">
                        {(selectedFile.size / 1024).toFixed(1)} KB — click to change
                      </div>
                    </div>
                  ) : (
                    <div className="text-muted">
                      Drag & drop a CSV, or <span style={{ color: 'var(--accent)' }}>click to browse</span>
                      <div className="text-sm" style={{ marginTop: 4 }}>
                        Download from Google Sheets: File → Download → CSV
                      </div>
                    </div>
                  )}
                  <input
                    ref={fileRef}
                    type="file"
                    accept=".csv"
                    style={{ display: 'none' }}
                    onChange={e => { if (e.target.files?.[0]) handleFileSelect(e.target.files[0]) }}
                  />
                </div>
              </>
            ) : (
              <div className="form-group" style={{ marginBottom: 12 }}>
                <label className="form-label">Google Sheet URL</label>
                <input
                  type="url"
                  placeholder="https://docs.google.com/spreadsheets/d/..."
                  value={sheetUrl}
                  onChange={e => setSheetUrl(e.target.value)}
                />
                <div className="text-muted text-sm" style={{ marginTop: 4 }}>
                  The sheet must be set to "Anyone with the link can view"
                </div>
              </div>
            )}

            {error && (
              <div className="text-sm" style={{ color: 'var(--red)', marginBottom: 12 }}>{error}</div>
            )}

            {result && <ImportResultBanner result={result} />}

            <div className="flex-between" style={{ marginTop: 16 }}>
              <button className="btn-secondary" onClick={onClose}>Close</button>
              <button
                className="btn-primary"
                onClick={handleImport}
                disabled={loading || (importMode === 'file' && !selectedFile)}
              >
                {loading ? 'Importing…' : '📥 Import'}
              </button>
            </div>
          </>
        )}

        {tab === 'export' && (
          <>
            <div className="modal-title">Export Attempts</div>
            <p className="text-muted text-sm" style={{ marginBottom: 20 }}>
              Download all your logged attempts as a CSV file. You can open it in Google Sheets,
              Excel, or any spreadsheet app.
            </p>

            <div className="card" style={{ marginBottom: 20 }}>
              <div className="text-bold mb-8" style={{ marginBottom: 8 }}>Export format</div>
              <div className="text-muted text-sm">
                Columns: <strong>Problem</strong>, <strong>Date</strong>,{' '}
                <strong>Time (minutes)</strong>, <strong>Difficulty</strong>,{' '}
                <strong>Status</strong>, <strong>Notes</strong>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <button className="btn-primary" onClick={handleExport}>
                ⬇️ Download CSV
              </button>
              <button
                className="btn-secondary"
                onClick={() => {
                  navigator.clipboard.writeText(
                    window.location.origin + spreadsheetApi.exportUrl
                  )
                }}
              >
                📋 Copy Export URL
              </button>
            </div>

            <div className="text-muted text-sm" style={{ marginTop: 16, lineHeight: 1.7 }}>
              <strong>To import into Google Sheets:</strong><br />
              1. Download the CSV above<br />
              2. In Google Sheets, go to File → Import<br />
              3. Upload the CSV and choose "Replace spreadsheet" or "Insert new sheet"
            </div>

            <div className="flex-between" style={{ marginTop: 20 }}>
              <button className="btn-secondary" onClick={onClose}>Close</button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function ImportResultBanner({ result }: { result: ImportResult }) {
  const success = result.imported > 0
  return (
    <div style={{
      background: success ? 'rgba(63,185,80,0.1)' : 'rgba(248,81,73,0.1)',
      border: `1px solid ${success ? 'var(--green)' : 'var(--red)'}`,
      borderRadius: 'var(--radius)',
      padding: '12px 14px',
      marginBottom: 12,
    }}>
      <div style={{ fontWeight: 600, marginBottom: 6, color: success ? 'var(--green)' : 'var(--red)' }}>
        {success ? '✅ Import complete' : '⚠️ Import finished with issues'}
      </div>
      <div className="text-sm" style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        {result.imported > 0 && <span>✓ {result.imported} attempt{result.imported !== 1 ? 's' : ''} imported</span>}
        {result.duplicate > 0 && <span className="text-muted">⏭ {result.duplicate} duplicate{result.duplicate !== 1 ? 's' : ''} skipped</span>}
        {result.skipped > 0 && <span className="text-muted">⏭ {result.skipped} row{result.skipped !== 1 ? 's' : ''} skipped (problem not found)</span>}
        {result.notFound.length > 0 && (
          <details style={{ marginTop: 4 }}>
            <summary className="text-muted" style={{ cursor: 'pointer' }}>
              {result.notFound.length} problem{result.notFound.length !== 1 ? 's' : ''} not matched in DB
            </summary>
            <div style={{ marginTop: 6, paddingLeft: 12, color: 'var(--text-muted)' }}>
              {result.notFound.map(t => <div key={t}>• {t}</div>)}
            </div>
          </details>
        )}
        {result.errors.length > 0 && (
          <details style={{ marginTop: 4 }}>
            <summary style={{ cursor: 'pointer', color: 'var(--red)' }}>
              {result.errors.length} parse error{result.errors.length !== 1 ? 's' : ''}
            </summary>
            <div style={{ marginTop: 6, paddingLeft: 12, color: 'var(--red)' }}>
              {result.errors.map((e, i) => <div key={i}>• {e}</div>)}
            </div>
          </details>
        )}
      </div>
    </div>
  )
}
