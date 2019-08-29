package org.waterapps.lib.DataDownload;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class GeoUtils {
    public static LatLngBounds makeSquare(LatLng center, float s) {
        double metersPerDegreeLat = (111132.92d - (559.82d * Math.cos(2.0d * ((center.latitude * 3.141592653589793d) / 180.0d)))) + (1.175d * Math.cos(4.0d * ((center.latitude * 3.141592653589793d) / 180.0d)));
        double metersPerDegreeLong = (111412.84d * Math.cos((center.latitude * 3.141592653589793d) / 180.0d)) - (93.5d * Math.cos(((3.0d * center.latitude) * 3.141592653589793d) / 180.0d));
        float r = s / 2.0f;
        double north = center.latitude + (((double) r) / metersPerDegreeLat);
        double east = center.longitude + (((double) r) / metersPerDegreeLong);
        LatLng latLng = new LatLng(center.latitude - (((double) r) / metersPerDegreeLat), center.longitude - (((double) r) / metersPerDegreeLong));
        LatLng latLng2 = new LatLng(north, east);
        LatLngBounds latLngBounds = new LatLngBounds(latLng, latLng2);
        return latLngBounds;
    }

    public static LatLngBounds makeRectangle(LatLng center, float s, float aspect) {
        double metersPerDegreeLat = (111132.92d - (559.82d * Math.cos(2.0d * ((center.latitude * 3.141592653589793d) / 180.0d)))) + (1.175d * Math.cos(4.0d * ((center.latitude * 3.141592653589793d) / 180.0d)));
        double metersPerDegreeLong = (111412.84d * Math.cos((center.latitude * 3.141592653589793d) / 180.0d)) - (93.5d * Math.cos(((3.0d * center.latitude) * 3.141592653589793d) / 180.0d));
        float r = s / 2.0f;
        double north = center.latitude + (((double) r) / metersPerDegreeLat);
        double east = center.longitude + (((double) aspect) * (((double) r) / metersPerDegreeLong));
        LatLng latLng = new LatLng(center.latitude - (((double) r) / metersPerDegreeLat), center.longitude - (((double) aspect) * (((double) r) / metersPerDegreeLong)));
        LatLng latLng2 = new LatLng(north, east);
        LatLngBounds latLngBounds = new LatLngBounds(latLng, latLng2);
        return latLngBounds;
    }

    public static float distanceBetween(LatLng p1, LatLng p2) {
        double longDistance = (p1.longitude - p2.longitude) * 111222.0d * Math.cos((p1.latitude + p2.latitude) / 2.0d);
        double latDistance = (p1.latitude - p2.latitude) * 111222.0d;
        return (float) Math.sqrt((latDistance * latDistance) + (longDistance * longDistance));
    }

    public static LatLng getCenter(LatLngBounds area) {
        return new LatLng((area.southwest.latitude + area.northeast.latitude) / 2.0d, (area.southwest.longitude + area.northeast.longitude) / 2.0d);
    }

    public static float getArea(LatLngBounds extent) {
        return getWidth(extent) * getHeight(extent);
    }

    public static float getWidth(LatLngBounds extent) {
        LatLng sw = extent.southwest;
        LatLng latLng = extent.northeast;
        LatLng se = new LatLng(extent.southwest.latitude, extent.northeast.longitude);
        new LatLng(extent.northeast.latitude, extent.southwest.longitude);
        return distanceBetween(sw, se);
    }

    public static float getHeight(LatLngBounds extent) {
        LatLng sw = extent.southwest;
        LatLng latLng = extent.northeast;
        new LatLng(extent.southwest.latitude, extent.northeast.longitude);
        return distanceBetween(sw, new LatLng(extent.northeast.latitude, extent.southwest.longitude));
    }
}
