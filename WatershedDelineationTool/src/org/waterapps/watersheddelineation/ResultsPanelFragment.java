package org.waterapps.watersheddelineation;

import org.waterapps.watersheddelineation.ProgressFragment.ProgressFragmentListener;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tiffdecoder.TiffDecoder;

public class ResultsPanelFragment extends Fragment{
//	private TextView averageSlope;
	private TextView area;
//	private TextView depressionStorage;
    private WatershedDataset watershedDataset;
//    private int selected_catchment;
	ResultsPanelFragmentListener listener;
	
	public interface ResultsPanelFragmentListener {
		public WatershedDataset ResultsPanelFragmentGetData();
		public void ResultsFragmentDone(WatershedDataset watershedDataset);
		//put functions in here to save any changes to the results views 
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		this.getData();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof ResultsPanelFragmentListener) {
			listener = (ResultsPanelFragmentListener) activity;
		}
	}
	
	public void getData() {
		watershedDataset = listener.ResultsPanelFragmentGetData();
		if(watershedDataset == null){
			Log.e("resultspanelfrag - getData", "watershedDataset = null");
		} 
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
//		float depressionVolume = 0; 
//		for (int r = 0; r < watershedDataset.originalDem.length; r++) {
//			for (int c = 0; c < watershedDataset.originalDem[0].length; c++) {
//				if (delinBitmap.getPixel(numcols - 1 - flowDirection[r][c].parentList.get(i).x, flowDirection[r][c].parentList.get(i).y) == Color.RED) {
//			}
//		}
		View view = inflater.inflate(R.layout.results_fragment, container, false);
		area = (TextView) view.findViewById(R.id.area);
		area.setText(Double.toString(WatershedDataset.delineatedArea*0.000247105*TiffDecoder.nativeTiffGetScaleX()*TiffDecoder.nativeTiffGetScaleY())); //convert to acres, multiply cell count by pixel area to get true area
//		area.setText(Double.toString(watershedDataset.pits.pitDataList.get(selected_catchment).areaCellCount*TiffDecoder.nativeTiffGetScaleX()*TiffDecoder.nativeTiffGetScaleY()));
//		averageSlope = (TextView) view.findViewById(R.id.average_slope);
//		averageSlope.setText(Double.toString(watershedDataset.pits.pitDataList.get(selected_catchment).averageSlope));
//		depressionStorage = (TextView) view.findViewById(R.id.depression_storage);
//		depressionStorage.setText(Double.toString(watershedDataset.pits.pitDataList.get(selected_catchment).filledVolume));
		return view;
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);	
	}	
}
