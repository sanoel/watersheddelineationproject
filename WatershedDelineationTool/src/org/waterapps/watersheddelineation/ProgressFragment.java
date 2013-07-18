package org.waterapps.watersheddelineation;

import org.waterapps.watersheddelineation.R;
import org.waterapps.watersheddelineation.WatershedDataset.WatershedDatasetListener;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressFragment extends Fragment{
	private ProgressBar progressBar;
	private TextView waitMessage;
	private TextView progressMessage;
    private WatershedDataset watershedDataset;
	ProgressFragmentListener pflistener;
    WatershedDatasetListener listener;
	
	public interface ProgressFragmentListener {
		public WatershedDataset ProgressFragmentGetData();
//		public void ProgressFragmentOnProgress();
		public void ProgressFragmentDone(WatershedDataset watershedDataset);
		//put functions in here to save any changes to the results views 
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.getData();
	}
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof ProgressFragmentListener) {
			pflistener = (ProgressFragmentListener) activity;
		}
		if (activity instanceof WatershedDatasetListener) {
			listener = (WatershedDatasetListener) activity;
		}
	}
	
	public void getData() {
		watershedDataset = pflistener.ProgressFragmentGetData();
		if(watershedDataset == null){
		} else {
			new RunSimulation().execute(watershedDataset);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.progress_fragment, container, false);
		waitMessage = (TextView) view.findViewById(R.id.progress_message);
		waitMessage.setText("Here's what's coming...");
		progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		ImageView catchmentsImage = (ImageView) view.findViewById(R.id.catchments_example_image);
		progressMessage = (TextView) view.findViewById(R.id.progress_status);
		
		return view;
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);	
	}
	
	private class RunSimulation extends AsyncTask<WatershedDataset, String, WatershedDataset> implements WatershedDatasetListener {
		protected void onPreExecute() {
			super.onPreExecute();
			progressBar.setVisibility(View.VISIBLE);
			progressMessage.setVisibility(View.VISIBLE);
		}

		protected WatershedDataset doInBackground(WatershedDataset... inputWds) {		 
			// Background Work
			if (inputWds[0].fillPits() != true) {
			  // fillPits failed!
			}
			return inputWds[0];
		}

		@Override
		protected void onProgressUpdate(String... values) {
			progressBar.setProgress(Integer.parseInt(values[0]));
			progressMessage.setText(values[1]);
		}

		protected void onPostExecute(WatershedDataset result) {
			super.onPostExecute(result);
			pflistener.ProgressFragmentDone(result);
		}

		@Override
		public void watershedDatasetOnProgress(int progress, String status) {
			String[] array = new String[2];
			array[0] = Integer.toString(progress);
			array[1] = status;
			publishProgress(array);
		}

		@Override
		public void watershedDatasetDone() {

		}
	}
	
}
