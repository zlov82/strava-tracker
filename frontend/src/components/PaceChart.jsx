import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'

const TYPES = [
  { value: 'Ride', label: 'Велосипед' },
  { value: 'VirtualRide', label: 'Виртуальный' },
  { value: 'Swim', label: 'Плавание' },
  { value: 'Run', label: 'Бег' },
  { value: 'Walk', label: 'Ходьба' },
]

const LIMIT_OPTIONS = [
  { value: 20, label: '20 активностей' },
  { value: 30, label: '30 активностей' },
  { value: 50, label: '50 активностей' },
  { value: 100, label: '100 активностей' },
]

export default function PaceChart({ data, type, onTypeChange, limit, onLimitChange }) {
  const reversed = [...data].reverse()

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
      <div className="flex items-center justify-between mb-1">
        <h2 className="font-semibold text-gray-700">Динамика скорости</h2>
        <div className="flex gap-2">
          <select
            value={limit}
            onChange={e => onLimitChange(Number(e.target.value))}
            className="text-sm border border-gray-200 rounded-lg px-2 py-1 focus:outline-none"
          >
            {LIMIT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
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
        <LineChart data={reversed} margin={{ top: 4, right: 4, left: 0, bottom: 0 }}>
          <XAxis dataKey="date" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
          <YAxis unit=" км/ч" tick={{ fontSize: 10 }} width={52} domain={['auto', 'auto']} />
          <Tooltip
            formatter={(v, _n, p) => [`${v} км/ч — ${p.payload.name}`, '']}
            labelFormatter={() => ''}
          />
          <Line
            type="monotone"
            dataKey="speedKmh"
            stroke="#f97316"
            dot={false}
            strokeWidth={2}
            activeDot={{ r: 4 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
