package com.precisionag.lib;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.tiffdecoder.TiffDecoder;

import java.io.File;
import java.net.URI;

import static com.tiffdecoder.TiffDecoder.nativeTiffGetCornerLatitude;
import com.ibm.util.CoordinateConversion;

/**
 * Created by steve on 5/30/13.
 */
public class ReadGeoTiff implements ReadElevationRaster {

    public ElevationRaster readFromFile(URI fileUri) {
        ElevationRaster raster = new ElevationRaster();
        raster.setMaxElevation(Float.NEGATIVE_INFINITY);
        raster.setMinElevation(Float.POSITIVE_INFINITY);
        TiffDecoder.nativeTiffOpen(fileUri.getPath());
        raster.setDimensions(TiffDecoder.nativeTiffGetWidth(), TiffDecoder.nativeTiffGetHeight());
        float[] pixels = TiffDecoder.nativeTiffGetFloats();
        raster.elevationData = new float[raster.getNrows()][raster.getNcols()];
        String noDataString = TiffDecoder.nativeTiffGetNoData();
        float nodata = Float.parseFloat(noDataString);
        //float nodata = -9999.0f;

        for(int i=0; i<raster.getNrows(); i++) {
            for(int j=0; j<raster.getNcols(); j++) {
                raster.elevationData[i][raster.getNcols()-1-j] = pixels[j+(raster.getNcols()*i)];
                //Log.e("elevation", Double.toString(raster.getElevationData()[i][raster.getNcols()-1-j]));
                if (raster.getElevationData()[i][raster.getNcols()-1-j] != nodata ) {
                    if (raster.getElevationData()[i][raster.getNcols()-1-j] < raster.getMinElevation()) raster.setMinElevation(raster.getElevationData()[i][raster.getNcols()-1-j]);
                    if (raster.getElevationData()[i][raster.getNcols()-1-j] > raster.getMaxElevation()) raster.setMaxElevation(raster.getElevationData()[i][raster.getNcols()-1-j]);
                }
                else {
                    raster.elevationData[i][raster.getNcols()-1-j] = raster.getMinElevation();
                }

            }
        }

        Log.i("min elevation", Float.toString(raster.getMinElevation()));
        Log.i("max elevation", Float.toString(raster.getMaxElevation()));
        //float []anchor = nativeTiffGetCornerLatitude();
        float longitude = TiffDecoder.nativeTiffGetCornerLongitude();
        float latitude = TiffDecoder.nativeTiffGetCornerLatitude();
        double latLng[];
        CoordinateConversion conversion = new CoordinateConversion();
        String UTM = TiffDecoder.nativeTiffGetParams();
        Log.d("params", UTM);
        String UTMZone = UTM.substring(18, 20).concat(" ").concat(UTM.substring(20, 21)).concat(" ");
        Log.d("utmzone", UTMZone);
        latLng = conversion.utm2LatLon(UTMZone + Integer.toString((int)longitude) + " " + Integer.toString((int)latitude));
        double scaleX = TiffDecoder.nativeTiffGetScaleX();
        double scaleY = TiffDecoder.nativeTiffGetScaleY();

        double width = scaleX*raster.getNrows()/(111111.0);
        double height = scaleY*raster.getNcols()/(111111.0*Math.cos(Math.toRadians(latLng[0])));
        raster.setLowerLeft(new LatLng(latLng[0]-width, latLng[1]));
        raster.setUpperRight(new LatLng(latLng[0], latLng[1]+height));
        return raster;
    }

}
