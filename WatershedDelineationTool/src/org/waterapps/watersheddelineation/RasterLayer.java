package org.waterapps.watersheddelineation;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class RasterLayer {
	//bitmap represents rasterized elevation data
	Bitmap rasterLayerBitmap = null;
	
	//defines the edges of the field
	LatLngBounds rasterBounds;
	LatLng sw;
	LatLng ne;
	
	
	//constructor method
	public RasterLayer(double[][] inputDouble, LatLng southwest, LatLng northeast) {
		int numrows = inputDouble.length;
		int numcols = inputDouble[0].length;
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		rasterLayerBitmap = Bitmap.createBitmap(numcols, numrows, config);
		sw = southwest;
		ne = northeast;
		rasterBounds = new LatLngBounds(sw, ne);
	}
	
	public void setBounds(LatLngBounds bounds) {
		rasterBounds = bounds;
	}
	
	
	public void setNortheast(LatLng northeast) {
		ne = northeast;
	}
	
	public void setSouthwest(LatLng southwest) {
		sw = southwest;
	}
	
	public LatLng getCenterLatLng() {
		double north = ne.longitude;
		double east = ne.latitude;
		double south = sw.longitude;
		double west = sw.latitude;
		double centerLong = (north + south) / 2;
		double centerLat = (east + west) / 2;
		LatLng center = new LatLng(centerLat, centerLong);
		return center;
	}
	
	//creates an overlay view of the field on the specified map object
	public GroundOverlay createOverlay(GoogleMap map) {
		if(map == null){
			Log.e("createOverlay", "Null map");
		}
		GroundOverlay groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
	     .image(BitmapDescriptorFactory.fromBitmap(rasterLayerBitmap))
	     .positionFromBounds(rasterBounds)
	     .transparency(0));
		groundOverlay.setVisible(true);
		return groundOverlay;
	}

	//returns elevation of given point
	public double pixelValueFromLatLng(LatLng point) {
		if (rasterBounds.contains(point)) {
			//use linear interpolation to figure out which pixel to get data from
			//should be accurate since fields <= ~1 mile wide
			double north = ne.longitude;
			double east = ne.latitude;
			double south = sw.longitude;
			double west = sw.latitude;
			
			int width = rasterLayerBitmap.getWidth();
			int height = rasterLayerBitmap.getHeight();
			
			double x = (double)width*(point.latitude-west)/(east-west);
			double y = (double)height*(point.longitude-south)/(north-south);
			
			//retrieve packed int
			int pixelValue = rasterLayerBitmap.getPixel((int)x, (int)y);
			
			//pixels are represented as packed ARGB, so discard all but blue channel
			//this gives range of 0-255
			//pixelValue &= 0x000000FF;
			return pixelValue;
		}
		else {
			//point isn't in the field
			return 0.0;
		}
	}
	
	//returns elevation of given point
	public int[] pixelCoordsFromLatLng(LatLng point) {
		int[] xy = null;
		if (rasterBounds.contains(point)) {
			//use linear interpolation to figure out which pixel to get data from
			//should be accurate since fields <= ~1 mile wide
			double north = ne.longitude;
			double east = ne.latitude;
			double south = sw.longitude;
			double west = sw.latitude;
			
			int width = rasterLayerBitmap.getWidth();
			int height = rasterLayerBitmap.getHeight();
			
			double x = (double)width*(point.latitude-west)/(east-west);
			double y = (double)height*(point.longitude-south)/(north-south);
			
			xy[0] = (int) x;
			xy[1] = (int) y;
			
			return xy;
		}
		else {
			//point isn't in the field
			return xy;
		}
	}
	
}
