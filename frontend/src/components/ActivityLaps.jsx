import { C_BORDER, C_MUTED, C_SURF2, C_TEXT } from '../theme';

const fmtTime = (s) => {
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
  if (h > 0) return `${h}:${String(m).padStart(2,'0')}:${String(sec).padStart(2,'0')}`;
  return `${m}:${String(sec).padStart(2,'0')}`;
};

const fmtPace = (ms, type) => {
  if (!ms || ms <= 0) return '—';
  if (type === 'Swim') {
    const secPer100 = 100 / ms;
    const m = Math.floor(secPer100 / 60);
    return `${m}:${String(Math.round(secPer100 % 60)).padStart(2,'0')} /100м`;
  }
  if (type === 'Run' || type === 'Walk') {
    const secPerKm = 1000 / ms;
    const m = Math.floor(secPerKm / 60);
    return `${m}:${String(Math.round(secPerKm % 60)).padStart(2,'0')} /км`;
  }
  return `${(ms * 3.6).toFixed(1)} км/ч`;
};

const fmtDist = (m, type) =>
  type === 'Swim' ? `${Math.round(m)} м` : `${(m / 1000).toFixed(2)} км`;

export default function ActivityLaps({ lapsRaw, type, loading }) {
  const laps = lapsRaw
    ? (typeof lapsRaw === 'string' ? JSON.parse(lapsRaw) : lapsRaw)
    : null;

  if (loading) return (
    <div style={{ fontSize: 12, color: C_MUTED, padding: '24px 0', textAlign: 'center' }}>загрузка…</div>
  );

  if (!laps || laps.length === 0) return (
    <div style={{ fontSize: 12, color: C_MUTED, padding: '24px 0', textAlign: 'center' }}>нет данных</div>
  );

  const hasHr      = laps.some(l => l.average_heartrate);
  const hasCadence = laps.some(l => l.average_cadence);
  // Swim laps: the watch pauses its timer (moving_time), so moving_time/pace per lap is meaningless
  // (e.g. 25 m "in 4 s" while the lap took 31 s). Show full lap time and no per-lap pace.
  const isSwim = type === 'Swim';

  return (
    <>
    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
      <thead>
        <tr style={{ background: C_SURF2 }}>
          <Th>#</Th>
          <Th>Дист</Th>
          <Th>Время</Th>
          {!isSwim && <Th>{type === 'Ride' ? 'Скорость' : 'Темп'}</Th>}
          {hasHr      && <Th>Пульс</Th>}
          {hasCadence && <Th>Каденс</Th>}
        </tr>
      </thead>
      <tbody>
        {laps.map((lap, i) => (
          <tr key={lap.id ?? i} style={{ borderTop: `1px solid ${C_BORDER}`, background: i % 2 === 1 ? C_SURF2 : 'transparent' }}>
            <Td muted>{lap.lap_index ?? i + 1}</Td>
            <Td>{fmtDist(lap.distance, type)}</Td>
            <Td>{fmtTime(isSwim ? Math.round(lap.elapsed_time ?? lap.moving_time) : lap.moving_time)}</Td>
            {!isSwim && <Td>{fmtPace(lap.average_speed, type)}</Td>}
            {hasHr      && <Td>{lap.average_heartrate ? `${Math.round(lap.average_heartrate)}` : '—'}</Td>}
            {hasCadence && <Td>{lap.average_cadence  ? `${Math.round(lap.average_cadence)}`  : '—'}</Td>}
          </tr>
        ))}
      </tbody>
    </table>
    {isSwim && (
      <div style={{ fontSize: 11, color: C_MUTED, padding: '10px 12px' }}>
        Время круга полное (с паузами). Темп по кругам не показывается: часы ставят таймер на паузу, а дистанции кругов в файле неточны.
      </div>
    )}
    </>
  );
}

const Th = ({ children }) => (
  <th style={{ padding: '7px 12px', color: C_MUTED, fontWeight: 600, textAlign: 'left', letterSpacing: '0.05em' }}>
    {children}
  </th>
);

const Td = ({ children, muted }) => (
  <td style={{ padding: '7px 12px', color: muted ? C_MUTED : C_TEXT, fontVariantNumeric: 'tabular-nums' }}>
    {children}
  </td>
);
