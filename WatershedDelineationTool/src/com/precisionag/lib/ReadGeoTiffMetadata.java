package com.precisionag.lib;

import android.net.Uri;

import com.ibm.util.CoordinateConversion;
import com.tiffdecoder.TiffDecoder;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;

/**
 * Created by steve on 6/7/13.
 */
public class ReadGeoTiffMetadata {
    public static DemFile readMetadata(File file) {
        String fileName = file.getPath();
        DateFormat df = DateFormat.getDateInstance();
        String timeStamp = df.format(file.lastModified());
        URI fileUri = URI.create(Uri.fromFile(file).toString());
        float sw_lat = 0.0f, sw_long = 0.0f, ne_lat = 0.0f, ne_long = 0.0f;

        TiffDecoder.nativeTiffOpen(fileUri.getPath());

        float longitude = TiffDecoder.nativeTiffGetCornerLongitude();
        float latitude = TiffDecoder.nativeTiffGetCornerLatitude();
        double latLng[];
        CoordinateConversion conversion = new CoordinateConversion();
        latLng = conversion.utm2LatLon("16 N " + Integer.toString((int)longitude) + " " + Integer.toString((int)latitude));
        double scaleX = TiffDecoder.nativeTiffGetScaleX();
        double scaleY = TiffDecoder.nativeTiffGetScaleY();

        double width = scaleX*TiffDecoder.nativeTiffGetHeight()/(111111.0);
        double height = scaleY*TiffDecoder.nativeTiffGetWidth()/(111111.0*Math.cos(Math.toRadians(latLng[0])));

        return new DemFile((float)(latLng[0]-width), ((float)latLng[1]), (float)latLng[0], (float)(latLng[1]+height), fileName, timeStamp, fileUri);
    }
}
