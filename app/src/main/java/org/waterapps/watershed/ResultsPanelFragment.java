package org.waterapps.watershed;

import org.waterapps.watershed.R;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ResultsPanelFragment extends Fragment{
	private TextView title;
	private TextView area;
//	private TextView depressionStorage;
	ResultsPanelFragmentListener listener;
	int CLICKED_CATCHMENT= 0;
	int CLICKED_WATERSHED = 1;
	
	public interface ResultsPanelFragmentListener {
		public WatershedDataset ResultsPanelFragmentGetData();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		double delineationArea = getArguments().getDouble("delineationArea");    
		this.updateResultsCatchment(1, delineationArea);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof ResultsPanelFragmentListener) {
			listener = (ResultsPanelFragmentListener) activity;
		}
	}
	
	public void updateResultsCatchment(int catchmentId, double catchmentArea) {
			title.setText("Catchment " + String.format(Integer.toString(catchmentId)));
			area.setText("Area: " + String.format("%.3f", catchmentArea) + " acres");
	}
	
	public void updateResultsDelineation(double delineationArea) {
			title.setText("Watershed Delineation");
			area.setText("Area: " + String.format("%.3f", delineationArea) + " acres");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.results_fragment, container, false);
		title = (TextView) view.findViewById(R.id.results_title);
		area = (TextView) view.findViewById(R.id.area);
//		depressionStorage = (TextView) view.findViewById(R.id.depression_storage);
		return view;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);	
	}	
}
