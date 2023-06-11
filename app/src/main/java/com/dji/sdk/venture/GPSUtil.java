package com.dji.sdk.venture;

public class GPSUtil {
    private static final double EARTH_RADIUS = 6371; // Earth's radius in kilometers

    static double haversine(double lat1, double lon1,
                            double lat2, double lon2)
    {
        // distance between latitudes and longitudes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // apply formulae
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) *
                        Math.cos(lat1) *
                        Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_RADIUS * c; // it is in Km.
    }

    public static double calculateBearing(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        double startLatitudeRad = Math.toRadians(startLatitude);
        double startLongitudeRad = Math.toRadians(startLongitude);
        double endLatitudeRad = Math.toRadians(endLatitude);
        double endLongitudeRad = Math.toRadians(endLongitude);

        double deltaLongitude = endLongitudeRad - startLongitudeRad;

        double y = Math.sin(deltaLongitude) * Math.cos(endLatitudeRad);
        double x = Math.cos(startLatitudeRad) * Math.sin(endLatitudeRad) -
                Math.sin(startLatitudeRad) * Math.cos(endLatitudeRad) * Math.cos(deltaLongitude);

        double bearingRad = Math.atan2(y, x);
        double bearingDeg = Math.toDegrees(bearingRad);

        // Convert to a compass bearing between -180 and 180 degrees
        double bearing = (bearingDeg + 360) % 360;

        if (bearing > 180.0) {
            bearing -= 360.0;
        }

        return bearing;
    }

    public static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180.0;
    }

    public static double radiansToDegrees(double radians) {
        return radians * 180.0 / Math.PI;
    }

    public static double calculateDestinationLatitude(double startLatitude, double distance, double bearing) {
        double startLatitudeRad = degreesToRadians(startLatitude);
        double bearingRad = degreesToRadians(bearing);

        double newLatitudeRad = Math.asin(Math.sin(startLatitudeRad) * Math.cos(distance / EARTH_RADIUS) +
                Math.cos(startLatitudeRad) * Math.sin(distance / EARTH_RADIUS) * Math.cos(bearingRad));

        return radiansToDegrees(newLatitudeRad);
    }

    public static double calculateDestinationLongitude(double startLatitude, double startLongitude, double distance, double bearing) {
        double startLatitudeRad = degreesToRadians(startLatitude);
        double startLongitudeRad = degreesToRadians(startLongitude);
        double bearingRad = degreesToRadians(bearing);

        double newLatitude = calculateDestinationLatitude(startLatitude,distance,bearing);

        double newLongitudeRad = startLongitudeRad +
                Math.atan2(Math.sin(bearingRad) * Math.sin(distance / EARTH_RADIUS) * Math.cos(startLatitudeRad),
                        Math.cos(distance / EARTH_RADIUS) - Math.sin(startLatitudeRad) * Math.sin(degreesToRadians(newLatitude)));

        return radiansToDegrees(newLongitudeRad);
    }
}
