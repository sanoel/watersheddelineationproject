package org.waterapps.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.internal.view.SupportMenu;
import android.util.Log;
import com.openatk.openatklib.atkmap.ATKMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DemLoadUtils {
    private static final int FILE_CHOOSER = 6503;
    private static final int INITIAL_LOAD = 6502;
    private String demDirectory;
    List<DemFile> demFiles;
    private DemData loadedDemData;
    private ATKMap map;
    SharedPreferences prefs;
    WmacListener wmacListener;
    Activity mainAct;
    Integer writePerm;

    public DemLoadUtils(Activity act, ATKMap map2, SharedPreferences prefs2, int perm) {
        this.map = map2;
        this.prefs = prefs2;
        this.demDirectory = prefs2.getString("dem_dir", new StringBuilder(String.valueOf(Environment.getExternalStorageDirectory().toString())).append("/dem").toString());
        this.mainAct = act;
        this.writePerm = perm;
        scanDems(act, perm);
    }

    public void loadDem(Context context, String filePath) {
        boolean z;
        if (this.loadedDemData != null) {
            this.loadedDemData.getDemFile().getOutlinePolygon().setFillColor(Color.argb(64, 255, 0, 0));
            this.loadedDemData.getDemFile().getOutlinePolygon().setStrokeColor(SupportMenu.CATEGORY_MASK);
            this.loadedDemData.getDemFile().getOutlinePolygon().setLabel("Tap here to load");
        }
        DemFile demFile = getDemFileFromFilePath(filePath);
        String str = "demFileIsNull";
        if (demFile == null) {
            z = true;
        } else {
            z = false;
        }
        Log.w(str, Boolean.toString(z));
        Log.w("demFilepath", filePath);
        new ReadDemDataTask(context, this, this.wmacListener, demFile).execute(new Uri[]{Uri.parse(Uri.encode(filePath))});
    }

    public void loadClickedDem(Context context, String demFilePath) {
        if (!demFilePath.equals(this.loadedDemData.getFilePath())) {
            loadDem(context, demFilePath);
        }
    }

    public void scanDems(Activity act, int perm) {
        File f = new File(this.demDirectory);
        Log.w("file", Boolean.toString(f.isDirectory()));
        Log.w("file", Boolean.toString(f.exists()));
        Log.w("PERM", Boolean.toString(ContextCompat.checkSelfPermission(act,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED));
        if (!f.exists() || !f.isDirectory()) {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(act,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(act,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    return;
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(act,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            perm);
                    Boolean succ = f.mkdir();
                    Log.w("MKDIR", Boolean.toString(succ));
                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                Log.w("HELLO", "MAKING DIR");
                // Permission has already been granted
                Boolean succ = f.mkdir();
                Log.w("MKDIR", Boolean.toString(succ));
            }
        }
        this.demFiles = new ArrayList(f.listFiles().length);
        if (f.isDirectory()) {
            File[] file = f.listFiles();
            for (int i = 0; i < file.length; i++) {
                if (file[i].isFile()) {
                    this.demFiles.add(new DemFile(file[i], GdalUtils.readDemFileBounds(file[i]), this.map));
                }
            }
        }
    }

    public void clearDemFiles() {
        for (int i = 0; i < this.demFiles.size(); i++) {
            ((DemFile) this.demFiles.get(i)).getOutlinePolygon().remove();
        }
        this.demFiles = null;
    }

    public void clearLoadedDemFile() {
        this.loadedDemData.getGroundOverlay().remove();
        this.loadedDemData = null;
    }

    public void loadInitialDem(Context context) {
        File file = new File(this.prefs.getString("last_dem", "foo"));
        if (!file.exists() || !file.isFile()) {
            File f = new File(this.demDirectory);
            if (!f.exists()) {
                f.mkdir();
                copyAssets(this.demDirectory, context);
                loadDem(context, this.demDirectory + "/Feldun.tif");
                setDemDirectoryPreference(this.demDirectory);
            } else if (f.isFile()) {
                for (int i = 0; i < 10; i++) {
                    this.demDirectory += Integer.toString(i);
                    File f2 = new File(this.demDirectory);
                    if (!f2.exists()) {
                        f2.mkdir();
                        copyAssets(this.demDirectory, context);
                        loadDem(context, this.demDirectory + "/Feldun.tif");
                        setDemDirectoryPreference(this.demDirectory);
                        return;
                    }
                }
            } else {
                File[] files = f.listFiles();
                int count = 0;
                for (int i2 = 0; i2 < f.listFiles().length; i2++) {
                    if (files[i2].getName().contains(".tif")) {
                        count++;
                    }
                }
                if (count == 0) {
                    copyAssets(this.demDirectory, context);
                    File feldunFile = new File(this.demDirectory + "/Feldun.tif");
                    this.demFiles.add(new DemFile(feldunFile, GdalUtils.readDemFileBounds(feldunFile), this.map));
                    loadDem(context, this.demDirectory + "/Feldun.tif");
                } else if (count == 1) {
                    loadDem(context, files[0].getName());
                } else {
                    Intent intent = new Intent("com.filebrowser.DataFileChooser");
                    intent.putExtra("path", this.demDirectory);
                    ((Activity) context).startActivityForResult(intent, 6503);
                }
            }
        } else {
            loadDem(context, file.getPath());
        }
    }

    private void copyAssets(String path, Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        String filename = "Feldun.tif";
        try {
            InputStream in = assetManager.open(filename);
            FileOutputStream fileOutputStream = new FileOutputStream(new File(path, filename));
            try {
                copyFile(in, fileOutputStream);
                in.close();
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e2) {
                FileOutputStream fileOutputStream2 = fileOutputStream;
                Log.e("tag", "Failed to copy asset file: " + filename, e2);
            }
        } catch (IOException e3) {
            Log.e("tag", "Failed to copy asset file: " + filename, e3);
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int read = in.read(buffer);
            if (read != -1) {
                out.write(buffer, 0, read);
            } else {
                return;
            }
        }
    }

    private void removeDemOutlines() {
        for (int i = 0; i < this.demFiles.size(); i++) {
            ((DemFile) this.demFiles.get(i)).getOutlinePolygon().remove();
        }
    }

    public String getDemDirectory() {
        return this.demDirectory;
    }

    public DemData getLoadedDemData() {
        return this.loadedDemData;
    }

    public void setNewDemDirectory(String demDirectory2) {
        this.demDirectory = demDirectory2;
        setDemDirectoryPreference(demDirectory2);
        clearDemFiles();
        scanDems(this.mainAct, this.writePerm);
    }

    /* access modifiers changed from: 0000 */
    public void setDemFilePreference(String filePath) {
        Editor editor = this.prefs.edit();
        editor.putString("last_dem", filePath);
        editor.commit();
    }

    public void setDemDirectoryPreference(String demDirectory2) {
        Editor editor = this.prefs.edit();
        editor.putString("dem_dir", demDirectory2);
        editor.commit();
    }

    private DemFile getDemFileFromFilePath(String filePath) {
        DemFile demFile = null;
        for (int i = 0; i < this.demFiles.size(); i++) {
            demFile = (DemFile) this.demFiles.get(i);
            if (demFile.getFilePath().equals(filePath)) {
                return demFile;
            }
        }
        return demFile;
    }

    public void registerWmacListener(WmacListener listener) {
        this.wmacListener = listener;
    }

    public ATKMap getMap() {
        return this.map;
    }

    public void setLoadedDemData(DemData demData) {
        this.loadedDemData = demData;
    }
}
