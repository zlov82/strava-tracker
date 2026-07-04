import { useState } from "react";
import axios from "axios";
import { Upload, ArrowLeft, CheckCircle2, AlertCircle } from "lucide-react";

// ── Colors (shared with Dashboard) ───────────────────────────────────────────
const C_BIKE    = "#16A97A";
const C_BG      = "#0F1117";
const C_SURFACE = "#181C27";
const C_SURF2   = "#1E2333";
const C_BORDER  = "#2A2F42";
const C_TEXT    = "#E8EAF0";
const C_MUTED   = "#6B7280";
const C_RED     = "#EF4444";

const fmtTime = (s) => {
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60);
  return h > 0 ? `${h} ч ${m} мин` : `${m} мин`;
};

const Field = ({ label, value }) => (
  <div style={{ background: C_SURF2, border: `1px solid ${C_BORDER}`, borderRadius: 10, padding: "10px 14px" }}>
    <div style={{ fontSize: 10, color: C_MUTED, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 3 }}>{label}</div>
    <div style={{ fontSize: 16, fontWeight: 700, color: C_TEXT }}>{value ?? "—"}</div>
  </div>
);

export default function ImportPage() {
  const stravaId = new URLSearchParams(window.location.search).get("stravaId");
  const [file, setFile]         = useState(null);
  const [name, setName]         = useState("");
  const [description, setDesc]  = useState("");
  const [status, setStatus]     = useState("idle"); // idle | loading | ok | error
  const [result, setResult]     = useState(null);
  const [error, setError]       = useState(null);

  const submit = async (e) => {
    e.preventDefault();
    if (!file) return;
    setStatus("loading");
    setError(null);
    setResult(null);
    const form = new FormData();
    form.append("file", file);
    if (stravaId)           form.append("stravaId", stravaId);
    if (name.trim())        form.append("name", name.trim());
    if (description.trim()) form.append("description", description.trim());
    try {
      const { data } = await axios.post("/api/import/fit", form);
      setResult(data);
      setStatus("ok");
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || err.message || "Ошибка загрузки");
      setStatus("error");
    }
  };

  return (
    <div style={{ background: C_BG, minHeight: "100vh", fontFamily: "'DM Sans', 'Helvetica Neue', sans-serif", color: C_TEXT }}>
      <style>{`@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');`}</style>

      <div style={{ maxWidth: 640, margin: "0 auto", padding: "32px 24px" }}>
        <a href="/" style={{ display: "inline-flex", alignItems: "center", gap: 6, color: C_MUTED, fontSize: 13, textDecoration: "none", marginBottom: 24 }}>
          <ArrowLeft size={14} /> К дашборду
        </a>

        <h1 style={{ fontSize: 22, fontWeight: 700, marginBottom: 6 }}>Импорт тренировки из FIT</h1>
        <p style={{ fontSize: 13, color: C_MUTED, marginBottom: 24, lineHeight: 1.6 }}>
          {stravaId ? (
            <>Загрузите оригинальный <code>.fit</code> файл (можно <code>.fit.gz</code>) — он будет привязан
            к заезду <strong style={{ color: C_TEXT }}>#{stravaId}</strong> независимо от имени файла.</>
          ) : (
            <>Загрузите оригинальный <code>.fit</code> файл (можно <code>.fit.gz</code>). Имя файла должно быть
            идентификатором тренировки, например <code>19166201899.fit</code>.</>
          )}
        </p>

        <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <label style={{
            border: `1.5px dashed ${file ? C_BIKE : C_BORDER}`, borderRadius: 12, padding: "24px 20px",
            display: "flex", flexDirection: "column", alignItems: "center", gap: 8, cursor: "pointer",
            background: C_SURFACE, transition: "border-color 0.15s",
          }}>
            <Upload size={22} color={file ? C_BIKE : C_MUTED} />
            <span style={{ fontSize: 13, color: file ? C_TEXT : C_MUTED }}>
              {file ? file.name : "Выберите FIT-файл"}
            </span>
            <input type="file" accept=".fit,.gz,application/octet-stream" style={{ display: "none" }}
              onChange={e => { setFile(e.target.files?.[0] ?? null); setStatus("idle"); }} />
          </label>

          <div>
            <label style={{ fontSize: 12, color: C_MUTED, display: "block", marginBottom: 6 }}>Название (необязательно)</label>
            <input value={name} onChange={e => setName(e.target.value)}
              placeholder="Если пусто — сгенерируется автоматически"
              style={{ width: "100%", background: C_SURFACE, border: `1px solid ${C_BORDER}`, borderRadius: 10, padding: "10px 14px", color: C_TEXT, fontSize: 14, fontFamily: "inherit", boxSizing: "border-box" }} />
          </div>

          <div>
            <label style={{ fontSize: 12, color: C_MUTED, display: "block", marginBottom: 6 }}>Описание (необязательно)</label>
            <textarea value={description} onChange={e => setDesc(e.target.value)} rows={3}
              style={{ width: "100%", background: C_SURFACE, border: `1px solid ${C_BORDER}`, borderRadius: 10, padding: "10px 14px", color: C_TEXT, fontSize: 14, fontFamily: "inherit", resize: "vertical", boxSizing: "border-box" }} />
          </div>

          <button type="submit" disabled={!file || status === "loading"}
            style={{
              background: !file || status === "loading" ? C_SURF2 : C_BIKE,
              color: !file || status === "loading" ? C_MUTED : "#052E22",
              border: "none", borderRadius: 10, padding: "12px 16px", fontSize: 14, fontWeight: 700,
              cursor: !file || status === "loading" ? "default" : "pointer", transition: "background 0.15s",
            }}>
            {status === "loading" ? "Загрузка…" : "Импортировать"}
          </button>
        </form>

        {status === "error" && (
          <div style={{ marginTop: 20, background: C_SURFACE, border: `1px solid ${C_RED}`, borderRadius: 10, padding: "14px 16px", display: "flex", gap: 10, alignItems: "flex-start" }}>
            <AlertCircle size={18} color={C_RED} style={{ flexShrink: 0, marginTop: 1 }} />
            <div style={{ fontSize: 13, color: C_TEXT }}>{error}</div>
          </div>
        )}

        {status === "ok" && result && (
          <div style={{ marginTop: 24 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 16 }}>
              <CheckCircle2 size={18} color={C_BIKE} />
              <span style={{ fontSize: 15, fontWeight: 700 }}>Импортировано: {result.name}</span>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(140px, 1fr))", gap: 10 }}>
              <Field label="Тип"           value={result.type} />
              <Field label="Расстояние"    value={`${(result.distance / 1000).toFixed(1)} км`} />
              <Field label="В движении"    value={fmtTime(result.movingTimeSec)} />
              <Field label="Общее время"   value={fmtTime(result.elapsedTimeSec)} />
              <Field label="Набор высоты"  value={`${Math.round(result.elevation)} м`} />
              <Field label="Ср. скорость"  value={result.avgSpeed ? `${result.avgSpeed} км/ч` : null} />
              <Field label="Ср. пульс"     value={result.heartrate?.avg ? `${Math.round(result.heartrate.avg)} уд/мин` : null} />
              <Field label="Кругов"        value={result.laps?.length ?? 0} />
            </div>
            <a href="/" style={{ display: "inline-block", marginTop: 20, color: C_BIKE, fontSize: 13, textDecoration: "none" }}>
              → Открыть дашборд
            </a>
          </div>
        )}
      </div>
    </div>
  );
}
