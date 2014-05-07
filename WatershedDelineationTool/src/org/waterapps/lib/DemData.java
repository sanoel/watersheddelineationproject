package org.waterapps.lib;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;
import android.widget.SeekBar;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Collections;
//import java.util.Iterator;
//import java.util.List;

//import org.waterapps.watershed.MainActivity;

/**
 * Stores data read in from a DEM, as both raw floats and a bitmap representation.
 */
public class DemData {
    private int ncols;
    private int nrows;
    public float[][] elevationData;
    private static float minElevation;
    private static float maxElevation;
    private static LatLng sw;
    private static LatLng ne;
//    private static boolean currentlyDrawing = false;
    //bitmap represents rasterized elevation data
    private static Bitmap elevationBitmap;
    static Polyline polyline;

    //defines the edges of the field
    private static LatLngBounds demBounds;

    static MapFragment mapFragment;
    public static SeekBar seekBar;
    public static GroundOverlay prevoverlay;
    float cellSize;
    float noDataVal;

    public DemData(Bitmap bitmap, LatLng southwest, LatLng northeast, double minHeight, double maxHeight) {
        setElevationBitmap(bitmap);
        sw = southwest;
        ne = northeast;
//        setDemBounds(new LatLngBounds(sw, ne));
//        setMinElevation((float)minHeight);
//        setMaxElevation((float)maxHeight);
        prevoverlay = createOverlay(bitmap, getDemBounds());
    }

    public DemData(int w, int h, float [][]data) {
//        setMinElevation(Float.POSITIVE_INFINITY);
//        setMaxElevation(Float.NEGATIVE_INFINITY);
        setNcols(w);
        setNrows(h);
        setElevationData(data);
    }

    public DemData(int w, int h) {
//        setMinElevation(Float.POSITIVE_INFINITY);
//        setMaxElevation(Float.NEGATIVE_INFINITY);
        setNcols(w);
        setNrows(h);
        setElevationData(new float[getNcols()][getNrows()]);
    }

    public DemData() {
//        setMinElevation(Float.POSITIVE_INFINITY);
//        setMaxElevation(Float.NEGATIVE_INFINITY);
        setNcols(1);
        setNrows(1);
        setElevationData(new float[getNcols()][getNrows()]);
    }

    public void setDimensions(int w, int h) {
        setNcols(w);
        setNrows(h);
        setElevationData(new float[getNcols()][getNrows()]);
    }

    public float[][] getData() {
        return getElevationData();
    }

    public LatLngBounds getBounds() {
        return new LatLngBounds(getLowerLeft(), getUpperRight());
    }

    /**
     * Calculates the center of the DEM area
     * @return Calculated center
     */
    public LatLng getCenter() {
        LatLngBounds bounds = getBounds();
        LatLng sw = bounds.southwest;
        LatLng ne = bounds.northeast;
        double newLat = (sw.latitude + ne.latitude)/2;
        double newLong = (sw.longitude + ne.longitude)/2;
        return new LatLng(newLat, newLong);
    }

    /**
     * Generates a bitmap from the raw float data
     * @return Generated bitmap
     */
    public Bitmap getBitmap() {
        Log.i("bitmap", "bitmap being created");
        Bitmap bitmap;
        int intpixels[] = new int[getNrows()*getNcols()];

        Log.i("min value", Double.toString(getMinElevation()));
        Log.i("max value", Double.toString(getMaxElevation()));
        Log.i("range value", Double.toString(getMaxElevation()-getMinElevation()));
        for(int k = 0; k<getNrows(); k++) {
            for(int m=0; m<getNcols(); m++) {
                //normalize each float to a value from 0-255

                double range = 255/(getMaxElevation()-getMinElevation());
                intpixels[k+(m*getNrows())] = (int)(range*(getElevationData()[k][m]-getMinElevation()));
            }
        }

        for(int l=0;l<getNcols()*getNrows(); l++) {
            //intpixels[l] = 0xFF000000 + intpixels[l] + intpixels[l]<<8 + intpixels[l]<<16;
            intpixels[l] = Color.argb(255, intpixels[l], intpixels[l], intpixels[l]);
        }
        bitmap = Bitmap.createBitmap(intpixels, 0, getNrows(), getNrows(), getNcols(), Bitmap.Config.ARGB_8888);

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);

        return bitmap;
    }


    public float [][] getElevationData() {
        return elevationData;
    }

    public void setElevationData(float [][] elevationData) {
        this.elevationData = elevationData;
    }

//    public void setMaxElevation(float maxElevation) {
//        this.maxElevation = maxElevation;
//    }
//
//    public void setMinElevation(float minElevation) {
//        this.minElevation = minElevation;
//    }

    public int getNcols() {
        return ncols;
    }

    public void setNcols(int ncols) {
        this.ncols = ncols;
    }

    public int getNrows() {
        return nrows;
    }

    public void setNrows(int nrows) {
        this.nrows = nrows;
    }

    public LatLng getLowerLeft() {
        return sw;
    }

//    public void setLowerLeft(LatLng lowerLeft) {
//        this.sw = lowerLeft;
//    }

    public LatLng getUpperRight() {
        return ne;
    }

//    public void setUpperRight(LatLng upperRight) {
//        this.ne = upperRight;
//    }

    /**
     * Calculates the upper and lower slider values to set for the field, based on upper/lower 3% of data
     */
    public void calculateTenths() {
        int size = nrows*ncols;
        float []tempArray = new float[size];
        for(int i=0; i<nrows; i++) {
            for(int j=0; j<ncols; j++) {
                tempArray[i+(nrows*j)] = elevationData[i][j];
            }
        }
        Arrays.sort(tempArray);
//        float min = tempArray[(size/100)*3];
//        float max = tempArray[(size/100)*97];
//        MainActivity.sliderMin = min;
//        MainActivity.sliderMax = max;
//        MainActivity.updateEditText(min, max);
    }

    //access methods
    public static void setMapFragment(MapFragment map) {
        mapFragment = map;
    }

    public static void setSeekBar(SeekBar bar) {
        seekBar = bar;
    }

    public void setBitmap(Bitmap bits) {
        setElevationBitmap(bits);
    }

    public void setBounds(LatLngBounds bounds) {
//        setDemBounds(bounds);
        ne = bounds.northeast;
        sw = bounds.southwest;
    }

    public void setNorthEast(LatLng northeast) {
        ne = northeast;
    }

    public void setSouthWest(LatLng southwest) {
        sw = southwest;
    }

    /**
     * Creates an overlay view of the field on the specified map object
     * @param map Map to overlay on
     * @return Generated overlay
     */
    public static GroundOverlay createOverlay(GoogleMap map) {
        PolylineOptions rectOptions = new PolylineOptions()
                .add(new LatLng(sw.latitude, ne.longitude))
                .add(sw)
                .add(new LatLng(ne.latitude, sw.longitude))
                .add(ne)
                .add(new LatLng(sw.latitude, ne.longitude)); // Closes the polyline.
        polyline = mapFragment.getMap().addPolyline(rectOptions);

        GroundOverlay groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(getElevationBitmap()))
                .positionFromBounds(getDemBounds())
                .transparency(0));
        groundOverlay.setVisible(true);
        return groundOverlay;
    }

    /**
     * Draws a PolyLine around the boundaries of the DEM data
     * @return Generated PolyLine
     */
    public Polyline updatePolyLine() {
        if(polyline != null) {
            polyline.remove();
        }
        PolylineOptions rectOptions = new PolylineOptions()
                .add(new LatLng(sw.latitude, ne.longitude))
                .add(sw)
                .add(new LatLng(ne.latitude, sw.longitude))
                .add(ne)
                .add(new LatLng(sw.latitude, ne.longitude))
                .zIndex(1.0f); // Closes the polyline.
        return polyline = mapFragment.getMap().addPolyline(rectOptions);
    }

    /**
     * Gets elevation of a point
     * @param point Location to get elevation from
     * @return Elevation of the point
     */
    public double elevationFromLatLng(LatLng point) {

        if (getDemBounds().contains(point)) {
            //use linear interpolation to figure out which pixel to get data from
            //should be accurate since fields <= ~1 mile wide
            double north = ne.latitude;
            double east = ne.longitude;
            double south = sw.latitude;
            double west = sw.longitude;

            int width = getElevationBitmap().getWidth();
            int height = getElevationBitmap().getHeight();

            double x = (double) width*(point.longitude-west)/(east-west);
            double y = (double) height*(north - point.latitude)/(north-south);

            Log.d("x", Double.toString(x));
            //retrieve packed int
            int waterLevel = getElevationBitmap().getPixel((int)x, (int)y);

            //pixels are represented as packed ARGB, so discard all but blue channel
            //this gives range of 0-255
            waterLevel &= 0x000000FF;

            //convert 0-255 pixel data to elevation float
            double waterLevelMeters = getMinElevation() + ((double)waterLevel*(maxElevation-getMinElevation())/255.0);
            return waterLevelMeters;

        }
        else {
            //point isn't in the field
            return 0.0;
        }
    }

//    ElevationPoint elevationPointFromPixel(int x, int y) {
//        double north = ne.latitude;
//        double east = ne.longitude;
//        double south = sw.latitude;
//        double west = sw.longitude;
//        double longitude = west+(((double)y/(double)nrows)*(east-west));
//        double latitude = south+(((double)x/(double)ncols)*(north-south));
//        LatLng location = new LatLng(latitude, longitude);
//
//        Log.d("blarg north", Double.toString(north));
//        Log.d("blarg south", Double.toString(south));
//        Log.d("blarg east", Double.toString(east));
//        Log.d("blarg west", Double.toString(west));
//        Log.d("blarg lat", Double.toString(latitude));
//        Log.d("blarg long", Double.toString(longitude));
//        return new ElevationPoint(elevationFromLatLng(location), location);
//    }

    /**
     * Creates a Google Maps overlay of the given bitmap at the given location
     * @param overlayBitmap Bitmap to draw
     * @param bounds Location to draw
     * @return The generated Google Maps GroundOverlay object
     */
    private GroundOverlay createOverlay(Bitmap overlayBitmap, LatLngBounds bounds) {
        BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(overlayBitmap);
        GoogleMap map = mapFragment.getMap();
        GroundOverlay groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                .image(image)
                .positionFromBounds(bounds)
                .transparency(0));
        groundOverlay.setVisible(true);
        return groundOverlay;
    }

    public static double getMinElevation() {
        return minElevation;
    }

    public static double getMaxElevation() {
        return maxElevation;
    }

    public static Bitmap getElevationBitmap() {
        return elevationBitmap;
    }

    public void setElevationBitmap(Bitmap elevationBitmap) {
        this.elevationBitmap = elevationBitmap;
    }

    public static LatLngBounds getDemBounds() {
        return demBounds;
    }

	public void setNoDataVal(float noDataVal) {
		this.noDataVal = noDataVal;
	}
	
	public float getNoDataVal() {
		return noDataVal;
	}
	
	public float getCellSize() {
		return cellSize;
	}
	
	public void setCellSize(float cellSize) {
		this.cellSize = cellSize;
	}
//    /**
//     * Sets the bounds of the DEM
//     * @param demBounds
//     */
//    public void setDemBounds(LatLngBounds demBounds) {
//        this.demBounds = demBounds;
//    }
//
//    /**
//     * Finds elevation at every pixel in DEM between points
//     * @param p1 Point 1
//     * @param p2 Point 2
//     * @return list of elevations connecting the points
//     */
//    public List<ElevationPoint> getLineElevations(LatLng p1, LatLng p2) {
//        ElevationPoint min;
//        ElevationPoint max;
//        List<ElevationPoint> values = new ArrayList<ElevationPoint>();
//        int x1 = 1, x2 = 1, y1 = 1, y2 = 1;
//        if (getDemBounds().contains(p1) && getDemBounds().contains(p2)) {
//            //use linear interpolation to figure out which pixel to get data from
//            //should be accurate since fields <= ~1 mile wide
//            double north = ne.longitude;
//            double east = ne.latitude;
//            double south = sw.longitude;
//            double west = sw.latitude;
//
//            int width = getElevationBitmap().getWidth();
//            int height = getElevationBitmap().getHeight();
//
//            x1 = (int)((double)width*(p1.latitude-west)/(east-west));
//            y1 = (int)((double)height*(p1.longitude-south)/(north-south));
//            x2 = (int)((double)width*(p2.latitude-west)/(east-west));
//            y2 = (int)((double)height*(p2.longitude-south)/(north-south));
//            System.out.println(x1);
//            System.out.println(x2);
//            System.out.println(y1);
//            System.out.println(y2);
//            System.out.println(width);
//            System.out.print(height);
//        }
//        int dx = Math.abs(x2 - x1);
//        int dy = Math.abs(y2 - y1);
//
//        int sx = (x1 < x2) ? 1 : -1;
//        int sy = (y1 < y2) ? 1 : -1;
//
//        int err = dx - dy;
//
//        while (true) {
//            values.add(elevationPointFromPixel(x1, y1));
//
//            if (x1 == x2 && y1 == y2) {
//                break;
//            }
//
//            int e2 = 2 * err;
//
//            if (e2 > -dy) {
//                err = err - dy;
//                x1 = x1 + sx;
//            }
//
//            if (e2 < dx) {
//                err = err + dx;
//                y1 = y1 + sy;
//            }
//        }
//
//        return values;
//    }

//    /**
//     * Finds elevation at every pixel in DEM along polyline
//     * @param points Points in polyline
//     * @return list of elevations connecting the points
//     */
//    public List<ElevationPoint> getLineElevations(List<LatLng> points) {
//        Iterator<LatLng> iter = points.iterator();
//        LatLng point1, point2;
//        ArrayList<ElevationPoint> elevationPoints = new ArrayList<ElevationPoint>();
//        if(iter.hasNext()) {
//            point1 = iter.next();
//        }
//        else return null;
//        if(iter.hasNext()) {
//            point2 = iter.next();
//        }
//        else return null;
//        elevationPoints.addAll(getLineElevations(point1, point2));
//        while (iter.hasNext()) {
//            point1 = point2;
//            point2 = iter.next();
//            elevationPoints.addAll(getLineElevations(point1, point2));
//        }
//        return elevationPoints;
//    }
//
//    /**
//     * Finds the lowest elevation on the line connecting the two points
//     * @param p1
//     * @param p2
//     * @return
//     */
//    public ElevationPoint getMinLine(LatLng p1, LatLng p2) {
//        List<ElevationPoint> list = getLineElevations(p1, p2);
//        Collections.sort(list);
//        return list.get(0);
//    }
//
//    /**
//     * Finds the lowest elevation on the polyline connecting the points
//     * @param points
//     * @return
//     */
//    public ElevationPoint getMinLine(List<LatLng> points) {
//        LatLng point1, point2;
//        Iterator<LatLng> iter = points.iterator();
//        ArrayList<ElevationPoint> elevationPoints = new ArrayList<ElevationPoint>();
//        if(iter.hasNext()) {
//            point1 = iter.next();
//        }
//        else return null;
//        if(iter.hasNext()) {
//            point2 = iter.next();
//        }
//        else return null;
//        elevationPoints.add(getMinLine(point1, point2));
//        while (iter.hasNext()) {
//            point1 = point2;
//            point2 = iter.next();
//            elevationPoints.add(getMinLine(point1, point2));
//        }
//        Collections.sort(elevationPoints);
//        return elevationPoints.get(0);
//    }
//
//    /**
//     * Finds the highest elevation on the line connecting the two points
//     * @param p1
//     * @param p2
//     * @return
//     */
//    public ElevationPoint getMaxLine(LatLng p1, LatLng p2) {
//        List<ElevationPoint> list = getLineElevations(p1, p2);
//        Collections.sort(list);
//        return list.get(list.size()-1);
//    }
//
//    /**
//     * Finds the highest elevation on the polyline connecting the points
//     * @param points
//     * @return
//     */
//    public ElevationPoint getMaxLine(List<LatLng> points) {
//        LatLng point1, point2;
//        Iterator<LatLng> iter = points.iterator();
//        ArrayList<ElevationPoint> elevationPoints = new ArrayList<ElevationPoint>();
//        if(iter.hasNext()) {
//            point1 = iter.next();
//        }
//        else return null;
//        if(iter.hasNext()) {
//            point2 = iter.next();
//        }
//        else return null;
//        elevationPoints.add(getMaxLine(point1, point2));
//        while (iter.hasNext()) {
//            point1 = point2;
//            point2 = iter.next();
//            elevationPoints.add(getMinLine(point1, point2));
//        }
//        Collections.sort(elevationPoints);
//        return elevationPoints.get(elevationPoints.size()-1);
//    }
//
//    /**
//     * Takes a new water elevation and updates the bitmap to reflect the change
//     * @param level New water level
//     */
//    public void setWaterLevel(double level) {
//        seekBar.setProgress((int)(255.0*(level-MainActivity.sliderMin)/(MainActivity.sliderMax-MainActivity.sliderMin)));
//        MainActivity.updateColors(this);
//    }
//
//    public static void updateColors(double waterLevelMeters, boolean coloring, boolean transparency, float alpha) {
//        if (!currentlyDrawing) {
//            currentlyDrawing = true;
//            //get level from seekbar
//            double distanceFromBottom = waterLevelMeters - getMinElevation();
//            double fieldRange = getMaxElevation() - getMinElevation();
//
//            double level = 255.0*distanceFromBottom/fieldRange;
//
//            int waterLevel = (int)level;
//            int width = getElevationBitmap().getWidth();
//            int height = getElevationBitmap().getHeight();
//            int[] pixels = new int[width * height];
//            getElevationBitmap().getPixels(pixels, 0, width, 0, 0, width, height);
//            Bitmap bitmap = getElevationBitmap().copy(getElevationBitmap().getConfig(), true);
//            int c;
//            if (!coloring) {
//                //test each pixel, if below water level set blue, else set transparent
//                for (int i = 0; i < (width * height); i++) {
//                    pixels[i] = ((pixels[i] & 0x000000FF) < waterLevel) ? 0xFF0000FF : 0x00000000;
//                }
//            }
//            else {
//                //elevation shading is being used
//                for (int i = 0; i < (width * height); i++) {
//                    c = pixels[i] & 0x000000FF;
//                    pixels[i] = (c < waterLevel) ? MainActivity.hsvColors[c] : 0x00000000;
//                }
//            }
//            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
//
//            //remove old map overlay and create new one
//            //this unfortunately creates annoying flickering
//            //currently not aware of any way to avoid this
//
//            GroundOverlay ppo = prevoverlay;
//            prevoverlay = createOverlay(mapFragment.getMap());
//            prevoverlay.setImage(BitmapDescriptorFactory.fromBitmap(bitmap));
//            if (transparency) {
//                prevoverlay.setTransparency(alpha);
//            }
//            ppo.remove();
//            currentlyDrawing = false;
//        }
//    }
}
