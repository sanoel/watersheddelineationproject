package org.waterapps.lib;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;

/**
 * Created by Steve on 13/10/30.
 */
public class GeoRectangle {
    final static double metersPerDegree = 111222.0;
    final static double metersPerFoot = 100.0/(2.54*12.0);
    protected double north, south, east, west;
    public enum unit {METERS, FEET};
    private unit units = unit.METERS;

    public GeoRectangle(LatLngBounds extent) {
        north = extent.northeast.latitude;
        south = extent.southwest.latitude;
        east = extent.northeast.longitude;
        west = extent.southwest.longitude;
    }

    GeoRectangle(LatLng p1, LatLng p2) {
        north = p1.latitude > p2.latitude ? p1.latitude : p2.latitude;
        south = p1.latitude < p2.latitude ? p1.latitude : p2.latitude;
        east = p1.longitude > p2.longitude ? p1.longitude : p2.longitude;
        west = p1.longitude < p2.longitude ? p1.longitude : p2.longitude;
    }

    public void setUnits(unit units) {
        this.units = units;
    }

    public LatLng center() {
        return new LatLng((north+south)/2.0, (east+west)/2.0);
    }

    public PolygonOptions getPolyOptions(int color) {
        return new PolygonOptions()
                .add(new LatLng(north, east))
                .add(new LatLng(north, west))
                .add(new LatLng(south, west))
                .add(new LatLng(south, east))
                .strokeColor(color);
    }

    public LatLngBounds getBounds() {
        return new LatLngBounds(new LatLng(south, west), new LatLng(north, east));
    }

    public LatLng getSW() {
        return new LatLng(south, west);
    }

    public LatLng getSE() {
        return new LatLng(south, east);
    }

    public LatLng getNW() {
        return new LatLng(north, west);
    }

    public LatLng getNE() {
        return new LatLng(north, east);
    }

    public double width() {
        double latDistance = (west-east)*metersPerDegree;
        return (units==unit.METERS) ? latDistance : latDistance/metersPerFoot;
    }

    public double height() {
        double longDistance = (west-east)*metersPerDegree*Math.cos((north+south)/2);
        return (units==unit.METERS) ? longDistance : longDistance/metersPerFoot;
    }

    public double area() {
        double area = width()*height();
        return (units==unit.METERS) ? area : area/(metersPerFoot*metersPerFoot);
    }
}
