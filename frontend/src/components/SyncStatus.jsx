import { formatDistanceToNow } from 'date-fns'
import { ru } from 'date-fns/locale'

const STATUS_COLOR = {
  IDLE: 'text-green-500',
  RUNNING: 'text-blue-500',
  ERROR: 'text-red-500',
  NEVER: 'text-gray-400',
}

const STATUS_LABEL = {
  IDLE: 'синхронизировано',
  RUNNING: 'синхронизация...',
  ERROR: 'ошибка',
  NEVER: 'никогда',
}

export default function SyncStatus({ sync }) {
  if (!sync) return null

  const color = STATUS_COLOR[sync.status] || 'text-gray-400'
  const label = STATUS_LABEL[sync.status] || sync.status

  const ago = sync.lastSyncAt
    ? formatDistanceToNow(new Date(sync.lastSyncAt), { addSuffix: true, locale: ru })
    : null

  return (
    <div className="flex items-center gap-2 text-sm text-gray-400">
      <span className={`font-medium ${color}`}>{label}</span>
      {ago && <span>· {ago}</span>}
      {sync.error && <span className="text-red-400 truncate max-w-xs" title={sync.error}>· {sync.error}</span>}
    </div>
  )
}
