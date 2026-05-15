import { useEffect, useRef } from 'react';
import L from 'leaflet';
import polylineDecoder from '@mapbox/polyline';
import 'leaflet/dist/leaflet.css';

const TYPE_COLOR = { Ride: '#16A97A', Run: '#F59E0B', Swim: '#3B82F6', Walk: '#9CA3AF' };

export default function ActivityMap({ act }) {
  const containerRef = useRef(null);
  const mapRef      = useRef(null);

  const rawData  = act.rawData ? (typeof act.rawData === 'string' ? JSON.parse(act.rawData) : act.rawData) : null;
  const polyline = rawData?.map?.summary_polyline;

  useEffect(() => {
    if (!polyline || !containerRef.current) return;

    const coords = polylineDecoder.decode(polyline);
    const color  = TYPE_COLOR[act.type] ?? '#6B7280';

    const map  = L.map(containerRef.current, { zoomControl: true, scrollWheelZoom: false, attributionControl: false });
    mapRef.current = map;

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);

    const line = L.polyline(coords, { color, weight: 3.5, opacity: 0.9 }).addTo(map);
    map.fitBounds(line.getBounds(), { padding: [16, 16] });

    return () => { map.remove(); mapRef.current = null; };
  }, [polyline, act.type]);

  if (!polyline) return null;

  return (
    <div ref={containerRef} style={{ height: 240, borderRadius: 10, overflow: 'hidden', marginBottom: 16 }} />
  );
}
