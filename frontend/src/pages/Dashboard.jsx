import { useState, useMemo, useEffect } from "react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer,
} from "recharts";
import axios from "axios";
import ActivityMap from "../components/ActivityMap";
import ActivityLaps from "../components/ActivityLaps";
import ActivityCharts from "../components/ActivityCharts";

// ── API ────────────────────────────────────────────────────────────────────────
const api = axios.create();
api.interceptors.response.use(
  r => r,
  err => { if (err.response?.status === 401) window.location.href = "/auth/strava"; return Promise.reject(err); }
);

const fetchActivities = () =>
  api.get("/api/activities", { params: { page: 0, size: 500 } }).then(r => r.data.content.map(normalize));
const fetchAthlete    = () => api.get("/api/athlete").then(r => r.data);
const fetchSync       = () => api.get("/api/sync/status").then(r => r.data);

const normalize = (a) => ({
  ...a,
  start_date:    a.startDate,
  distance:      Math.round(a.distanceKm * 1000),
  moving_time:   a.movingTimeSec,
  elapsed_time:  a.elapsedTimeSec,
  average_speed: a.averageSpeedKmh ? a.averageSpeedKmh / 3.6 : 0,
  max_speed:     a.maxSpeedKmh ? a.maxSpeedKmh / 3.6 : null,
  type:          a.type === "VirtualRide" ? "Ride" : a.type,
});

// ── Helpers ────────────────────────────────────────────────────────────────────
const MONTHS      = ["Янв","Фев","Мар","Апр","Май","Июн","Июл","Авг","Сен","Окт","Ноя","Дек"];
const MONTHS_FULL = ["Январь","Февраль","Март","Апрель","Май","Июнь","Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь"];

const fmtDist  = (m, t) => t === "Swim" ? `${(m / 1000).toFixed(2)} км` : `${(m / 1000).toFixed(1)} км`;
const fmtTime  = (s) => { const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60); return h > 0 ? `${h}ч ${m}мин` : `${m}мин`; };
const fmtSpeed = (ms, t) => {
  if (t === "Swim") { const p = (100 / ms) / 60; const min = Math.floor(p); return `${min}:${String(Math.round((p - min) * 60)).padStart(2, "0")} /100м`; }
  return `${(ms * 3.6).toFixed(1)} км/ч`;
};
const fmtDate = (iso) => { const d = new Date(iso); return `${d.getDate()} ${MONTHS[d.getMonth()]}`; };

// ── Colors ─────────────────────────────────────────────────────────────────────
const C_BIKE    = "#16A97A";
const C_SWIM    = "#3B82F6";
const C_BG      = "#0F1117";
const C_SURFACE = "#181C27";
const C_SURF2   = "#1E2333";
const C_BORDER  = "#2A2F42";
const C_TEXT    = "#E8EAF0";
const C_MUTED   = "#6B7280";

// ── Components ─────────────────────────────────────────────────────────────────
const KpiCard = ({ label, value, sub, color }) => (
  <div style={{ background: C_SURFACE, border: `1px solid ${C_BORDER}`, borderRadius: 12, padding: "16px 20px", display: "flex", flexDirection: "column", gap: 4 }}>
    <span style={{ fontSize: 11, color: C_MUTED, letterSpacing: "0.08em", textTransform: "uppercase" }}>{label}</span>
    <span style={{ fontSize: 26, fontWeight: 700, color: color || C_TEXT, fontVariantNumeric: "tabular-nums", lineHeight: 1.1 }}>{value}</span>
    {sub && <span style={{ fontSize: 12, color: C_MUTED }}>{sub}</span>}
  </div>
);

const RecordCard = ({ label, value, name, date, color, onClick }) => (
  <div onClick={onClick} style={{ background: C_SURFACE, border: `1px solid ${C_BORDER}`, borderRadius: 12, padding: "14px 16px", display: "flex", flexDirection: "column", gap: 3, cursor: onClick ? "pointer" : "default", transition: "border-color 0.15s" }}
    onMouseEnter={e => onClick && (e.currentTarget.style.borderColor = color)}
    onMouseLeave={e => onClick && (e.currentTarget.style.borderColor = C_BORDER)}
  >
    <span style={{ fontSize: 10, color: C_MUTED, letterSpacing: "0.07em", textTransform: "uppercase", fontWeight: 600 }}>{label}</span>
    <span style={{ fontSize: 22, fontWeight: 700, color: color || C_TEXT, fontVariantNumeric: "tabular-nums", lineHeight: 1.15 }}>{value}</span>
    {name && <span style={{ fontSize: 12, color: C_TEXT, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{name}</span>}
    {date && <span style={{ fontSize: 11, color: C_MUTED }}>{date}</span>}
  </div>
);

const MiniMonthChart = ({ data, dataKey, color, onMonthClick }) => (
  <ResponsiveContainer width="100%" height={100}>
    <BarChart data={data} barGap={2} margin={{ top: 4, right: 0, left: -28, bottom: 0 }}
      onClick={({ activePayload } = {}) => activePayload?.[0] && onMonthClick(activePayload[0].payload.month)}
      style={{ cursor: "pointer" }}
    >
      <CartesianGrid strokeDasharray="3 3" stroke={C_BORDER} vertical={false} />
      <XAxis dataKey="name" tick={{ fill: C_MUTED, fontSize: 10 }} axisLine={false} tickLine={false} />
      <YAxis tick={{ fill: C_MUTED, fontSize: 10 }} axisLine={false} tickLine={false} width={36} />
      <Tooltip content={({ active, payload, label }) => active && payload?.length && payload[0].value > 0
        ? <div style={{ background: C_SURF2, border: `1px solid ${C_BORDER}`, borderRadius: 8, padding: "5px 10px", fontSize: 12, color: C_TEXT }}>{label}: <strong>{payload[0].value} км</strong></div>
        : null}
        cursor={{ fill: "rgba(255,255,255,0.03)" }}
      />
      <Bar dataKey={dataKey} fill={color} radius={[3, 3, 0, 0]} />
    </BarChart>
  </ResponsiveContainer>
);

const bestOf = (acts, key) => acts.length ? acts.reduce((b, a) => (a[key] ?? 0) > (b[key] ?? 0) ? a : b) : null;
const fmtActDate = (iso) => new Date(iso).toLocaleDateString("ru", { day: "numeric", month: "long" });

const TYPE_META = {
  Ride:  { bg: "#D1FAE5", fg: "#065F46", label: "🚴 ВЕЛО" },
  Swim:  { bg: "#DBEAFE", fg: "#1E40AF", label: "🏊 ПЛАВАНИЕ" },
  Run:   { bg: "#FEF3C7", fg: "#92400E", label: "🏃 БЕГ" },
  Walk:  { bg: "#F3F4F6", fg: "#374151", label: "🚶 ХОДЬБА" },
};
const DEFAULT_TYPE = { bg: "#F3F4F6", fg: "#374151", label: "🏅 ДРУГОЕ" };

const TypeBadge = ({ type }) => {
  const meta = TYPE_META[type] || DEFAULT_TYPE;
  return (
    <span style={{ fontSize: 10, fontWeight: 600, letterSpacing: "0.06em", padding: "2px 8px", borderRadius: 20, whiteSpace: "nowrap", background: meta.bg, color: meta.fg, display: "inline-block", minWidth: 88, textAlign: "center" }}>
      {meta.label}
    </span>
  );
};

const ActivityRow = ({ act, onClick }) => (
  <div className="act-row" onClick={onClick} style={{ display: "grid", gridTemplateColumns: "auto 1fr 100px 80px 110px", gap: "0 16px", alignItems: "center", padding: "12px 16px", background: C_SURFACE, border: `1px solid ${C_BORDER}`, borderRadius: 10, cursor: "pointer", transition: "border-color 0.15s" }}
    onMouseEnter={e => e.currentTarget.style.borderColor = C_BIKE}
    onMouseLeave={e => e.currentTarget.style.borderColor = C_BORDER}
  >
    <TypeBadge type={act.type} />
    <div>
      <div style={{ fontSize: 13, fontWeight: 600, color: C_TEXT, marginBottom: 2 }}>{act.name}</div>
      <div style={{ fontSize: 11, color: C_MUTED }}>{fmtDate(act.start_date)}</div>
    </div>
    <div style={{ textAlign: "right" }}>
      <div style={{ fontSize: 13, fontWeight: 600, color: C_TEXT }}>{fmtDist(act.distance, act.type)}</div>
      <div style={{ fontSize: 11, color: C_MUTED }}>расстояние</div>
    </div>
    <div className="act-time" style={{ textAlign: "right" }}>
      <div style={{ fontSize: 13, fontWeight: 600, color: C_TEXT }}>{fmtTime(act.moving_time)}</div>
      <div style={{ fontSize: 11, color: C_MUTED }}>время</div>
    </div>
    <div className="act-speed" style={{ textAlign: "right" }}>
      <div style={{ fontSize: 13, fontWeight: 600, color: act.type === "Ride" ? C_BIKE : C_SWIM }}>{fmtSpeed(act.average_speed, act.type)}</div>
      <div style={{ fontSize: 11, color: C_MUTED }}>скорость</div>
    </div>
  </div>
);

const DetailStat = ({ label, value, color }) => (
  <div style={{ background: C_SURF2, border: `1px solid ${C_BORDER}`, borderRadius: 10, padding: "12px 16px" }}>
    <div style={{ fontSize: 11, color: C_MUTED, marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.06em" }}>{label}</div>
    <div style={{ fontSize: 20, fontWeight: 700, color: color || C_TEXT }}>{value ?? "—"}</div>
  </div>
);

const isIndoorRide = (act) => act.type === "Ride" && (act.trainer || act.sportType === "VirtualRide");

const CopyLinkButton = ({ stravaId }) => {
  const [copied, setCopied] = useState(false);
  const copy = () => {
    const url = `${window.location.origin}${window.location.pathname}?activity=${stravaId}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };
  return (
    <button onClick={copy} title="Скопировать ссылку" style={{ background: "none", border: `1px solid ${C_BORDER}`, borderRadius: 8, color: copied ? C_BIKE : C_MUTED, fontSize: 13, cursor: "pointer", padding: "2px 10px", transition: "color 0.2s, border-color 0.2s", borderColor: copied ? C_BIKE : C_BORDER }}>
      {copied ? "✓ скопировано" : "🔗"}
    </button>
  );
};

const TABS = ["Обзор", "Маршрут", "Круги", "Графики"];

const ActivityModal = ({ act, onClose }) => {
  const [tab,          setTab]          = useState("Обзор");
  const [description,  setDescription]  = useState(act?.description ?? null);
  const [lapsRaw,      setLapsRaw]      = useState(act?.lapsRaw ?? null);
  const [streamsRaw,   setStreamsRaw]    = useState(act?.streamsRaw ?? null);
  const [loading,      setLoading]      = useState(false);
  const [streamsLoading, setStreamsLoading] = useState(false);

  useEffect(() => {
    if (!act) return;
    setTab("Обзор");
    setDescription(act.description ?? null);
    setLapsRaw(act.lapsRaw ?? null);
    setStreamsRaw(act.streamsRaw ?? null);
    if (act.activityRaw != null) return;
    setLoading(true);
    api.get(`/api/activities/${act.stravaId}`)
      .then(r => {
        setDescription(r.data.description ?? "");
        setLapsRaw(r.data.lapsRaw ?? null);
      })
      .catch(() => setDescription(""))
      .finally(() => setLoading(false));
  }, [act?.stravaId]);

  const onChartsTab = () => {
    setTab("Графики");
    if (streamsRaw != null) return;
    setStreamsLoading(true);
    api.get(`/api/activities/${act.stravaId}/streams`)
      .then(r => setStreamsRaw(r.data.streamsRaw ?? null))
      .catch(() => {})
      .finally(() => setStreamsLoading(false));
  };

  if (!act) return null;

  const hasMap      = !!(act.mapPolyline || act.activityRaw);
  const visibleTabs = TABS.filter(t => t !== "Маршрут" || hasMap);

  return (
    <div onClick={onClose} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.7)", zIndex: 100, display: "flex", alignItems: "center", justifyContent: "center" }}>
      <div onClick={e => e.stopPropagation()} style={{ background: C_SURFACE, border: `1px solid ${C_BORDER}`, borderRadius: 16, padding: 28, width: "min(560px, 92vw)", maxHeight: "85vh", overflowY: "auto" }}>

        {/* Шапка */}
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 16 }}>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 6 }}>
              <TypeBadge type={act.type} />
              {isIndoorRide(act) && <span style={{ fontSize: 10, color: C_MUTED, border: `1px solid ${C_BORDER}`, borderRadius: 20, padding: "2px 8px" }}>тренажёр</span>}
              {act.commute && <span style={{ fontSize: 10, color: C_MUTED, border: `1px solid ${C_BORDER}`, borderRadius: 20, padding: "2px 8px" }}>поездка на работу</span>}
            </div>
            <div style={{ fontSize: 18, fontWeight: 700, color: C_TEXT }}>{act.name}</div>
            <div style={{ fontSize: 12, color: C_MUTED, marginTop: 4 }}>
              {new Date(act.start_date).toLocaleString("ru", { day: "2-digit", month: "long", year: "numeric", hour: "2-digit", minute: "2-digit" })}
            </div>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <CopyLinkButton stravaId={act.stravaId} />
            <button onClick={onClose} style={{ background: "none", border: "none", color: C_MUTED, fontSize: 20, cursor: "pointer", lineHeight: 1 }}>✕</button>
          </div>
        </div>

        {/* Вкладки */}
        <div style={{ display: "flex", borderBottom: `1px solid ${C_BORDER}`, marginBottom: 20 }}>
          {visibleTabs.map(t => (
            <button key={t} onClick={t === "Графики" ? onChartsTab : () => setTab(t)} style={{
              padding: "8px 16px", background: "none", border: "none",
              borderBottom: tab === t ? `2px solid ${C_BIKE}` : "2px solid transparent",
              color: tab === t ? C_TEXT : C_MUTED,
              cursor: "pointer", fontSize: 13, fontWeight: tab === t ? 600 : 400,
              marginBottom: -1, transition: "color 0.15s",
            }}>
              {t}
            </button>
          ))}
        </div>

        {/* Обзор */}
        {tab === "Обзор" && (
          <>
            {description && (
              <div style={{ background: C_SURF2, border: `1px solid ${C_BORDER}`, borderRadius: 10, padding: "12px 16px", marginBottom: 16, fontSize: 13, color: C_MUTED, lineHeight: 1.6, whiteSpace: "pre-wrap" }}>
                {description}
              </div>
            )}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
              <DetailStat label="Расстояние"      value={fmtDist(act.distance, act.type)} color={act.type === "Ride" ? C_BIKE : C_SWIM} />
              <DetailStat label="Время в движении" value={fmtTime(act.moving_time)} />
              <DetailStat label="Общее время"      value={act.elapsed_time ? fmtTime(act.elapsed_time) : null} />
              <DetailStat label="Набор высоты"     value={act.elevationM ? `${act.elevationM} м` : null} />
              <DetailStat label="Ср. скорость"     value={act.average_speed ? fmtSpeed(act.average_speed, act.type) : null} />
              <DetailStat label="Макс. скорость"   value={act.max_speed ? fmtSpeed(act.max_speed, act.type) : null} />
              <DetailStat label="Ср. пульс"        value={act.averageHeartrate ? `${Math.round(act.averageHeartrate)} уд/мин` : null} />
              <DetailStat label="Макс. пульс"      value={act.maxHeartrate ? `${Math.round(act.maxHeartrate)} уд/мин` : null} />
              <DetailStat label="Ср. каденс"       value={act.averageCadence ? `${Math.round(act.averageCadence)} об/мин` : null} />
              <DetailStat label="Мощность"         value={act.averageWatts ? `${Math.round(act.averageWatts)} Вт` : null} />
            </div>
          </>
        )}

        {/* Маршрут */}
        {tab === "Маршрут" && import.meta.env.VITE_SHOW_ACTIVITY_MAP !== 'false' && (
          <ActivityMap act={act} standalone />
        )}

        {/* Круги */}
        {tab === "Круги" && (
          <ActivityLaps lapsRaw={lapsRaw} type={act.type} loading={loading} />
        )}

        {/* Графики */}
        {tab === "Графики" && (
          <ActivityCharts streamsRaw={streamsRaw} type={act.type} loading={streamsLoading} />
        )}

      </div>
    </div>
  );
};


const NavBtn = ({ active, color, onClick, children }) => (
  <button onClick={onClick} style={{
    padding: "5px 10px", borderRadius: 8, fontSize: 12, fontWeight: 600, cursor: "pointer",
    border: `1px solid ${active ? color : C_BORDER}`,
    background: active ? `${color}26` : "transparent",
    color: active ? color : C_MUTED,
    transition: "all 0.15s",
  }}>{children}</button>
);

// ── Dashboard ──────────────────────────────────────────────────────────────────
export default function Dashboard() {
  const [allActivities, setAllActivities] = useState([]);
  const [athlete, setAthlete]             = useState(null);
  const [sync, setSync]                   = useState(null);
  const [loading, setLoading]             = useState(true);
  const [selectedYear, setSelectedYear]   = useState(null);
  const [selectedMonth, setSelectedMonth] = useState(null);
  const [selectedAct, setSelectedAct]     = useState(null);

  const openActivity = (act) => {
    setSelectedAct(act);
    window.history.pushState({}, '', `?activity=${act.stravaId}`);
  };

  const closeActivity = () => {
    setSelectedAct(null);
    window.history.pushState({}, '', window.location.pathname);
  };

  useEffect(() => {
    Promise.all([fetchAthlete(), fetchActivities(), fetchSync()])
      .then(([ath, acts, syncData]) => {
        setAthlete(ath);
        setAllActivities(acts);
        setSync(syncData);
        if (acts.length > 0) {
          const years = [...new Set(acts.map(a => new Date(a.start_date).getFullYear()))].sort();
          setSelectedYear(years[years.length - 1]);
        }
        setLoading(false);
        const paramId = new URLSearchParams(window.location.search).get('activity');
        if (paramId) {
          const found = acts.find(a => String(a.stravaId) === paramId);
          if (found) setSelectedAct(found);
        }
      });

    const timer = setInterval(() => {
      fetchActivities().then(setAllActivities);
      fetchSync().then(setSync);
    }, 60_000);
    return () => clearInterval(timer);
  }, []);

  const years = useMemo(() =>
    [...new Set(allActivities.map(a => new Date(a.start_date).getFullYear()))].sort(),
  [allActivities]);

  const handleYearChange = (y) => { setSelectedYear(y); setSelectedMonth(null); };

  const yearActs = useMemo(() =>
    allActivities.filter(a => new Date(a.start_date).getFullYear() === selectedYear),
  [allActivities, selectedYear]);

  const filtered = useMemo(() => {
    if (selectedMonth === null) return yearActs;
    return yearActs.filter(a => new Date(a.start_date).getMonth() === selectedMonth);
  }, [yearActs, selectedMonth]);

  const bikeActs     = filtered.filter(a => a.type === "Ride");
  const swimActs     = filtered.filter(a => a.type === "Swim");
  const runActs      = filtered.filter(a => a.type === "Run");
  const walkActs     = filtered.filter(a => a.type === "Walk");
  const totalBikeKm  = bikeActs.reduce((s, a) => s + a.distance, 0) / 1000;
  const totalSwimKm  = swimActs.reduce((s, a) => s + a.distance, 0) / 1000;
  const totalRunKm   = runActs.reduce((s, a) => s + a.distance, 0) / 1000;
  const totalWalkKm  = walkActs.reduce((s, a) => s + a.distance, 0) / 1000;
  const totalTimeSec = filtered.reduce((s, a) => s + a.moving_time, 0);

  const activeMonths = useMemo(() =>
    new Set(yearActs.map(a => new Date(a.start_date).getMonth())),
  [yearActs]);

  const monthlyData = useMemo(() => MONTHS.map((name, idx) => {
    const acts = yearActs.filter(a => new Date(a.start_date).getMonth() === idx);
    return {
      name, month: idx,
      bikeKm: +(acts.filter(a => a.type === "Ride").reduce((s, a) => s + a.distance, 0) / 1000).toFixed(1),
      swimKm: +(acts.filter(a => a.type === "Swim").reduce((s, a) => s + a.distance, 0) / 1000).toFixed(1),
      runKm:  +(acts.filter(a => a.type === "Run" ).reduce((s, a) => s + a.distance, 0) / 1000).toFixed(1),
    };
  }), [yearActs]);


  const recent = selectedMonth === null ? filtered.slice(0, 8) : filtered;

  const periodLabel = selectedMonth === null
    ? `${selectedYear}, весь год`
    : `${MONTHS_FULL[selectedMonth]} ${selectedYear}`;

  if (loading) return (
    <div style={{ background: C_BG, minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", color: C_MUTED, fontFamily: "sans-serif" }}>
      Загрузка…
    </div>
  );

  return (
    <div style={{ background: C_BG, minHeight: "100vh", fontFamily: "'DM Sans', 'Helvetica Neue', sans-serif", color: C_TEXT, paddingBottom: 48 }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');
        .month-nav { overflow-x: auto; flex-wrap: nowrap !important; -webkit-overflow-scrolling: touch; padding-bottom: 2px; }
        .month-nav::-webkit-scrollbar { display: none; }
        @media (max-width: 640px) {
          .act-row { grid-template-columns: auto 1fr auto !important; }
          .act-time, .act-speed { display: none !important; }
          .header-pad { padding: 12px 16px !important; }
          .content-pad { padding: 0 16px !important; }
          .kpi-grid { grid-template-columns: repeat(2, 1fr) !important; }

        }
      `}</style>
      <ActivityModal act={selectedAct} onClose={closeActivity} />

      {/* Header */}
      <div className="header-pad" style={{ borderBottom: `1px solid ${C_BORDER}`, padding: "16px 32px", position: "sticky", top: 0, background: C_BG, zIndex: 10 }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 12 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            {athlete?.avatar
              ? <img src={athlete.avatar} alt="" style={{ width: 36, height: 36, borderRadius: 10, objectFit: "cover" }} />
              : <div style={{ width: 36, height: 36, borderRadius: 10, background: "linear-gradient(135deg, #16A97A, #3B82F6)" }} />
            }
            <div>
              <div style={{ fontSize: 16, fontWeight: 700, letterSpacing: "-0.02em" }}>{athlete?.name ?? "Strava Dashboard"}</div>
              <div style={{ fontSize: 12, color: C_MUTED }}>
                {periodLabel}
                {sync?.lastSyncAt && <span> · синхр. {new Date(sync.lastSyncAt).toLocaleString("ru", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" })}</span>}
              </div>
            </div>
          </div>
          <div style={{ display: "flex", gap: 6 }}>
            {years.map(y => (
              <NavBtn key={y} active={selectedYear === y} color={C_BIKE} onClick={() => handleYearChange(y)}>{y}</NavBtn>
            ))}
          </div>
        </div>

        <div className="month-nav" style={{ display: "flex", gap: 5, flexWrap: "wrap" }}>
          <NavBtn active={selectedMonth === null} color={C_BIKE} onClick={() => setSelectedMonth(null)}>Весь год</NavBtn>
          {MONTHS.map((m, i) => activeMonths.has(i) && (
            <NavBtn key={i} active={selectedMonth === i} color={C_SWIM} onClick={() => setSelectedMonth(i)}>{m}</NavBtn>
          ))}
        </div>
      </div>

      <div className="content-pad" style={{ maxWidth: 1100, margin: "0 auto", padding: "0 32px" }}>
        {/* KPI */}
        <div className="kpi-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))", gap: 12, margin: "24px 0" }}>
          {(selectedMonth === null || totalBikeKm > 0) && <KpiCard label="🚴 Велосипед" value={`${totalBikeKm.toFixed(0)} км`}  sub={`${bikeActs.length} поездок`}  color={C_BIKE} />}
          {(selectedMonth === null || totalSwimKm > 0) && <KpiCard label="🏊 Плавание" value={`${totalSwimKm.toFixed(1)} км`}  sub={`${swimActs.length} сессий`}   color={C_SWIM} />}
          {(selectedMonth === null || totalRunKm > 0)  && <KpiCard label="🏃 Бег"      value={`${totalRunKm.toFixed(1)} км`}   sub={`${runActs.length} пробежек`}  color="#F59E0B" />}
          {(selectedMonth === null || totalWalkKm > 0) && <KpiCard label="🚶 Ходьба"   value={`${totalWalkKm.toFixed(1)} км`}  sub={`${walkActs.length} прогулок`} color="#9CA3AF" />}
          {selectedMonth !== null && <KpiCard label="Активностей" value={filtered.length} sub="за месяц" />}
          <KpiCard label="Время в движении" value={fmtTime(totalTimeSec)} sub="суммарно" />
        </div>

        {/* Рекорды года */}
        {selectedMonth === null && (() => {
          const longestRide  = bestOf(bikeActs, "distance");
          const highestRide  = bestOf(bikeActs, "elevationM");
          const fastestRide  = bestOf(bikeActs.filter(a => a.distance > 10000), "average_speed");
          const longestSwim  = bestOf(swimActs, "distance");
          const longestSwimT = bestOf(swimActs, "moving_time");
          const longestRun   = bestOf(runActs,  "distance");

          const groups = [
            bikeActs.length > 0 && {
              label: "🚴 Велосипед", color: C_BIKE, dataKey: "bikeKm",
              records: [
                longestRide  && { label: "Самая длинная поездка",          value: `${(longestRide.distance/1000).toFixed(1)} км`,         act: longestRide },
                highestRide  && { label: "Наибольший набор высоты",        value: `${highestRide.elevationM.toFixed(0)} м`,               act: highestRide },
                fastestRide  && { label: "Самая быстрая средняя скорость", value: `${(fastestRide.average_speed * 3.6).toFixed(1)} км/ч`, act: fastestRide },
              ].filter(Boolean),
            },
            swimActs.length > 0 && {
              label: "🏊 Плавание", color: C_SWIM, dataKey: "swimKm",
              records: [
                longestSwim  && { label: "Самое длинное плавание",         value: `${(longestSwim.distance/1000).toFixed(2)} км`,         act: longestSwim },
                longestSwimT && { label: "Самый долгий заплыв",            value: fmtTime(longestSwimT.moving_time),                     act: longestSwimT },
              ].filter(Boolean),
            },
            runActs.length > 0 && {
              label: "🏃 Бег", color: "#F59E0B", dataKey: "runKm",
              records: [
                longestRun   && { label: "Самый длинный забег",            value: `${(longestRun.distance/1000).toFixed(1)} км`,          act: longestRun },
              ].filter(Boolean),
            },
          ].filter(Boolean);

          if (groups.length === 0) return null;
          return (
            <div style={{ marginBottom: 20 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: C_MUTED, letterSpacing: "0.06em", textTransform: "uppercase", marginBottom: 12 }}>
                Рекорды {selectedYear}
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
                {groups.map(({ label, color, dataKey, records }) => (
                  <div key={label} style={{ background: C_SURFACE, border: `1px solid ${C_BORDER}`, borderRadius: 14, padding: "16px 20px" }}>
                    <div style={{ fontSize: 11, color, fontWeight: 600, letterSpacing: "0.05em", textTransform: "uppercase", marginBottom: 12 }}>{label}</div>
                    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(170px, 1fr))", gap: 10, marginBottom: 16 }}>
                      {records.map(({ label: rl, value, act }) => (
                        <RecordCard key={rl} label={rl} value={value} name={act.name} date={fmtActDate(act.start_date)} color={color} onClick={() => openActivity(act)} />
                      ))}
                    </div>
                    <div style={{ fontSize: 11, color: C_MUTED, fontWeight: 600, letterSpacing: "0.05em", textTransform: "uppercase", marginBottom: 4 }}>Объём по месяцам, км</div>
                    <MiniMonthChart data={monthlyData} dataKey={dataKey} color={color} onMonthClick={setSelectedMonth} />
                  </div>
                ))}
              </div>
            </div>
          );
        })()}


        {/* Activity list — только на вкладке конкретного месяца */}
        {selectedMonth !== null && <div style={{ background: C_SURFACE, border: `1px solid ${C_BORDER}`, borderRadius: 14, padding: "20px 24px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 }}>
            <div style={{ fontSize: 13, fontWeight: 600, color: C_MUTED, letterSpacing: "0.06em", textTransform: "uppercase" }}>
              {`Активности — ${MONTHS_FULL[selectedMonth]}`}
            </div>
            <div style={{ fontSize: 12, color: C_MUTED }}>{filtered.length} активностей</div>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {filtered.length === 0
              ? <div style={{ color: C_MUTED, fontSize: 13, padding: "20px 0", textAlign: "center" }}>Нет активностей за этот период</div>
              : filtered.map(act => <ActivityRow key={act.stravaId} act={act} onClick={() => openActivity(act)} />)
            }
          </div>
        </div>}
      </div>
    </div>
  );
}
