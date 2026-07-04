package xyz.zlov.app.strava.fit;

import java.util.List;

/**
 * Encodes a list of [lat, lng] points into a Google Encoded Polyline (precision 5),
 * matching the format Strava produced in summary_polyline and that the frontend
 * decodes via @mapbox/polyline.
 */
final class PolylineEncoder {

    private PolylineEncoder() {}

    static String encode(List<double[]> points) {
        StringBuilder sb = new StringBuilder();
        long lastLat = 0;
        long lastLng = 0;
        for (double[] p : points) {
            long lat = Math.round(p[0] * 1e5);
            long lng = Math.round(p[1] * 1e5);
            encodeSigned(lat - lastLat, sb);
            encodeSigned(lng - lastLng, sb);
            lastLat = lat;
            lastLng = lng;
        }
        return sb.toString();
    }

    private static void encodeSigned(long value, StringBuilder sb) {
        long v = value << 1;
        if (value < 0) v = ~v;
        while (v >= 0x20) {
            sb.append((char) ((int) ((0x20 | (v & 0x1f)) + 63)));
            v >>= 5;
        }
        sb.append((char) ((int) (v + 63)));
    }
}
