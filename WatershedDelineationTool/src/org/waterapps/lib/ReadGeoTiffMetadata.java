package org.waterapps.lib;

import android.net.Uri;

//import com.ibm.util.CoordinateConversion;
//import com.tiffdecoder.TiffDecoder;


import android.util.Log;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by steve on 6/7/13.
 */
public class ReadGeoTiffMetadata {
//    public static DemFile readMetadata(File file) {
//        String fileName = file.getPath();
//        DateFormat df = DateFormat.getDateInstance();
//        String timeStamp = df.format(file.lastModified());
//        URI fileUri = URI.create(Uri.fromFile(file).toString());
//        float sw_lat = 0.0f, sw_long = 0.0f, ne_lat = 0.0f, ne_long = 0.0f;
//
//        TiffDecoder.nativeTiffOpen(fileUri.getPath());
//
//        float longitude = TiffDecoder.nativeTiffGetCornerLongitude();
//        float latitude = TiffDecoder.nativeTiffGetCornerLatitude();
//        double latLng[];
//        CoordinateConversion conversion = new CoordinateConversion();
//        latLng = conversion.utm2LatLon("16 N " + Integer.toString((int)longitude) + " " + Integer.toString((int)latitude));
//        double scaleX = TiffDecoder.nativeTiffGetScaleX();
//        double scaleY = TiffDecoder.nativeTiffGetScaleY();
//
//        double width = scaleX*TiffDecoder.nativeTiffGetHeight()/(111111.0);
//        double height = scaleY*TiffDecoder.nativeTiffGetWidth()/(111111.0*Math.cos(Math.toRadians(latLng[0])));
//
//        return new DemFile((float)(latLng[0]-width), ((float)latLng[1]), (float)latLng[0], (float)(latLng[1]+height), fileName, timeStamp, fileUri);
//    }
    
    public static DemFile readMetadata(File file) {
        DateFormat df = DateFormat.getDateInstance();
        String timeStamp = df.format(file.lastModified());
        URI fileUri = URI.create(Uri.fromFile(file).toString());

        //gdal stuff
        gdal.AllRegister();
        for (int i = 0; i < gdal.GetDriverCount(); i++) {
        	if (gdal.GetDriver(i).getShortName().equals("GTiff")) {
        	}else {
        		gdal.GetDriver(i).Deregister();
        	}
        }
        Dataset dataset = gdal.Open(file.getPath());
        
     // Find southwest and northeast lat long coordinates.  Start with given northwest (x,y) from getgeotransform and use scale and size to get other (x,y)s then transform to WGS8
        SpatialReference srs = new SpatialReference(dataset.GetProjection());
        SpatialReference wgs = new SpatialReference("GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]");
        CoordinateTransformation coordTrans = osr.CreateCoordinateTransformation(srs, wgs);
        double[] sw = new double[3];
        double[] ne = new double[3];
        double scaleX = dataset.GetGeoTransform()[1];
        double scaleY = Math.abs(dataset.GetGeoTransform()[5]); // scaleY can be negative if it is an affine transformation
        double xSize = dataset.getRasterXSize();
        double ySize = dataset.getRasterYSize();
        coordTrans.TransformPoint(sw, dataset.GetGeoTransform()[0], dataset.GetGeoTransform()[3] - ySize*scaleY);
        coordTrans.TransformPoint(ne, dataset.GetGeoTransform()[0] + scaleX*xSize, dataset.GetGeoTransform()[3]);
        
        dataset.delete();
        dataset = null;
        return new DemFile(new LatLng(sw[0], sw[1]), new LatLng(ne[0], ne[1]), file.getName(), timeStamp, fileUri);
    }

}
