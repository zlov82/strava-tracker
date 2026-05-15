import { format } from 'date-fns'
import { ru } from 'date-fns/locale'

const EMOJI = { Ride: '🚴', VirtualRide: '🖥️', Swim: '🏊', Run: '🏃', Walk: '🚶' }

function formatTime(sec) {
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}ч ${m}м` : `${m}м`
}

export default function RecentActivitiesList({ data }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
      <h2 className="font-semibold text-gray-700 mb-3">Последние активности</h2>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-gray-400 border-b border-gray-100">
              <th className="pb-2 font-normal">Название</th>
              <th className="pb-2 font-normal">Дата</th>
              <th className="pb-2 font-normal text-right">Км</th>
              <th className="pb-2 font-normal text-right">Время</th>
              <th className="pb-2 font-normal text-right">Скорость</th>
            </tr>
          </thead>
          <tbody>
            {data.map(a => (
              <tr key={a.stravaId} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                <td className="py-2 pr-2 max-w-[180px] truncate">
                  {EMOJI[a.type] || '🏅'} {a.name}
                </td>
                <td className="py-2 text-gray-400 whitespace-nowrap">
                  {format(new Date(a.startDate), 'dd MMM', { locale: ru })}
                </td>
                <td className="py-2 text-right font-medium">{a.distanceKm}</td>
                <td className="py-2 text-right text-gray-500">{formatTime(a.movingTimeSec)}</td>
                <td className="py-2 text-right text-gray-500">
                  {a.averageSpeedKmh ? `${a.averageSpeedKmh} км/ч` : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
