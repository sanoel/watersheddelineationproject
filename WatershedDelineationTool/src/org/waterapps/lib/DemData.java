package org.waterapps.lib;

import static android.graphics.Color.HSVToColor;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.widget.SeekBar;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.openatk.openatklib.atkmap.ATKMap;

import java.io.File;
import java.util.Arrays;

/**
 * Stores data read in from a DEM, as both raw floats and a bitmap representation.
 */
public class DemData extends DemFile{
	private float[][] elevationData;
	private float cellSize;
	private float noDataVal;

	//View-related variables
	private float minElevation;
	private float maxElevation;
	private Bitmap elevationBitmap;
	private GroundOverlay demOverlay;
	WmacDemLoadUtilsListener listener;

	private int hsvColors[] = new int[256];
	private int hsvTransparentColors[];
	
	public DemData(DemFile demFile) {
		super(demFile);
		this.minElevation = Float.NEGATIVE_INFINITY;
		this.minElevation = Float.POSITIVE_INFINITY;		
	}

	/**
	 * Calculates the center of the DEM area
	 * @return Calculated center
	 */
	public LatLng getCenter() {
		LatLngBounds bounds = getDemFile().getBounds();
		LatLng sw = bounds.southwest;
		LatLng ne = bounds.northeast;
		double newLat = (sw.latitude + ne.latitude)/2;
		double newLong = (sw.longitude + ne.longitude)/2;
		return new LatLng(newLat, newLong);
	}

	/**
	 * Gets the row and column (x,y) from a latitude, longitude location
	 * @param latitude, longitude location to find the (x,y)
	 * @return (x,y) Point corresponding to the given latitude, longitude 
	 */
	public Point getXYFromLatLng(LatLng latlng) {
		if (getBounds().contains(latlng)) {
			//use linear interpolation to figure out which pixel to get data from
			//should be accurate since fields <= ~1 mile wide
			double north = getBounds().northeast.latitude;
			double east = getBounds().northeast.longitude;
			double south = getBounds().southwest.latitude;
			double west = getBounds().southwest.longitude;

			int width = getElevationBitmap().getWidth();
			int height = getElevationBitmap().getHeight();

			double x = (double) width*(east - latlng.longitude)/(east-west);
			double y = (double) height*(north - latlng.latitude)/(north-south);
			Point xy = new Point();
			xy.set((int)x, (int) y);
			return xy;
		} else {
			return null;
		}
	}

	/**
	 * Gets elevation of a point
	 * @param point Location to get elevation from
	 * @return Elevation of the point
	 */
	public double elevationFromLatLng(LatLng latlng) {
		if (getBounds().contains(latlng)) {
			Point xy = getXYFromLatLng(latlng);
			int waterLevel = getElevationBitmap().getPixel((int)xy.x, (int)xy.y);

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
	
	public GroundOverlay createOverlay(LatLngBounds bounds, ATKMap map) {
		GroundOverlay groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(elevationBitmap))
		.positionFromBounds(bounds)
		.transparency(0));
		groundOverlay.setVisible(true);
		return groundOverlay;
	}

	public GroundOverlay createOverlay(ATKMap map) {
		BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(elevationBitmap);
		demOverlay = map.addGroundOverlay(new GroundOverlayOptions()
		.image(image)
		.positionFromBounds(getBounds())
		.transparency(0));
		demOverlay.setVisible(true);
		return demOverlay;
	}
	
	/**
	 * Generates a bitmap from the raw float data
	 * @return Generated bitmap
	 */
	public void makeElevationBitmap() {
		setHsv();
		int pixels[] = new int[this.elevationData.length*this.elevationData[0].length];
		double range = 255/(this.maxElevation-this.minElevation);
		for(int r = 0; r < this.elevationData.length; r++) {
			for(int c = 0; c < this.elevationData[0].length; c++) {
				//integer from 0 - 255
				pixels[r+(c*this.elevationData.length)] = (int)(range*(this.elevationData[r][c]-this.minElevation));
				//colormapped color
				pixels[r+(c*this.elevationData.length)] = hsvColors[pixels[r+(c*this.elevationData.length)]];
				//black and white
//				pixels[r+(c*this.elevationData.length)] = Color.argb(255, pixels[r+(c*this.elevationData.length)], pixels[r+(c*this.elevationData.length)], pixels[r+(c*this.elevationData.length)]);
			}
		}
		elevationBitmap = Bitmap.createBitmap(pixels, 0, this.elevationData.length, this.elevationData.length, this.elevationData[0].length, Bitmap.Config.ARGB_8888);
	}
	
	// Get colors for DEM coloring
	public void setHsv(){
		hsvColors = new int[256];
		hsvTransparentColors = new int[256];
		float hsvComponents[] = {1.0f, 0.75f, 0.75f};
		for(int i = 0; i<255; i++) {
			hsvComponents[0] = 360.0f*i/255.0f;
			hsvColors[i] = HSVToColor(hsvComponents);
			hsvTransparentColors[i] = HSVToColor(128, hsvComponents);
		}
	}
	
	//TODO this seems a bit waterplane specific
	/**
	 * Calculates the upper and lower slider values to set for the field, based on upper/lower 3% of data
	 */
	public void calculateTenths() {
		int size = elevationData.length*elevationData[0].length;
		float[] tempArray = new float[size];
		for(int i=0; i<elevationData.length; i++) {
			for(int j=0; j<elevationData[0].length; j++) {
				tempArray[i+(elevationData.length*j)] = elevationData[i][j];
			}
		}
		Arrays.sort(tempArray);
	}

	public Bitmap getBitmap() {
		return elevationBitmap;
	}

	public void setElevationBitmap(Bitmap elevationBitmap) {
		this.elevationBitmap = elevationBitmap;
	}

	public void setBitmap(Bitmap bits) {
		setElevationBitmap(bits);
	}

	public double getMinElevation() {
		return minElevation;
	}

	public double getMaxElevation() {
		return maxElevation;
	}

	public void setMaxElevation(float maxElevation) {
		this.maxElevation = maxElevation;
	}

	public void setMinElevation(float minElevation) {
		this.minElevation = minElevation;
	}

	public float[][] getData() {
		return getElevationData();
	}

	public float [][] getElevationData() {
		return elevationData;
	}

	public void setElevationData(float [][] elevationData) {
		this.elevationData = elevationData;
	}

	public DemFile getDemFile() {
		return this;
	}

	public void setSeekBar(SeekBar bar) {
	}

	public Bitmap getElevationBitmap() {
		return elevationBitmap;
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

	public void setGroundOverlay(ATKMap map, GroundOverlayOptions groundOverlayOptions) {
		if (demOverlay != null) {
			demOverlay.remove();
		}
		this.demOverlay = map.addGroundOverlay(groundOverlayOptions);
	}

	public GroundOverlay getGroundOverlay() {
		return demOverlay;
	}
	
}
