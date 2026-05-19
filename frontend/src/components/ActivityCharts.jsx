import {
  ResponsiveContainer, LineChart, AreaChart,
  Line, Area, XAxis, YAxis, Tooltip, CartesianGrid,
} from 'recharts';

const C_BORDER = '#2A2F42';
const C_MUTED  = '#6B7280';
const C_SURF2  = '#1E2333';
const C_TEXT   = '#E8EAF0';

const CHART_COLORS = {
  heartrate:        '#EF4444',
  velocity_smooth:  '#6366F1',
  altitude:         '#16A97A',
  cadence:          '#F97316',
  watts:            '#A78BFA',
};

const MAX_POINTS = 300;

const downsample = (arr) => {
  if (!arr || arr.length <= MAX_POINTS) return arr;
  const step = Math.ceil(arr.length / MAX_POINTS);
  return arr.filter((_, i) => i % step === 0);
};

const ChartTooltip = ({ active, payload, label, xFormatter, yFormatter }) => {
  if (!active || !payload?.length) return null;
  const val = payload[0]?.value;
  return (
    <div style={{ background: C_SURF2, border: `1px solid ${C_BORDER}`, borderRadius: 8, padding: '6px 12px', fontSize: 12, color: C_TEXT }}>
      <div style={{ color: C_MUTED, marginBottom: 2 }}>{xFormatter ? xFormatter(label) : label}</div>
      <div style={{ color: payload[0]?.color, fontWeight: 600 }}>{yFormatter ? yFormatter(val) : val}</div>
    </div>
  );
};

const fmtAxisNum = (v) => typeof v === 'number' ? +v.toFixed(1) : v;

const fmtDist = (v) => `${v} км`;


const Chart = ({ title, data, dataKey, color, xKey, xFormatter, yFormatter, area = false, stat }) => {
  if (!data || data.length === 0) return null;
  const ChartComp  = area ? AreaChart  : LineChart;
  const SeriesComp = area ? Area : Line;
  return (
    <div style={{ marginBottom: 20 }}>
      <div style={{ fontSize: 11, color: C_MUTED, fontWeight: 600, letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: 6 }}>
        {title}
      </div>
      <ResponsiveContainer width="100%" height={140}>
        <ChartComp data={data} margin={{ top: 4, right: 4, left: -16, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke={C_BORDER} />
          <XAxis dataKey={xKey} tickFormatter={xFormatter} tick={{ fontSize: 10, fill: C_MUTED }} tickLine={false} axisLine={false} interval="preserveStartEnd" />
          <YAxis domain={['auto', 'auto']} tick={{ fontSize: 10, fill: C_MUTED }} tickLine={false} axisLine={false} width={48} tickFormatter={fmtAxisNum} />
          <Tooltip content={<ChartTooltip xFormatter={xFormatter} yFormatter={yFormatter} />} />
          <SeriesComp
            type="monotone" dataKey={dataKey}
            stroke={color} strokeWidth={1.5} dot={false}
            fill={area ? color : undefined} fillOpacity={area ? 0.15 : undefined}
            isAnimationActive={false}
          />
        </ChartComp>
      </ResponsiveContainer>
      {stat && (
        <div style={{ marginTop: 6, fontSize: 12, textAlign: 'center' }}>
          <span style={{ color: C_MUTED }}>{stat.label}: </span>
          <span style={{ color: C_TEXT, fontWeight: 600 }}>{stat.value}</span>
        </div>
      )}
    </div>
  );
};

export default function ActivityCharts({ streamsRaw, type, loading, activity }) {
  if (loading) return (
    <div style={{ fontSize: 12, color: C_MUTED, padding: '24px 0', textAlign: 'center' }}>загрузка…</div>
  );
  if (!streamsRaw) return (
    <div style={{ fontSize: 12, color: C_MUTED, padding: '24px 0', textAlign: 'center' }}>нет данных</div>
  );

  const s = typeof streamsRaw === 'string' ? JSON.parse(streamsRaw) : streamsRaw;

  const distance = downsample(s.distance?.data);
  const hr       = downsample(s.heartrate?.data);
  const vel      = downsample(s.velocity_smooth?.data);
  const alt      = downsample(s.altitude?.data);
  const cad      = downsample(s.cadence?.data);
  const watts    = downsample(s.watts?.data);

  const byDist = (values) =>
    distance && values ? distance.map((d, i) => ({ d: +(d / 1000).toFixed(2), v: values[i] })) : null;

  const isPace = type === 'Run' || type === 'Walk';
  const velLabel    = type === 'Ride' ? 'км/ч' : 'мин/км';
  const velConvert  = (ms) => type === 'Ride' ? +(ms * 3.6).toFixed(1) : +(1000 / ms / 60).toFixed(2);
  const velData     = vel ? byDist(vel).map(p => ({ ...p, v: velConvert(p.v) })) : null;

  const avgHr  = activity?.averageHeartrate ? Math.round(activity.averageHeartrate) : null;
  const avgVel = activity?.average_speed    ? velConvert(activity.average_speed)    : null;
  const avgCad = activity?.averageCadence   ? Math.round(activity.averageCadence)   : null;
  const gain   = activity?.elevationM       ? activity.elevationM                   : null;

  return (
    <div>
      <Chart
        title="Пульс, уд/мин"
        data={byDist(hr)}
        dataKey="v" xKey="d" color={CHART_COLORS.heartrate}
        xFormatter={fmtDist} yFormatter={(v) => `${Math.round(v)}`}
        area
        stat={avgHr != null ? { label: 'Средний пульс', value: `${avgHr} уд/мин` } : null}
      />
      <Chart
        title={isPace ? 'Темп, мин/км' : 'Скорость, км/ч'}
        data={velData}
        dataKey="v" xKey="d" color={CHART_COLORS.velocity_smooth}
        xFormatter={fmtDist} yFormatter={(v) => `${v} ${velLabel}`}
        area
        stat={avgVel != null ? { label: 'Средняя скорость', value: `${avgVel} ${velLabel}` } : null}
      />
      <Chart
        title="Профиль высот, м"
        data={byDist(alt)}
        dataKey="v" xKey="d" color={CHART_COLORS.altitude}
        xFormatter={fmtDist} yFormatter={(v) => `${Math.round(v)} м`}
        area
        stat={gain != null ? { label: 'Набор высоты', value: `${gain} м` } : null}
      />
      <Chart
        title="Каденс, об/мин"
        data={byDist(cad)}
        dataKey="v" xKey="d" color={CHART_COLORS.cadence}
        xFormatter={fmtDist} yFormatter={(v) => `${Math.round(v)}`}
        stat={avgCad != null ? { label: 'Средний каденс', value: `${avgCad} об/мин` } : null}
      />
      {type === 'Ride' && (
        <Chart
          title="Мощность, Вт"
          data={byDist(watts)}
          dataKey="v" xKey="d" color={CHART_COLORS.watts}
          xFormatter={fmtDist} yFormatter={(v) => `${Math.round(v)}`}
        />
      )}
    </div>
  );
}
