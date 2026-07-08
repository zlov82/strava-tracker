import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import polylineDecoder from '@mapbox/polyline';
import 'leaflet/dist/leaflet.css';
import { C_BORDER, C_MUTED, C_SURFACE } from '../theme';

// Цвета трека подобраны под читаемость поверх тайлов карты, а не под палитру UI.
const TYPE_COLOR = { Ride: '#6366F1', Run: '#F97316', Swim: '#EC4899', Walk: '#A78BFA' };

function MapInstance({ polyline, type, style, onClick }) {
  const containerRef   = useRef(null);
  const mapRef         = useRef(null);
  const initializedRef = useRef(false);

  useEffect(() => {
    const el = containerRef.current;
    if (!el || initializedRef.current) return;

    const init = () => {
      if (!containerRef.current) return;
      const coords = polylineDecoder.decode(polyline);
      const color  = TYPE_COLOR[type] ?? '#6B7280';
      const map    = L.map(containerRef.current, { zoomControl: true, scrollWheelZoom: true, attributionControl: false });
      mapRef.current = map;
      initializedRef.current = true;
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
      const line = L.polyline(coords, { color, weight: 4, opacity: 0.9 }).addTo(map);
      map.fitBounds(line.getBounds(), { padding: [16, 16] });

      const markerOpts = (fillColor) => ({ radius: 6, fillColor, color: '#fff', weight: 2, opacity: 1, fillOpacity: 1 });
      L.circleMarker(coords[0],                 markerOpts('#22C55E')).addTo(map);
      L.circleMarker(coords[coords.length - 1], markerOpts('#EF4444')).addTo(map);
    };

    // Delay to ensure container has real dimensions (e.g. after CSS transition)
    const timer = setTimeout(init, 50);
    return () => {
      clearTimeout(timer);
      mapRef.current?.remove();
      mapRef.current = null;
      initializedRef.current = false;
    };
  }, [polyline, type]);

  return (
    <div ref={containerRef} style={style} onClick={onClick} />
  );
}

export default function ActivityMap({ act, standalone = false }) {
  const [expanded,   setExpanded]   = useState(false);
  const [fullscreen, setFullscreen] = useState(false);

  const activityRaw = act.activityRaw
    ? (typeof act.activityRaw === 'string' ? JSON.parse(act.activityRaw) : act.activityRaw)
    : null;
  const polyline = activityRaw?.map?.polyline
                ?? activityRaw?.map?.summary_polyline
                ?? act.mapPolyline;

  useEffect(() => {
    if (!fullscreen) return;
    const onKey = (e) => { if (e.key === 'Escape') setFullscreen(false); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [fullscreen]);

  if (!polyline) return null;

  const fullscreenOverlay = fullscreen && (
    <div
      onClick={() => setFullscreen(false)}
      style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.85)', zIndex: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
    >
      <div onClick={e => e.stopPropagation()} style={{ position: 'relative', width: '96vw', height: '90vh', borderRadius: 14, overflow: 'hidden', border: `1px solid ${C_BORDER}` }}>
        <MapInstance polyline={polyline} type={act.type} style={{ width: '100%', height: '100%' }} />
        <button
          onClick={() => setFullscreen(false)}
          style={{ position: 'absolute', top: 12, right: 12, zIndex: 1000, background: 'rgba(15,17,23,0.85)', border: `1px solid ${C_BORDER}`, borderRadius: 8, color: '#D0D5DD', fontSize: 18, width: 36, height: 36, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        >
          ✕
        </button>
      </div>
    </div>
  );

  if (standalone) {
    return (
      <>
        <div style={{ position: 'relative', borderRadius: 10, overflow: 'hidden', border: `1px solid ${C_BORDER}` }}>
          {!fullscreen
            ? <>
                <MapInstance polyline={polyline} type={act.type} style={{ height: 320, cursor: 'zoom-in' }} onClick={() => setFullscreen(true)} />
                <div style={{ position: 'absolute', bottom: 8, right: 8, background: 'rgba(15,17,23,0.75)', borderRadius: 6, padding: '3px 8px', fontSize: 11, color: '#D0D5DD', pointerEvents: 'none' }}>
                  нажми для полного экрана
                </div>
              </>
            : <div style={{ height: 320 }} />
          }
        </div>
        {fullscreenOverlay}
      </>
    );
  }

  return (
    <>
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
          position: 'relative',
        }}>
          {expanded && !fullscreen && (
            <>
              <MapInstance polyline={polyline} type={act.type} style={{ height: 260, cursor: 'zoom-in' }} onClick={() => setFullscreen(true)} />
              <div style={{ position: 'absolute', bottom: 8, right: 8, background: 'rgba(15,17,23,0.75)', borderRadius: 6, padding: '3px 8px', fontSize: 11, color: '#D0D5DD', pointerEvents: 'none' }}>
                нажми для полного экрана
              </div>
            </>
          )}
          {expanded && fullscreen && <div style={{ height: 260 }} />}
        </div>
      </div>
      {fullscreenOverlay}
    </>
  );
}
