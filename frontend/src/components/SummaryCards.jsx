export default function SummaryCards({ summary }) {
  if (!summary) return null

  const cards = [
    { label: 'Дистанция', value: `${Math.round(summary.totalDistanceKm).toLocaleString('ru')} км` },
    { label: 'Время', value: `${Math.round(summary.totalTimeHours)} ч` },
    { label: 'Набор высоты', value: `${(summary.totalElevationM / 1000).toFixed(1)} км` },
    { label: 'Активности', value: summary.totalActivities },
  ]

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      {cards.map(c => (
        <div key={c.label} className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
          <div className="text-3xl font-bold text-orange-500">{c.value}</div>
          <div className="text-sm text-gray-400 mt-1">{c.label}</div>
        </div>
      ))}
    </div>
  )
}
