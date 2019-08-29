package com.filebrowser;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.waterapps.watershed.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataFileChooser extends ListActivity {
    private static /* synthetic */ int[] $SWITCH_TABLE$com$filebrowser$DataFileChooser$DISPLAYMODE;
    private File currentDirectory = Environment.getExternalStorageDirectory();
    private List<DirectoryInfo> directories = new ArrayList();
    private final DISPLAYMODE displayMode = DISPLAYMODE.RELATIVE;
    private String returnIntent = "com.example.file_browser.MainActivity";

    private enum DISPLAYMODE {
        ABSOLUTE,
        RELATIVE
    }

    public class DirectoryAdapter extends ArrayAdapter<DirectoryInfo> {
        Context context;
        int resource;
        String response;

        public DirectoryAdapter(Context context2, int resource2, List<DirectoryInfo> items) {
            super(context2, resource2, items);
            this.resource = resource2;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout listline;
            DirectoryInfo aDirectory = (DirectoryInfo) getItem(position);
            if (convertView == null) {
                listline = new LinearLayout(getContext());
                ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(this.resource, listline, true);
            } else {
                listline = (LinearLayout) convertView;
            }
            ImageView directoryIcon = (ImageView) listline.findViewById(R.id.directoryIcon);
            ImageView fileIcon = (ImageView) listline.findViewById(R.id.fileIcon);
            ImageView upFolder = (ImageView) listline.findViewById(R.id.upIcon);
            ((TextView) listline.findViewById(R.id.directoryName)).setText(" " + aDirectory.getName().replaceFirst("/", ""));
            if (aDirectory.getType() == 3) {
                directoryIcon.setVisibility(8);
                fileIcon.setVisibility(8);
                upFolder.setVisibility(0);
            } else if (aDirectory.getType() == 0) {
                directoryIcon.setVisibility(8);
                upFolder.setVisibility(8);
                fileIcon.setVisibility(0);
            } else {
                directoryIcon.setVisibility(0);
                upFolder.setVisibility(8);
                fileIcon.setVisibility(8);
            }
            return listline;
        }
    }

    public class DirectoryInfo {
        private int fileCount;
        private String name;
        private int type;

        public DirectoryInfo(String theName, int files, int filetype) {
            this.name = theName;
            this.fileCount = files;
            this.type = filetype;
        }

        public String getName() {
            return this.name;
        }

        public int getFileCount() {
            return this.fileCount;
        }

        public int getType() {
            return this.type;
        }
    }

    static /* synthetic */ int[] $SWITCH_TABLE$com$filebrowser$DataFileChooser$DISPLAYMODE() {
        int[] iArr = $SWITCH_TABLE$com$filebrowser$DataFileChooser$DISPLAYMODE;
        if (iArr == null) {
            iArr = new int[DISPLAYMODE.values().length];
            try {
                iArr[DISPLAYMODE.ABSOLUTE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[DISPLAYMODE.RELATIVE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            $SWITCH_TABLE$com$filebrowser$DataFileChooser$DISPLAYMODE = iArr;
        }
        return iArr;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getIntent().getExtras();
        if (data == null || !data.containsKey("path")) {
            browseToRoot();
        } else {
            File startDir = new File(new File(data.getString("path")).getAbsolutePath());
            Toast.makeText(getApplicationContext(), "Path:" + startDir.toString(), 0).show();
            if (!startDir.exists() || !startDir.isDirectory()) {
                browseToRoot();
            } else {
                browseTo(startDir);
            }
        }
        if (data != null && data.containsKey("returnIntent")) {
            this.returnIntent = data.getString("returnIntent");
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.datapathmenu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menuCancel) {
            if (!this.returnIntent.contentEquals("back")) {
                new Intent(this.returnIntent);
            }
            finish();
        } else if (itemId == R.id.menuSelect) {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
            editor.putString("dataPath", new StringBuilder(String.valueOf(this.currentDirectory.toString())).append("/").toString());
            editor.commit();
            if (!this.returnIntent.contentEquals("back")) {
                new Intent(this.returnIntent);
            }
            finish();
        }
        return false;
    }

    private void browseToRoot() {
        browseTo(new File("/"));
        setTitle("/");
    }

    private void upOneLevel() {
        if (this.currentDirectory.getParent() != null) {
            browseTo(this.currentDirectory.getParentFile());
        }
        setTitle(this.currentDirectory.getAbsolutePath());
    }

    public void onBackPressed() {
        upOneLevel();
    }

    private void browseTo(File aDirectory) {
        if (aDirectory.isDirectory()) {
            this.currentDirectory = aDirectory;
            fill(aDirectory.listFiles());
        } else {
            setResult(-1, new Intent("com.example.RESULT_ACTION", Uri.parse(aDirectory.getPath())));
            finish();
        }
        setTitle(this.currentDirectory.getAbsolutePath());
    }

    private void fill(File[] files) {
        this.directories.clear();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        if (this.currentDirectory.getParent() != null) {
            this.directories.add(new DirectoryInfo("", 0, 3));
        }
        switch ($SWITCH_TABLE$com$filebrowser$DataFileChooser$DISPLAYMODE()[this.displayMode.ordinal()]) {
            case 1:
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        if (file != null) {
                            if (file.isDirectory()) {
                                this.directories.add(new DirectoryInfo(file.getPath(), 0, 1));
                            } else {
                                this.directories.add(new DirectoryInfo(file.getPath(), 0, 0));
                            }
                        }
                    }
                    break;
                }
            case 2:
                if (files != null && files.length > 0) {
                    int currentPathStringLength = this.currentDirectory.getAbsolutePath().length();
                    for (File file2 : files) {
                        if (!file2.getName().startsWith(".")) {
                            if (file2.isDirectory()) {
                                this.directories.add(new DirectoryInfo(file2.getAbsolutePath().substring(currentPathStringLength), 0, 1));
                            } else {
                                this.directories.add(new DirectoryInfo(file2.getAbsolutePath().substring(currentPathStringLength), 0, 0));
                            }
                        }
                    }
                    break;
                }
        }
        setListAdapter(new DirectoryAdapter(this, R.layout.datapath_listitem, this.directories));
    }

    /* access modifiers changed from: protected */
    public void onListItemClick(ListView l, View v, int position, long id) {
        int selectionRowID = (int) id;
        String selectedFileString = ((DirectoryInfo) this.directories.get(selectionRowID)).getName();
        int fileType = ((DirectoryInfo) this.directories.get(selectionRowID)).getType();
        if (selectedFileString.equals(".")) {
            browseTo(this.currentDirectory);
        } else if (fileType == 3) {
            upOneLevel();
        } else {
            File clickedFile = null;
            switch ($SWITCH_TABLE$com$filebrowser$DataFileChooser$DISPLAYMODE()[this.displayMode.ordinal()]) {
                case 1:
                    clickedFile = new File(((DirectoryInfo) this.directories.get(selectionRowID)).getName());
                    break;
                case 2:
                    clickedFile = new File(new StringBuilder(String.valueOf(this.currentDirectory.getAbsolutePath())).append(((DirectoryInfo) this.directories.get(selectionRowID)).getName()).toString());
                    break;
            }
            if (clickedFile != null) {
                browseTo(clickedFile);
            }
        }
    }
}
