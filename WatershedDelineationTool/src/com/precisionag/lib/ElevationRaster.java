package com.precisionag.lib;

import java.io.FileOutputStream;
import java.util.Arrays;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class ElevationRaster {

	private int ncols;
	private int nrows;
	float [][]elevationData;
	private float minElevation;
	private float maxElevation;
	private LatLng lowerLeft;
	private LatLng upperRight;
    float lowerTenth;
    float upperTenth;
	
	public ElevationRaster(int w, int h, float [][]data) {
		setMinElevation(Float.POSITIVE_INFINITY);
		setMaxElevation(Float.NEGATIVE_INFINITY);
		setNcols(w);
		setNrows(h);
		setElevationData(data);
	}
	
	public ElevationRaster(int w, int h) {
		setMinElevation(Float.POSITIVE_INFINITY);
		setMaxElevation(Float.NEGATIVE_INFINITY);
		setNcols(w);
		setNrows(h);
		setElevationData(new float[getNcols()][getNrows()]);
	}
	
	public ElevationRaster() {
		setMinElevation(Float.POSITIVE_INFINITY);
		setMaxElevation(Float.NEGATIVE_INFINITY);
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
	
	public Bitmap getBitmap() {
        Log.i("bitmap", "bitmap being created");
		Bitmap bitmap;
		int intpixels[] = new int[getNrows()*getNcols()];

        Log.i("min value", Float.toString(getMinElevation()));
        Log.i("max value", Float.toString(getMaxElevation()));
        Log.i("range value", Float.toString(getMaxElevation()-getMinElevation()));
		for(int k = 0; k<getNrows(); k++) {
			for(int m=0; m<getNcols(); m++) {
		    	//normalize each float to a value from 0-255

                double range = 255/(getMaxElevation()-getMinElevation());
		    	intpixels[k+(m*getNrows())] = (int)(range*(getElevationData()[k][m]-getMinElevation()));

                //Log.i("pixel value", Integer.toString(intpixels[m+(k*getNcols())]));
		    	//intpixels[k] = (int)( ((pixels[k]-min)/(max-min))*(double)255.0);
		    	//convert to greyscale ARGB value
		    	//intpixels[m+(k*getNcols())] = 0xFF000000 + intpixels[m+(k*getNcols())] + intpixels[m+(k*getNcols())]<<8 + intpixels[m+(k*getNcols())]<<16;
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
        try {
            FileOutputStream out = new FileOutputStream("/sdcard/field.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (Exception e) {
            e.printStackTrace();
        }



        return bitmap;
	}
	

	public float [][] getElevationData() {
		return elevationData;
	}

	public void setElevationData(float [][] elevationData) {
		this.elevationData = elevationData;
	}

	public float getMaxElevation() {
		return maxElevation;
	}

	public void setMaxElevation(float maxElevation) {
		this.maxElevation = maxElevation;
	}

	public float getMinElevation() {
		return minElevation;
	}

	public void setMinElevation(float minElevation) {
		this.minElevation = minElevation;
	}

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
		return lowerLeft;
	}

	public void setLowerLeft(LatLng lowerLeft) {
		this.lowerLeft = lowerLeft;
	}

	public LatLng getUpperRight() {
		return upperRight;
	}

	public void setUpperRight(LatLng upperRight) {
		this.upperRight = upperRight;
	}

    public void calculateTenths() {
        int size = nrows*ncols;
        float []tempArray = new float[size];
        for(int i=0; i<nrows; i++) {
            for(int j=0; j<ncols; j++) {
                tempArray[i+(nrows*j)] = elevationData[i][j];
            }
        }
        Arrays.sort(tempArray);
        float min = tempArray[(size/100)*3];
        float max = tempArray[(size/100)*97];
        // For Steven's water plane slider
//        MainActivity.sliderMin = min;
//        MainActivity.sliderMax = max;
//        MainActivity.updateEditText(min, max);
    }

}
