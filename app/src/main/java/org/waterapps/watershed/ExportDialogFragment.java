package org.waterapps.watershed;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class ExportDialogFragment extends DialogFragment {
	EditText catchmentsText;
	EditText delineationText;
	CheckBox catchmentsCheck;
	CheckBox delineationCheck;
//	MainActivity mainActivity;
	int[][] delineationRaster;
	int[][] catchmentRaster;

	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	//TODO Test out getting all main activity stuff from saedInstanceState
    	final String filePath = savedInstanceState.getString("filePath");
    	final String demDirectory = savedInstanceState.getString("demDirectory");
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = this.getActivity().getLayoutInflater().inflate(R.layout.export_dialog_fragment, null);
        catchmentsCheck = (CheckBox) view.findViewById(R.id.checkbox_catchments);
        delineationCheck = (CheckBox) view.findViewById(R.id.checkbox_delineation);
        catchmentsText = (EditText) view.findViewById(R.id.edittext_catchments);
        delineationText = (EditText) view.findViewById(R.id.edittext_delineation);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view);
        builder.setMessage("Export Files...");
        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	String delineationFilename = delineationText.getText().toString();
            	String catchmentsFilename = catchmentsText.getText().toString();
                 if (delineationCheck.isChecked()) {
                	 WatershedDataset.writeDelineation(filePath, delineationRaster, demDirectory+"/"+delineationFilename);
                 }
                 if (catchmentsCheck.isChecked()) {
                	 WatershedDataset.writeCatchments(filePath, catchmentRaster, demDirectory+"/"+catchmentsFilename);
                 }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ExportDialogFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }
    
    public void setData() {
    	
    }
    
    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();
        
        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_catchments:
                if (checked)
                	catchmentsText.setEnabled(true);
                else
                	catchmentsText.setEnabled(false);	
                break;
            case R.id.checkbox_delineation:
                if (checked)
                    delineationText.setEnabled(true);
                else
                	delineationText.setEnabled(false);
                break;
        }
    }
    
}

