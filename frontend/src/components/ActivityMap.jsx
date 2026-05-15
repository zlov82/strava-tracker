import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import polylineDecoder from '@mapbox/polyline';
import 'leaflet/dist/leaflet.css';

const TYPE_COLOR = { Ride: '#16A97A', Run: '#F59E0B', Swim: '#3B82F6', Walk: '#9CA3AF' };
const C_BORDER  = '#2A2F42';
const C_MUTED   = '#6B7280';
const C_SURFACE = '#181C27';

export default function ActivityMap({ act }) {
  const containerRef  = useRef(null);
  const mapRef        = useRef(null);
  const initializedRef = useRef(false);
  const [expanded, setExpanded] = useState(false);

  const rawData  = act.rawData ? (typeof act.rawData === 'string' ? JSON.parse(act.rawData) : act.rawData) : null;
  const polyline = rawData?.map?.summary_polyline;

  useEffect(() => {
    if (!expanded || !polyline || initializedRef.current) return;

    // Wait for CSS transition (300ms) before init so container has real dimensions
    const timer = setTimeout(() => {
      if (!containerRef.current) return;

      const coords = polylineDecoder.decode(polyline);
      const color  = TYPE_COLOR[act.type] ?? '#6B7280';

      const map = L.map(containerRef.current, { zoomControl: true, scrollWheelZoom: false, attributionControl: false });
      mapRef.current = map;
      initializedRef.current = true;

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
      const line = L.polyline(coords, { color, weight: 3.5, opacity: 0.9 }).addTo(map);
      map.fitBounds(line.getBounds(), { padding: [16, 16] });
    }, 310);

    return () => clearTimeout(timer);
  }, [expanded, polyline, act.type]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
      initializedRef.current = false;
    };
  }, []);

  if (!polyline) return null;

  return (
    <div style={{ marginBottom: 16 }}>
      <button
        onClick={() => setExpanded(v => !v)}
        style={{
          width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          background: C_SURFACE, border: `1px solid ${C_BORDER}`,
          borderRadius: expanded ? '10px 10px 0 0' : 10,
          padding: '9px 14px', cursor: 'pointer', color: C_MUTED, fontSize: 12, fontWeight: 600,
          letterSpacing: '0.06em', textTransform: 'uppercase', transition: 'border-radius 0.3s',
        }}
      >
        <span>Маршрут</span>
        <span style={{ fontSize: 14, display: 'inline-block', transition: 'transform 0.3s', transform: expanded ? 'rotate(180deg)' : 'none' }}>▾</span>
      </button>

      <div style={{
        overflow: 'hidden',
        maxHeight: expanded ? '300px' : '0',
        transition: 'max-height 0.3s ease',
        border: expanded ? `1px solid ${C_BORDER}` : 'none',
        borderTop: 'none',
        borderRadius: '0 0 10px 10px',
      }}>
        <div ref={containerRef} style={{ height: 260 }} />
      </div>
    </div>
  );
}
