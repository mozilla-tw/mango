/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.search;

import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.Settings;

/**
 * Preference for setting the default search engine.
 */
public class SearchEnginePreference extends DialogPreference {
    public SearchEnginePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchEnginePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSummaryProvider(new SummaryProvider<SearchEnginePreference>() {
            @Override
            public CharSequence provideSummary(SearchEnginePreference preference) {
                return SearchEngineManager.getInstance().getDefaultSearchEngine(getContext()).getName();
            }
        });
    }


    @Override
    protected void onClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        final SearchEngineAdapter adapter = new SearchEngineAdapter(getContext());

        builder.setTitle(R.string.preference_dialog_title_search_engine);

        builder.setAdapter(adapter, (dialog, which) -> {
            persistSearchEngine(adapter.getItem(which));
            notifyChanged();
            dialog.dismiss();
        });

        builder.create().show();
    }

    private void persistSearchEngine(SearchEngine searchEngine) {
        Settings.getInstance(getContext())
                .setDefaultSearchEngine(searchEngine);
    }
}
