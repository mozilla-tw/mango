/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import androidx.preference.ListPreference;
import androidx.annotation.WorkerThread;
import androidx.preference.PreferenceViewHolder;

import android.text.TextUtils;
import android.util.AttributeSet;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.StorageUtils;
import org.mozilla.threadutils.ThreadUtils;

public class DataSavingPathPreference extends ListPreference {
    private static final String LOG_TAG = "DataSavingPathPreference";

    private boolean hasRemovableStorage = false;

    public DataSavingPathPreference(Context context) {
        super(context);
        init();
    }

    public DataSavingPathPreference(Context context, AttributeSet attributes) {
        super(context, attributes);
        init();
    }
    public DataSavingPathPreference(Context context, AttributeSet attributes, int defStyle) {
        super(context, attributes, defStyle);
        init();
    }

    private void init() {
        setSummaryProvider(preference -> {

            // design's spec, always show 'save to internal' if there is no removable storage
            if (!hasRemovableStorage) {
                return getContext().getResources().getString(R.string.setting_dialog_internal_storage);
            }

            if (TextUtils.isEmpty(getEntry())) {
                final String[] entries = getContext().getResources().getStringArray(R.array.data_saving_path_entries);
                setValueIndex(0);
                return entries[0];
            }

            return getEntry();

        });
    }

    @Override
    public void onAttached() {
        super.onAttached();

        // Put pingRemovableStorage() in background thread to avoid strict mode violation: disk I/O on main thread.
        ThreadUtils.postToBackgroundThread(new Runnable() {
            @Override
            public void run() {
                pingRemovableStorage();
            }
        });
    }

    @WorkerThread
    private void pingRemovableStorage() {
        try {
            // This must be called in a background thread cause it has I/O access.
            StorageUtils.getAppMediaDirOnRemovableStorage(getContext());
            // no exception
            hasRemovableStorage = true;
        } catch (NoRemovableStorageException e) {
            hasRemovableStorage = false;
        }

        super.setEnabled(hasRemovableStorage);

        // notifyChanged() will update the UI so it must be called in main thread.
        ThreadUtils.postToMainThread(new Runnable() {
            @Override
            public void run() {
                // ensure Summary sync to current state
                DataSavingPathPreference.this.notifyChanged();
            }
        });

    }
}
