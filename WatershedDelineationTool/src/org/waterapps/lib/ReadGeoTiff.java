package org.waterapps.lib;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.waterapps.watershed.MainActivity;

import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Reads data from a GeoTIFF into a DemData object
 */
public class ReadGeoTiff implements ReadDemData {

    /**
     * Reads data from a GeoTIFF into a DemData object
     * @param filename File location to be read
     * @return The DEM data
     */
    public DemData readFromFile(String filename) {
        DemData raster = new DemData();

        //Gdal-related stuff
        gdal.AllRegister();
        for (int i = 0; i < gdal.GetDriverCount(); i++) {
        	if (gdal.GetDriver(i).getShortName().equals("GTiff")) {
        	}else {
        		gdal.GetDriver(i).Deregister();
        	}
        }
        String filePath = MainActivity.demDirectory + "/" + filename;
        Dataset dataset = gdal.Open(filePath);
        LatLngBounds demBounds = GdalUtils.getLatLngBounds(dataset);
        int xsize = dataset.getRasterXSize();
        int ysize = dataset.getRasterYSize();
        int count = xsize*ysize;
        
        
        int[] bands = {1};
        raster.setElevationData(new float[xsize][ysize]);
        raster.setNcols(ysize);
        raster.setNrows(xsize);
        float[] pixels = new float[count];

        //read raster band from gdal dataset
        dataset.ReadRaster(0, 0, xsize, ysize, xsize, ysize, gdalconstConstants.GDT_Float32, pixels, bands, 0, 0, 0);

        Double [] nodata_tmp = new Double[1];
        dataset.GetRasterBand(1).GetNoDataValue(nodata_tmp);
        float cellSize = (float) dataset.GetGeoTransform()[1];
        float nodata = nodata_tmp[0].floatValue();
        float lastGoodElevation = 0.0f;
        float value;
        //loop through each pixel, calculating min and max along the way and not setting nodata points
        for(int i=0; i<xsize; i++) {
            for(int j=0; j<ysize; j++) {
                value = pixels[i+(xsize*j)];
                raster.elevationData[i][j] = value;
                if (raster.getElevationData()[i][j] != nodata ) {
                    lastGoodElevation = raster.elevationData[i][j];
//                    if (raster.getElevationData()[i][j] < raster.getMinElevation()) raster.setMinElevation(raster.getElevationData()[i][j]);
//                    if (raster.getElevationData()[i][j] > raster.getMaxElevation()) raster.setMaxElevation(raster.getElevationData()[i][j]);
                }
                else {
                    raster.elevationData[i][raster.getNcols()-1-j] = lastGoodElevation;
                }

            }
        }

        raster.setSouthWest(demBounds.southwest);
        raster.setNorthEast(demBounds.northeast);
        raster.setCellSize(cellSize);
        raster.setNoDataVal(nodata);
        dataset.delete();
        dataset = null;
        return raster;
    }

}



//package com.precisionag.lib;
//
//import com.google.android.gms.maps.model.LatLng;
//import com.tiffdecoder.TiffDecoder;
//
//import java.net.URI;
//
//import com.ibm.util.CoordinateConversion;
//
///**
// * Created by steve on 5/30/13.
// */
//public class ReadGeoTiff implements ReadElevationRaster {
//
//    public ElevationRaster readFromFile(URI fileUri) {
//        ElevationRaster raster = new ElevationRaster();
//        raster.setMaxElevation(Float.NEGATIVE_INFINITY);
//        raster.setMinElevation(Float.POSITIVE_INFINITY);
//        TiffDecoder.nativeTiffOpen(fileUri.getPath());
//        raster.setDimensions(TiffDecoder.nativeTiffGetWidth(), TiffDecoder.nativeTiffGetHeight());
//        float[] pixels = TiffDecoder.nativeTiffGetFloats();
//        raster.elevationData = new float[raster.getNrows()][raster.getNcols()];
//        String noDataString = TiffDecoder.nativeTiffGetNoData();
//        float nodata = Float.parseFloat(noDataString);
//        //float nodata = -9999.0f;
//
//        for(int i=0; i<raster.getNrows(); i++) {
//            for(int j=0; j<raster.getNcols(); j++) {
//                raster.elevationData[i][raster.getNcols()-1-j] = pixels[j+(raster.getNcols()*i)];
//                //Log.e("elevation", Double.toString(raster.getElevationData()[i][raster.getNcols()-1-j]));
//                if (raster.getElevationData()[i][raster.getNcols()-1-j] != nodata ) {
//                    if (raster.getElevationData()[i][raster.getNcols()-1-j] < raster.getMinElevation()) raster.setMinElevation(raster.getElevationData()[i][raster.getNcols()-1-j]);
//                    if (raster.getElevationData()[i][raster.getNcols()-1-j] > raster.getMaxElevation()) raster.setMaxElevation(raster.getElevationData()[i][raster.getNcols()-1-j]);
//                }
//                else {
//                    raster.elevationData[i][raster.getNcols()-1-j] = raster.getMinElevation();
//                }
//
//            }
//        }
//
////        Log.i("max elevation", Float.toString(raster.getMaxElevation()));
//        //float []anchor = nativeTiffGetCornerLatitude();
//        float longitude = TiffDecoder.nativeTiffGetCornerLongitude();
//        float latitude = TiffDecoder.nativeTiffGetCornerLatitude();
//        double latLng[];
//        CoordinateConversion conversion = new CoordinateConversion();
//        String UTM = TiffDecoder.nativeTiffGetParams();
////        Log.d("params", UTM);
//        String UTMZone = UTM.substring(18, 20).concat(" ").concat(UTM.substring(20, 21)).concat(" ");
////        Log.d("utmzone", UTMZone);
//        latLng = conversion.utm2LatLon(UTMZone + Integer.toString((int)longitude) + " " + Integer.toString((int)latitude));
//        double scaleX = TiffDecoder.nativeTiffGetScaleX();
//        double scaleY = TiffDecoder.nativeTiffGetScaleY();
//
//        double width = scaleX*raster.getNrows()/(111111.0);
//        double height = scaleY*raster.getNcols()/(111111.0*Math.cos(Math.toRadians(latLng[0])));
//        raster.setSouthWest(new LatLng(latLng[0]-width, latLng[1]));
//        raster.setNorthEast(new LatLng(latLng[0], latLng[1]+height));
//        return raster;
//    }
//
//}
