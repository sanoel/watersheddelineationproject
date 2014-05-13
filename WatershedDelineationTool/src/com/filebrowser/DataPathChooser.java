package com.filebrowser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.waterapps.watershed.MainActivity;
import org.waterapps.watershed.R;
import org.waterapps.watershed.SettingsActivity;

public class DataPathChooser extends ListActivity {
	private enum DISPLAYMODE {
		ABSOLUTE, RELATIVE;
	}

	private final DISPLAYMODE displayMode = DISPLAYMODE.RELATIVE;
	private List<DirectoryInfo> directories = new ArrayList<DirectoryInfo>();

	private File currentDirectory = Environment.getExternalStorageDirectory();
	private String returnIntent = "com.example.file_browser.MainActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Bundle data = getIntent().getExtras();
		if (data != null && data.containsKey("path")) {
			File current = new File(data.getString("path"));
			File startDir = new File(current.getAbsolutePath());
			//Toast message3 = Toast.makeText(getApplicationContext(), "Path:"
			//		+ startDir.toString(), Toast.LENGTH_SHORT);
			//message3.show();
			if (startDir.exists() && startDir.isDirectory()) {
				this.browseTo(startDir);
			} else {
				browseToRoot();
			}
		} else {
			browseToRoot();
		}
		if(data != null && data.containsKey("returnIntent")){
			returnIntent = data.getString("returnIntent");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.datapathmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menuCancel) {
			if(returnIntent.contentEquals("back") == false){
				Intent i = new Intent(returnIntent);
				//startActivity(i);
			}
			finish();
		} else if (itemId == R.id.menuSelect) {
			SettingsActivity.updateDemFolder();
			if(returnIntent.contentEquals("back") == false){
				Intent i2 = new Intent(returnIntent);
				i2.putExtra("directory", currentDirectory.getPath());
				//startActivity(i2);
			}
			finish();
		}
		return false;
	}

	/**
	 * This function browses to the root-directory of the file-system.
	 */
	private void browseToRoot() {
		browseTo(Environment.getExternalStorageDirectory());
		this.setTitle("/");
	}

	/**
	 * This function browses up one level according to the field:
	 * currentDirectory
	 */
	private void upOneLevel() {
		if (this.currentDirectory.getParent() != null)
			this.browseTo(this.currentDirectory.getParentFile());
		this.setTitle(this.currentDirectory.getAbsolutePath());
	}
	
	@Override
	public void onBackPressed() {
		upOneLevel();
		return;
	}

	private void browseTo(final File aDirectory) {
		if (aDirectory.isDirectory()) {
			this.currentDirectory = aDirectory;
			fill(aDirectory.listFiles());
		} else {
			/*
			 * OnClickListener okButtonListener = new OnClickListener() { //
			 * 
			 * @Override public void onClick(DialogInterface arg0, int arg1) {
			 * Intent myIntent = new Intent( android.content.Intent.ACTION_VIEW,
			 * Uri.parse("file://" + aDirectory.getAbsolutePath()));
			 * startActivity(myIntent); } }; OnClickListener
			 * cancelButtonListener = new OnClickListener() { // @Override
			 * public void onClick(DialogInterface arg0, int arg1) { // Do
			 * nothing } }; new AlertDialog.Builder(this) .setTitle("Question")
			 * .setMessage( "Do you want to open that file?n" +
			 * aDirectory.getName()) .setPositiveButton("OK", okButtonListener)
			 * .setNegativeButton("Cancel", cancelButtonListener).show();
			 */
			// Do nothing when click file
		}
		this.setTitle(this.currentDirectory.getAbsolutePath());
	}

	private void fill(File[] files) {
		this.directories.clear();

		// Add the "." and the ".." == 'Up one level'
		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// this.directories.add(new DirectoryInfo(".", 0));

		if (this.currentDirectory.getParent() != null) {
			this.directories.add(new DirectoryInfo("", 0, 3));
		}
		switch (this.displayMode) {
		case ABSOLUTE:
			if (files != null) {
				if (files.length > 0) {
					for (File file : files) {
						if (file != null) {
							if (file.isDirectory()) {
								this.directories.add(new DirectoryInfo(file
										.getPath(), 0, 1));
							} else {
								this.directories.add(new DirectoryInfo(file
										.getPath(), 0, 0));
							}
						}
					}
				}
			}
			break;
		case RELATIVE: // On relative Mode, we have to add the current-path to
						// the beginning
			if (files != null) {
				if (files.length > 0) {
					int currentPathStringLength = this.currentDirectory
							.getAbsolutePath().length();
					for (File file : files) {
						// Dont show hidden files
						if (file.getName().startsWith(".") == false) {
							if (file.isDirectory()) {
								this.directories
										.add(new DirectoryInfo(
												file.getAbsolutePath()
														.substring(
																currentPathStringLength),
												0, 1));
							} else {
								this.directories
										.add(new DirectoryInfo(
												file.getAbsolutePath()
														.substring(
																currentPathStringLength),
												0, 0));
							}
						}
					}
				}
			}
			break;
		}

		// ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this,
		// R.layout.datapath_listitem, this.directoryEntries);
		// this.setListAdapter(directoryList);

		DirectoryAdapter adapter = new DirectoryAdapter(this,
				R.layout.datapath_listitem, this.directories);
		this.setListAdapter(adapter);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		int selectionRowID = (int) id;
		String selectedFileString = this.directories.get(selectionRowID)
				.getName();
		int fileType = this.directories.get(selectionRowID).getType();

		if (selectedFileString.equals(".")) {
			// Refresh, wont happen ever
			this.browseTo(this.currentDirectory);
		} else if (fileType == 3) {
			this.upOneLevel();
		} else {
			File clickedFile = null;
			switch (this.displayMode) {
			case RELATIVE:
				clickedFile = new File(this.currentDirectory.getAbsolutePath()
						+ this.directories.get(selectionRowID).getName());
				break;
			case ABSOLUTE:
				clickedFile = new File(this.directories.get(selectionRowID)
						.getName());
				break;
			}
			if (clickedFile != null)
				this.browseTo(clickedFile);
		}
	}

	public class DirectoryAdapter extends ArrayAdapter<DirectoryInfo> {

		int resource;
		String response;
		Context context;

		// Initialize adapter
		public DirectoryAdapter(Context context, int resource,
				List<DirectoryInfo> items) {
			super(context, resource, items);
			this.resource = resource;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout listline;
			// Get the current alert object
			DirectoryInfo aDirectory = getItem(position);

			// Inflate the view
			if (convertView == null) {
				listline = new LinearLayout(getContext());
				String inflater = Context.LAYOUT_INFLATER_SERVICE;
				LayoutInflater vi;
				vi = (LayoutInflater) getContext().getSystemService(inflater);
				vi.inflate(resource, listline, true);
			} else {
				listline = (LinearLayout) convertView;
			}
			// Get the text boxes from the listitem.xml file
			TextView fileName = (TextView) listline
					.findViewById(R.id.directoryName);
			ImageView directoryIcon = (ImageView) listline
					.findViewById(R.id.directoryIcon);
			ImageView fileIcon = (ImageView) listline
					.findViewById(R.id.fileIcon);
			ImageView upFolder = (ImageView) listline.findViewById(R.id.upIcon);
			// Assign the appropriate data from our alert object above
			fileName.setText(" " + aDirectory.getName().replaceFirst("/", ""));
			if (aDirectory.getType() == 3) {
				// upfolder
				directoryIcon.setVisibility(8);
				fileIcon.setVisibility(8);
				upFolder.setVisibility(0);
			} else if (aDirectory.getType() == 0) {
				// File
				// directoryIcon.setImageResource(R.drawable.ic_launcher);
				directoryIcon.setVisibility(8);
				upFolder.setVisibility(8);
				fileIcon.setVisibility(0);
			} else {
				// Directory
				directoryIcon.setVisibility(0);
				upFolder.setVisibility(8);
				fileIcon.setVisibility(8);
			}

			return listline;
		}
	}

	public class DirectoryInfo {

		private String name;
		private int fileCount;
		private int type; // 1 is directory, 0 file

		public DirectoryInfo(String theName, int files, int filetype) {
			this.name = theName;
			this.fileCount = files;
			this.type = filetype;
		}

		public String getName() {
			return name;
		}

		public int getFileCount() {
			return fileCount;
		}

		public int getType() {
			return type;
		}
	}
}
