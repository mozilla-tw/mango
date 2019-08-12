/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.InfoActivity;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SupportUtils;

/**
 * Created by ylai on 2017/9/21.
 */

public class TurboSwitchPreference extends SwitchPreference {

    public TurboSwitchPreference(Context context) {
        super(context);
        init();
    }

    public TurboSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TurboSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // We are keeping track of the preference value ourselves.
        setPersistent(false);

        setChecked(Settings.getInstance(getContext()).shouldUseTurboMode());
        setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.getInstance(preference.getContext()).setTurboMode((Boolean) newValue);
            notifyChanged();
            return true;
        });
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final Switch switchWidget = (Switch) holder.findViewById(R.id.switch_widget);
        switchWidget.setChecked(isChecked());

        final TextView summary = (TextView) holder.findViewById(android.R.id.summary);

        TypedValue typedValue = new TypedValue();
        TypedArray ta = getContext().obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorLink});
        int color = ta.getColor(0, 0);
        ta.recycle();

        summary.setTextColor(color);
        summary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is a hardcoded link: if we ever end up needing more of these links, we should
                // move the link into an xml parameter, but there's no advantage to making it configurable now.
                final String url = SupportUtils.getSumoURLForTopic(getContext(), "turbo");
                final String title = getTitle().toString();

                final Intent intent = InfoActivity.getIntentFor(getContext(), url, title);
                getContext().startActivity(intent);
                TelemetryWrapper.settingsLearnMoreClickEvent(getContext().getString(R.string.pref_key_turbo_mode));
            }
        });
    }
}
