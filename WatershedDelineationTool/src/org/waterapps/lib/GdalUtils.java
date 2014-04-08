package org.waterapps.lib;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

/**
 * Created by Steve on 4/6/2014.
 */
public class GdalUtils {
    /**
     * returns the boundaries of a DEM in lat/long format
     * @param filename filename of the DEM to be read
     * @return LatLngBounds representing the boundary of the DEM
     */
    public static LatLngBounds getLatLngBounds(String filename) {
        //initialize GDAL by loading drivers
//        gdal.AllRegister();

        //open up the DEM as a GDAL dataset
        Dataset dataset = gdal.Open(filename);

        //extract parameters from the DEM
        double north = dataset.GetGeoTransform()[3];
        double west = dataset.GetGeoTransform()[0];
        double south = north - (dataset.getRasterYSize() * dataset.GetGeoTransform()[1]);
        double east = west + (dataset.getRasterXSize() * dataset.GetGeoTransform()[1]);

        //setup coordiante transformation from DEM projection -> latitude/longitude
        SpatialReference inputSR = new SpatialReference(dataset.GetProjection());
        SpatialReference wgsSR = new SpatialReference("GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]");
        CoordinateTransformation trans = CoordinateTransformation.CreateCoordinateTransformation(inputSR, wgsSR);

        //perform coordinate transformation
        double[] northEast = trans.TransformPoint(east, north);
        double[] southWest = trans.TransformPoint(west, south);

        //create LatLng objects for the return
        LatLng northEastLL = new LatLng(northEast[1], northEast[0]);
        LatLng southWestLL = new LatLng(southWest[1], southWest[0]);
        dataset.delete();

        return new LatLngBounds(southWestLL, northEastLL);
    }
}