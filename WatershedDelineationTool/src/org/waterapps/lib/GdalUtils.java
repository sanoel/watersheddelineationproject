package org.waterapps.lib;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;
import org.waterapps.watershed.MainActivity;

/**
 * Created by Steve on 4/6/2014.
 */
public class GdalUtils {
    public static void init() {
//        System.loadLibrary("proj");
        gdal.AllRegister();
        for (int i = 0; i < gdal.GetDriverCount(); i++) {
        	if (gdal.GetDriver(i).getShortName().equals("GTiff")) {
        	}else {
        		gdal.GetDriver(i).Deregister();
        	}
        }
    }

    public static LatLngBounds getLatLngBounds(String filename) {
    	String filePath = MainActivity.demDirectory + "/" + filename;
        Dataset dataset = gdal.Open(filePath);
        if (dataset == null) {
            return new LatLngBounds(new LatLng(0,0), new LatLng(0,0));
        }
        return getLatLngBounds(dataset);
    }

    /**
     * returns the boundaries of a DEM in lat/long format
     * @param dataset filename of the DEM to be read
     * @return LatLngBounds representing the boundary of the DEM
     */

    public static LatLngBounds getLatLngBounds(Dataset dataset) {
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

        return new LatLngBounds(southWestLL, northEastLL);
    }
    
    public DemData readFromFile(String filename) {
        DemData raster = new DemData();

        //Gdal-related stuff
        gdal.AllRegister();
        for (int i = 0; i < gdal.GetDriverCount(); i++) {
        	if (gdal.GetDriver(i).getShortName().equals("GTiff")) {
        		Log.w("found it", "yep");
        	}else {
        		gdal.GetDriver(i).Deregister();
        	}
        }
        String filePath = MainActivity.demDirectory + "/" + filename;
        Dataset dataset = gdal.Open(filePath);
        
        // get DEM Lat Long Bounds
        LatLngBounds demBounds = GdalUtils.getLatLngBounds(dataset);
        int xsize = dataset.getRasterXSize();
        int ysize = dataset.getRasterYSize();
        int count = xsize*ysize;
        
        // Read Band data and copy into a float[][]
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
        
        //Transform upper left x,y to lat, long
        SpatialReference srs = new SpatialReference(dataset.GetProjection());
        SpatialReference wgs = new SpatialReference("GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]");
        CoordinateTransformation coordTrans = osr.CreateCoordinateTransformation(srs, wgs);
        double[] nw = new double[3];
        coordTrans.TransformPoint(nw, dataset.GetGeoTransform()[0], dataset.GetGeoTransform()[3]);       
        
        // Now find lower right x,y and transform to lat, long
        double[] se = new double[3];
        double scaleX = dataset.GetGeoTransform()[1];
        double scaleY = Math.abs(dataset.GetGeoTransform()[5]); // scaleY can be negative if it is an affine transformation
        double xSize = dataset.getRasterXSize();
        double ySize = dataset.getRasterYSize();
        coordTrans.TransformPoint(se, dataset.GetGeoTransform()[0] + scaleX*xSize, dataset.GetGeoTransform()[3] - ySize*scaleY);
        
        
        dataset.delete();
        dataset = null;
        return new DemFile(new LatLng(se[0], se[1]), new LatLng(nw[0], nw[1]), file.getName(), timeStamp, fileUri);
    }
}
