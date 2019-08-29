package org.waterapps.lib;

import java.io.File;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;
import android.util.Log;

/**
 * Created by Steve on 4/6/2014.
 */
public class GdalUtils {
	//TODO does this run? It doesn't seem to work with readDemFileBounds
    public static void init() {
        gdal.AllRegister();
        for (int i = 0; i < gdal.GetDriverCount(); i++) {
        	if (gdal.GetDriver(i).getShortName().equals("GTiff")) {
            } else if (gdal.GetDriver(i).getShortName().equals("AAIGrid")) {
            } else if (gdal.GetDriver(i).getShortName().equals("AIG")) {
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
    public static DemData readDemData(DemData demData) {

        //TODO Test this filepath value
        Dataset dataset = gdal.Open(demData.getFilePath());
        
        int xSize = dataset.getRasterXSize();
        int ySize = dataset.getRasterYSize();
        int count = xSize*ySize;
        
        // Read raster band from gdal dataset and remove nodata cells
        int[] bands = {1};
        float[] pixels = new float[count];
        
        dataset.ReadRaster(0, 0, xSize, ySize, xSize, ySize, gdalconstConstants.GDT_Float32, pixels, bands, 0, 0, 0);
        Double [] nodata = new Double[1];
        dataset.GetRasterBand(1).GetNoDataValue(nodata);
        demData.setNoDataVal(nodata[0].floatValue());
        demData.setElevationData(oneDToTwoDArray(pixels, xSize, ySize, demData));
        removeDemNoData(demData.getElevationData(), nodata[0].floatValue(), 5);
        
        float cellSize = (float) dataset.GetGeoTransform()[1];
        demData.setCellSize(cellSize);

        dataset.delete();
        dataset = null;
        return demData;
    }

    //Formerly readGeoTiffMetadata
    public static LatLngBounds readDemFileBounds(File file) {
        gdal.AllRegister();
        for (int i = 0; i < gdal.GetDriverCount(); i++) {
        	if (gdal.GetDriver(i).getShortName().equals("GTiff")) {
        	} else if (gdal.GetDriver(i).getShortName().equals("AAIGrid")) {
            } else if (gdal.GetDriver(i).getShortName().equals("AIG")) {
            } else {
        		gdal.GetDriver(i).Deregister();
        	}
        }

        try {

            Dataset dataset = gdal.Open(file.getPath());

            Log.w("FILE NAME", file.getPath());


            // Find southwest and northeast x,y coordinates and transform them to latitude and longitude to generate LatLngBounds
            SpatialReference srs = new SpatialReference(dataset.GetProjection());


            SpatialReference tgt = new SpatialReference("GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]");
            Log.w("source", srs.__str__());
            Log.w("target", tgt.__str__());

            CoordinateTransformation coordTrans = osr.CreateCoordinateTransformation(srs, tgt);
            Log.w("TRANSFORMATION", Boolean.toString(coordTrans == null));
            if (coordTrans == null) {
                // Handle Ohio State data
                if (srs.toString().contains("Ohio North")) {
                    Log.w("OHIO", "NORTH");
                    srs = new SpatialReference("PROJCS[\"NAD83(NSRS2007) / Ohio North (ftUS)\",GEOGCS[\"NAD83(NSRS2007)\",DATUM[\"NAD83_National_Spatial_Reference_System_2007\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY[\"EPSG\",\"6759\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4759\"]],UNIT[\"US survey foot\",0.3048006096012192,AUTHORITY[\"EPSG\",\"9003\"]],PROJECTION[\"Lambert_Conformal_Conic_2SP\"],PARAMETER[\"standard_parallel_1\",41.7],PARAMETER[\"standard_parallel_2\",40.43333333333333],PARAMETER[\"latitude_of_origin\",39.66666666666666],PARAMETER[\"central_meridian\",-82.5],PARAMETER[\"false_easting\",1968500],PARAMETER[\"false_northing\",0],AUTHORITY[\"EPSG\",\"3728\"],AXIS[\"X\",EAST],AXIS[\"Y\",NORTH]]");
                } else if (srs.toString().contains("HARN_StatePlane_Ohio_North")) {
                    Log.w("OHIO", "NORTH2");
                    srs = new SpatialReference("PROJCS[\"NAD83(HARN) / Ohio North (ftUS)\",GEOGCS[\"NAD83(HARN)\",DATUM[\"NAD83_High_Accuracy_Regional_Network\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],AUTHORITY[\"EPSG\",\"6152\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4152\"]],UNIT[\"US survey foot\",0.3048006096012192,AUTHORITY[\"EPSG\",\"9003\"]],PROJECTION[\"Lambert_Conformal_Conic_2SP\"],PARAMETER[\"standard_parallel_1\",41.7],PARAMETER[\"standard_parallel_2\",40.43333333333333],PARAMETER[\"latitude_of_origin\",39.66666666666666],PARAMETER[\"central_meridian\",-82.5],PARAMETER[\"false_easting\",1968500],PARAMETER[\"false_northing\",0],AUTHORITY[\"EPSG\",\"3753\"],AXIS[\"X\",EAST],AXIS[\"Y\",NORTH]]");
                } else if (srs.toString().contains("Ohio South")) {
                    srs = new SpatialReference("PROJCS[\"NAD83 / Ohio South (ftUS)\",GEOGCS[\"NAD83\",DATUM[\"North_American_Datum_1983\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],AUTHORITY[\"EPSG\",\"6269\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4269\"]],UNIT[\"US survey foot\",0.3048006096012192,AUTHORITY[\"EPSG\",\"9003\"]],PROJECTION[\"Lambert_Conformal_Conic_2SP\"],PARAMETER[\"standard_parallel_1\",40.03333333333333],PARAMETER[\"standard_parallel_2\",38.73333333333333],PARAMETER[\"latitude_of_origin\",38],PARAMETER[\"central_meridian\",-82.5],PARAMETER[\"false_easting\",1968500],PARAMETER[\"false_northing\",0],AUTHORITY[\"EPSG\",\"3735\"],AXIS[\"X\",EAST],AXIS[\"Y\",NORTH]]");
                } else if (srs.toString().contains("HARN_StatePlane_Ohio_North")) {
                    srs = new SpatialReference("PROJCS[\"NAD83(HARN) / Ohio South (ftUS)\",GEOGCS[\"NAD83(HARN)\",DATUM[\"NAD83_High_Accuracy_Regional_Network\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],AUTHORITY[\"EPSG\",\"6152\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4152\"]],UNIT[\"US survey foot\",0.3048006096012192,AUTHORITY[\"EPSG\",\"9003\"]],PROJECTION[\"Lambert_Conformal_Conic_2SP\"],PARAMETER[\"standard_parallel_1\",40.03333333333333],PARAMETER[\"standard_parallel_2\",38.73333333333333],PARAMETER[\"latitude_of_origin\",38],PARAMETER[\"central_meridian\",-82.5],PARAMETER[\"false_easting\",1968500],PARAMETER[\"false_northing\",0],AUTHORITY[\"EPSG\",\"3754\"],AXIS[\"X\",EAST],AXIS[\"Y\",NORTH]]");
                }
                Log.w("source", srs.__str__());
                coordTrans = osr.CreateCoordinateTransformation(srs, tgt);
            }

            Log.w("TRANSFORMATION", coordTrans.toString());
            double[] ne = new double[3];
            double[] sw = new double[3];
            double scaleX = dataset.GetGeoTransform()[1];
            double scaleY = Math.abs(dataset.GetGeoTransform()[5]); // scaleY can be negative if it is an affine transformation
            double xSize = dataset.getRasterXSize();
            double ySize = dataset.getRasterYSize();
            coordTrans.TransformPoint(ne, dataset.GetGeoTransform()[0] + scaleX * xSize, dataset.GetGeoTransform()[3]);
            coordTrans.TransformPoint(sw, dataset.GetGeoTransform()[0], dataset.GetGeoTransform()[3] - ySize * scaleY);

            dataset.delete();
            dataset = null;
            LatLngBounds bounds = new LatLngBounds(new LatLng(sw[1], sw[0]), new LatLng(ne[1], ne[0]));
            return bounds;
        } catch (java.lang.NullPointerException npe) {
            return null;
        }
    }
    
    /**
     * Removes NoData cells from the DEM
     * @param array: array to convert into a 2D array 
     * @param xSize: x dimension of the desired output 2D array
     * @param ySize: y dimension of the desired output 2D array
     * @return DEM data
     */
    private static float[][] oneDToTwoDArray(float[] array, int xSize, int ySize, DemData demData) {
    	float[][] arrayOut = new float[ySize][xSize];
    	for(int c = 0; c < xSize; c++) {
            for(int r = 0; r < ySize; r++) {
                arrayOut[r][c] = array[(xSize - 1 - c)+(xSize*r)];
                if (arrayOut[r][c] != demData.getNoDataVal()) {
                	if (arrayOut[r][c] < demData.getMinElevation()) demData.setMinElevation(arrayOut[r][c]);
                	if (arrayOut[r][c] > demData.getMaxElevation()) demData.setMaxElevation(arrayOut[r][c]);
                }
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
