package org.waterapps.lib;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.openatk.openatklib.atkmap.ATKMap;

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
	//TODO does this run? It doesn't seem to work with readDemFileBounds
    public static void init() {
        gdal.AllRegister();
        for (int i = 0; i < gdal.GetDriverCount(); i++) {
        	if (gdal.GetDriver(i).getShortName().equals("GTiff")) {
        	}else {
        		gdal.GetDriver(i).Deregister();
        	}
        }
    }

    //Formerly ReadGeoTiff readFromFile
    /**
     * Use GDAL to read a geotiff into a DemData object
     * @param filename: filename of the geotiff to read
     * @return DemData
     */
    public static DemData readDemData(DemData demData, String filePath) {

        //TODO Test this filepath value
        Dataset dataset = gdal.Open(filePath);
        
        int xSize = dataset.getRasterXSize();
        int ySize = dataset.getRasterYSize();
        int count = xSize*ySize;
        
        // Read raster band from gdal dataset and remove nodata cells
        int[] bands = {1};
        float[] pixels = new float[count];
        dataset.ReadRaster(0, 0, xSize, ySize, xSize, ySize, gdalconstConstants.GDT_Float32, pixels, bands, 0, 0, 0);
        demData.setElevationData(oneDToTwoDArray(pixels, xSize, ySize, demData));
        Double [] nodata = new Double[1];
        dataset.GetRasterBand(1).GetNoDataValue(nodata);
        removeDemNoData(demData.getElevationData(), nodata[0].floatValue(), 5);
        
        float cellSize = (float) dataset.GetGeoTransform()[1];
        demData.setCellSize(cellSize);
        demData.setNoDataVal(nodata[0].floatValue());
        dataset.delete();
        dataset = null;
        return demData;
    }

    //Formerly readGeoTiffMetadata
    public static LatLngBounds readDemFileBounds(File file) {
        gdal.AllRegister();
        for (int i = 0; i < gdal.GetDriverCount(); i++) {
        	if (gdal.GetDriver(i).getShortName().equals("GTiff")) {
        	}else {
        		gdal.GetDriver(i).Deregister();
        	}
        }
    	
        Dataset dataset = gdal.Open(file.getPath());

        // Find southwest and northeast x,y coordinates and transform them to latitude and longitude to generate LatLngBounds
        SpatialReference srs = new SpatialReference(dataset.GetProjection());
        SpatialReference wgs = new SpatialReference("GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]");
        CoordinateTransformation coordTrans = osr.CreateCoordinateTransformation(srs, wgs);
        double[] ne = new double[3];
        double[] sw = new double[3];
        double scaleX = dataset.GetGeoTransform()[1];
        double scaleY = Math.abs(dataset.GetGeoTransform()[5]); // scaleY can be negative if it is an affine transformation
        double xSize = dataset.getRasterXSize();
        double ySize = dataset.getRasterYSize();
        coordTrans.TransformPoint(ne, dataset.GetGeoTransform()[0] + scaleX*xSize, dataset.GetGeoTransform()[3]);
        coordTrans.TransformPoint(sw, dataset.GetGeoTransform()[0], dataset.GetGeoTransform()[3] - ySize*scaleY);

        dataset.delete();
        dataset = null;
        LatLngBounds bounds = new LatLngBounds(new LatLng(sw[1], sw[0]), new LatLng(ne[1], ne[0])); 
        return bounds;
    }
    
    /**
     * Removes NoData cells from the DEM
     * @param array: array to convert into a 2D array 
     * @param xSize: x dimension of the desired output 2D array
     * @param ySize: y dimension of the desired output 2D array
     * @return DEM data
     */
    private static float[][] oneDToTwoDArray(float[] array, int xSize, int ySize, DemData demData) {
    	float[][] arrayOut = new float[xSize][ySize];
    	for(int i=0; i<xSize; i++) {
            for(int j=0; j<ySize; j++) {
                arrayOut[i][j] = array[i+(xSize*j)];
                if (arrayOut[i][j] < demData.getMinElevation()) demData.setMinElevation(arrayOut[i][j]);
                if (arrayOut[i][j] > demData.getMaxElevation()) demData.setMaxElevation(arrayOut[i][j]);
            }
        }
		return arrayOut;
    }
    
    
    /**
     * Removes NoData cells from the DEM by passing a window over each cell and estimating the value using inverse-distance weighting.   
     * @param array: array to remove NoData cells from 
     * @param noDataVal: NoData value to search for and remove from array.  Should be an odd number so that the window is of equal size on both sides.
     * @param windowSize: Size of the search window
     * @return DEM data
     */
	private static float[][] removeDemNoData(float[][] array, float noDataVal, int windowSize) {		
		int half = (windowSize - 1)/2;
		float distance;
		float weight;
		float[][] arrayOut = array;
		boolean noDataCellsRemaining = true;
		while (noDataCellsRemaining == true) {
			noDataCellsRemaining = false;
			for (int r = 0; r < array.length; r++) {
				for (int c = 0; c < array[0].length; c++) { 
					if (array[r][c] == noDataVal) {
						float weightsum = 0;
						float weightedvalsum = 0;
						for (int x = 0-half; x < 1 + half; x++) {
							for (int y = 0 - half; y < 1 + half; y++) {
								//skip the current cell
								if (x == 0 && y == 0) {
									continue;
								}
								//verify that the cell is in the DEM range
								if (r+y >= array.length || r+y < 0 || c+x >= array[0].length || c+x < 0) {
									continue;
								}
								//verify that the neighbor cell is not NoDATA, as this will break the IDW computation
								if (array[r+y][c+x] != noDataVal) {
									distance = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)); 
									weight = 1 / distance;
									weightsum += weight;
									weightedvalsum += array[r+y][c+x] * weight;
									arrayOut[r][c] = weightedvalsum/weightsum;

								}
							}
						}
						if (arrayOut[r][c] == noDataVal) {
							noDataCellsRemaining = true;
						}
					}
				}
			}
		}
		return arrayOut;
	}
}
