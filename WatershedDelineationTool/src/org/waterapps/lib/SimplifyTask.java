package org.waterapps.lib;

import org.gdal.ogr.Geometry;

import android.net.Uri;
import android.os.AsyncTask;

public class SimplifyTask extends AsyncTask<Uri, Integer, Geometry> {
	private float epsilon;
	private Geometry geometry;

	public SimplifyTask(Geometry gemoetry, float epsilon) {
		this.epsilon = epsilon;
		this.geometry = geometry;
	}
	
	@Override
	protected Geometry doInBackground(Uri... params) {
		geometry.Simplify(epsilon);
		return null;
	}
	
    @Override
    protected void onPreExecute() {
    }
    
    protected void onProgressUpdate(Integer... progress) {
    }

    protected void onPostExecute() {
    	
    }
}
