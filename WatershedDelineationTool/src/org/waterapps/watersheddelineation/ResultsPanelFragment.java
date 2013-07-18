package org.waterapps.watersheddelineation;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.waterapps.watersheddelineation.R;

public class ResultsPanelFragment extends Fragment{
	private TextView averageSlope;
	private TextView area;
	private TextView depressionStorage;
    private WatershedDataset watershedDataset;
    private int selected_catchment;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		this.getData();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}
	
	public void getData() {
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.results_fragment, container, false);
		area = (TextView) view.findViewById(R.id.area);
		area.setText(Double.toString(watershedDataset.pits.pitDataList.get(selected_catchment).areaCellCount*RainfallSimConfig.cellSize));
		averageSlope = (TextView) view.findViewById(R.id.average_slope);
//		averageSlope.setText(Double.toString(watershedDataset.pits.pitDataList.get(selected_catchment).averageSlope));
		depressionStorage = (TextView) view.findViewById(R.id.depression_storage);
		depressionStorage.setText(Double.toString(watershedDataset.pits.pitDataList.get(selected_catchment).filledVolume));
		return view;
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);	
	}	
}
