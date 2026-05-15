import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import { parse, format } from 'date-fns'
import { ru } from 'date-fns/locale'

const TYPES = [
  { value: '', label: 'Все' },
  { value: 'Ride', label: 'Велосипед' },
  { value: 'VirtualRide', label: 'Виртуальный' },
  { value: 'Swim', label: 'Плавание' },
  { value: 'Run', label: 'Бег' },
  { value: 'Walk', label: 'Ходьба' },
]

const WEEKS_OPTIONS = [
  { value: 4, label: '4 недели' },
  { value: 8, label: '8 недель' },
  { value: 12, label: '12 недель' },
  { value: 26, label: '6 месяцев' },
  { value: 52, label: '1 год' },
]

function parseWeekStart(isoWeek) {
  try {
    const [year, week] = isoWeek.split('-W')
    return parse(`${year}-W${week}-1`, "RRRR-'W'II-i", new Date())
  } catch {
    return null
  }
}

function formatWeekLabel(isoWeek) {
  const d = parseWeekStart(isoWeek)
  return d ? format(d, 'd MMM', { locale: ru }) : isoWeek
}

function formatWeekTooltip(isoWeek) {
  const d = parseWeekStart(isoWeek)
  return d ? `Неделя с ${format(d, 'd MMM yyyy', { locale: ru })}` : isoWeek
}

export default function WeeklyVolumeChart({ data, type, onTypeChange, weeks, onWeeksChange }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
      <div className="flex items-center justify-between mb-1">
        <h2 className="font-semibold text-gray-700">Объём по неделям</h2>
        <div className="flex gap-2">
          <select
            value={weeks}
            onChange={e => onWeeksChange(Number(e.target.value))}
            className="text-sm border border-gray-200 rounded-lg px-2 py-1 focus:outline-none"
          >
            {WEEKS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
          <select
            value={type}
            onChange={e => onTypeChange(e.target.value)}
            className="text-sm border border-gray-200 rounded-lg px-2 py-1 focus:outline-none"
          >
            {TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
          </select>
        </div>
      </div>
      <ResponsiveContainer width="100%" height={220}>
        <BarChart data={data} margin={{ top: 4, right: 4, left: 0, bottom: 0 }}>
          <XAxis dataKey="week" tick={{ fontSize: 10 }} tickFormatter={formatWeekLabel} />
          <YAxis unit=" км" tick={{ fontSize: 10 }} width={48} />
          <Tooltip
            formatter={v => [`${v} км`, 'Дистанция']}
            labelFormatter={formatWeekTooltip}
          />
          <Bar dataKey="km" fill="#f97316" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
