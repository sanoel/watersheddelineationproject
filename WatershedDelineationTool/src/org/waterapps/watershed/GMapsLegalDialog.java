package org.waterapps.watershed;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by steve on 7/17/13.
 */
public class GMapsLegalDialog extends DialogPreference {
    public GMapsLegalDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogMessage(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(context));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
        }
    }
}
