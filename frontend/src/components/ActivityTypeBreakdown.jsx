import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts'

const COLORS = ['#f97316', '#3b82f6', '#10b981', '#8b5cf6', '#ef4444']

export default function ActivityTypeBreakdown({ data }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
      <h2 className="font-semibold text-gray-700 mb-4">Типы активностей</h2>
      <ResponsiveContainer width="100%" height={220}>
        <PieChart>
          <Pie
            data={data}
            dataKey="count"
            nameKey="type"
            cx="50%"
            cy="50%"
            innerRadius={55}
            outerRadius={85}
          >
            {data.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
          </Pie>
          <Tooltip formatter={(v, _n, p) => [`${v} (${p.payload.percent}%)`, p.payload.type]} />
          <Legend formatter={value => value} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
