package org.waterapps.watershed;

import org.waterapps.watershed.R;
import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tiffdecoder.TiffDecoder;

public class ResultsPanelFragment extends Fragment{
	private TextView area;
	private TextView depressionStorage;
//    private WatershedDataset watershedDataset;
	ResultsPanelFragmentListener listener;
	
	public interface ResultsPanelFragmentListener {
		public WatershedDataset ResultsPanelFragmentGetData();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.updateResults(1);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof ResultsPanelFragmentListener) {
			listener = (ResultsPanelFragmentListener) activity;
		}
	}
	
	public void updateResults(int clickType) {
//		watershedDataset = listener.ResultsPanelFragmentGetData();
		if (clickType == 0) {
			area.setText("Area: " + String.format("%.3f", MainActivity.watershedDataset.pits.pitDataList.get(MainActivity.selectedPitIndex).allPointsList.size()*0.000247105*TiffDecoder.nativeTiffGetScaleX()*TiffDecoder.nativeTiffGetScaleY())+ " acres");
			depressionStorage.setText("Retention Volume: " + String.format("%.3f", MainActivity.watershedDataset.pits.pitDataList.get(MainActivity.selectedPitIndex).retentionVolume*0.000810713194) + " acre-feet");
		} else {
			area.setText("Area: " + String.format("%.3f", MainActivity.watershedDataset.delineatedArea*0.000247105*TiffDecoder.nativeTiffGetScaleX()*TiffDecoder.nativeTiffGetScaleY())+ " acres");
			depressionStorage.setText("Retention Volume: " + String.format("%.3f", MainActivity.watershedDataset.delineatedStorageVolume) + " acre-feet");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.results_fragment, container, false);
		area = (TextView) view.findViewById(R.id.area);
//		averageSlope = (TextView) view.findViewById(R.id.average_slope);
		depressionStorage = (TextView) view.findViewById(R.id.depression_storage);
		return view;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);	
	}	
}
