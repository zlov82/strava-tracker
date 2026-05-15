import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import polylineDecoder from '@mapbox/polyline';
import 'leaflet/dist/leaflet.css';

const TYPE_COLOR = { Ride: '#16A97A', Run: '#F59E0B', Swim: '#3B82F6', Walk: '#9CA3AF' };
const C_BORDER   = '#2A2F42';
const C_MUTED    = '#6B7280';
const C_SURFACE  = '#181C27';

export default function ActivityMap({ act }) {
  const containerRef  = useRef(null);
  const mapRef        = useRef(null);
  const [expanded, setExpanded] = useState(false);
  const [initialized, setInitialized] = useState(false);

  const rawData  = act.rawData ? (typeof act.rawData === 'string' ? JSON.parse(act.rawData) : act.rawData) : null;
  const polyline = rawData?.map?.summary_polyline;

  // Init map on first expand
  useEffect(() => {
    if (!expanded || !polyline || !containerRef.current || initialized) return;

    const coords = polylineDecoder.decode(polyline);
    const color  = TYPE_COLOR[act.type] ?? '#6B7280';

    const map = L.map(containerRef.current, { zoomControl: true, scrollWheelZoom: false, attributionControl: false });
    mapRef.current = map;

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
    const line = L.polyline(coords, { color, weight: 3.5, opacity: 0.9 }).addTo(map);
    map.fitBounds(line.getBounds(), { padding: [16, 16] });

    setInitialized(true);
    return () => { map.remove(); mapRef.current = null; };
  }, [expanded, polyline, act.type, initialized]);

  // After expand animation ends, fix tile rendering
  useEffect(() => {
    if (expanded && mapRef.current) {
      const t = setTimeout(() => mapRef.current?.invalidateSize(), 310);
      return () => clearTimeout(t);
    }
  }, [expanded]);

  if (!polyline) return null;

  return (
    <div style={{ marginBottom: 16 }}>
      <button
        onClick={() => setExpanded(v => !v)}
        style={{
          width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          background: C_SURFACE, border: `1px solid ${C_BORDER}`, borderRadius: expanded ? '10px 10px 0 0' : 10,
          padding: '9px 14px', cursor: 'pointer', color: C_MUTED, fontSize: 12, fontWeight: 600,
          letterSpacing: '0.06em', textTransform: 'uppercase', transition: 'border-radius 0.3s',
        }}
      >
        <span>Маршрут</span>
        <span style={{ fontSize: 14, transition: 'transform 0.3s', transform: expanded ? 'rotate(180deg)' : 'none' }}>▾</span>
      </button>

      <div style={{
        overflow: 'hidden',
        maxHeight: expanded ? '300px' : '0',
        transition: 'max-height 0.3s ease',
        borderRadius: '0 0 10px 10px',
        border: expanded ? `1px solid ${C_BORDER}` : 'none',
        borderTop: 'none',
      }}>
        <div ref={containerRef} style={{ height: 260 }} />
      </div>
    </div>
  );
}
