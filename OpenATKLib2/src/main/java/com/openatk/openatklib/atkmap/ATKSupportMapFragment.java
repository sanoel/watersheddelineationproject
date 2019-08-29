package com.openatk.openatklib.atkmap;


import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.openatk.openatklib.atkmap.listeners.ATKTouchableWrapperListener;

public class ATKSupportMapFragment extends SupportMapFragment implements OnMapReadyCallback {
	 public View mOriginalContentView;
	 public ATKTouchableWrapper mTouchView;   
	 private ATKMap map = null;
	 private boolean retained = false;
	 onMapReadyListener callback;


	@Override
	 public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
	    mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState); 
	    
	    
	    Log.d("atkSupportMapFragment", "onCreateView()");
	    //LayoutInflater vi = (LayoutInflater) this.getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    //View v = vi.inflate(R.layout.pan_button, null);

	    mTouchView = new ATKTouchableWrapper(getActivity());
	    mTouchView.addView(mOriginalContentView);
	    //mTouchView.addView(v);
	    
	    //Add listeners for clicks on the map
	    //mTouchView.addListener(new panListener(v));

	    if(this.map == null){
	    	Log.d("atkSMFrag-onCreateView", "New atkmap");
	    	//This is what makes it all save since retain instancestate is true
	    	//Views are recreated but objects are not
			Log.w("FIRING THE ASYNC", "HERE GOOOOOESSS");
			this.getMapAsync(this);
	    } else {
	    	Log.d("atkSMF-onCreateView()", "Reused atkmap");
	    	retained = true;
			mTouchView.addListener(this.map); //Let the atkMap listen for touch events
		}

	    return mTouchView;
	 }

	public void setOnMapReadyListener(onMapReadyListener callback) {
		this.callback = callback;
	}
	 	
	 public boolean getRetained(){
		 return this.retained;
	 }
	 
	 public ATKMap getAtkMap(){
		return this.map;
	 }

	@Override
	public void onMapReady(GoogleMap googleMap) {
	 	Log.w("MAP READY", "MAP READY!!!!!!!!!!!!!");
		this.map = new ATKMap(googleMap, this.getActivity().getApplicationContext());
		mTouchView.addListener(this.map); //Let the atkMap listen for touch events
		callback.onMapReadyNow(this);
	}

	public interface onMapReadyListener {
	 	public void onMapReadyNow(ATKSupportMapFragment atkSupportMapFragment);
	}

	 @Override
	 public View getView() {
		 return mOriginalContentView;
	 }
	 
	 private class panListener implements ATKTouchableWrapperListener {

		 private View panButton;

		 public panListener(View panButton){
			 this.panButton = panButton;
		 }

		 @Override
		 public boolean onTouch(MotionEvent event) {
			 Log.d("atkTouchableWrapper", "dispatchTouchEvent");
			 Log.d("atkTouchableWrapper", "event X:"  + Float.toString(event.getX()) + " event Y:" + Float.toString(event.getY()) );
			 Log.d("atkTouchableWrapper", "pic X:"  + Integer.toString(this.panButton.getLeft()) + " pic Y:" + Integer.toString(this.panButton.getTop()) );
			 Log.d("atkTouchableWrapper", "pic wdith:"  + Integer.toString(this.panButton.getWidth()) + " pic height:" + Integer.toString(this.panButton.getHeight()) );

			 //TODO Density stuff
			 float panX = 0.0f;
			 float panY = 0.0f;
			 float panWidth = 0.0f;
			 float panHeight = 0.0f;
			 
			 if(panX < event.getX() && (panX + panWidth) > event.getX()){
				 Log.d("atkTouchableWrapper", "X good");
				 if(panY < event.getY() && (panY + panHeight) > event.getY()){
					 Log.d("atkTouchableWrapper", "Y good");
					 //return true;
				 }
			 }
			 switch (event.getAction()) {
				 case MotionEvent.ACTION_DOWN:
					 break;
				 case MotionEvent.ACTION_UP:
					 break;
			 }
			 return false;
		 }
	 }
	 
}
