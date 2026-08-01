package com.bloodbridge.bloodbridge.util;

public class GeoHelper {

    private static final double EARTH_RADIUS_KM = 6371;

    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public static BoundingBox calculateBoundingBox(double lat, double lng, int radiusKm) {
        double cosLat = Math.cos(Math.toRadians(lat));
        cosLat = Math.abs(cosLat) > 0.0001 ? cosLat : 0.0001;

        double latChange = (double) radiusKm / 111;
        double lngChange = Math.abs(radiusKm / (111 * cosLat));

        return new BoundingBox(
                lat - latChange,
                lat + latChange,
                lng - lngChange,
                lng + lngChange
        );
    }

    public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {}
}