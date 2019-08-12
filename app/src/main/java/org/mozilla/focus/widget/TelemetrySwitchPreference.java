/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.InfoActivity;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.FirebaseHelper;
import org.mozilla.focus.utils.SupportUtils;

/**
 * Ideally we'd extend SwitchPreference, and only do the summary modification. Unfortunately
 * that results in us using an older Switch which animates differently to the (seemingly AppCompat)
 * switches used in the remaining preferences. There's no AppCompat SwitchPreference to extend,
 * so instead we just build our own preference.
 */
public class TelemetrySwitchPreference extends SwitchPreference {

    public TelemetrySwitchPreference(Context context) {
        super(context);
        init();
    }

    public TelemetrySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TelemetrySwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSummaryProvider(preference -> {
            final Resources resources = preference.getContext().getResources();
            final String appName = resources.getString(R.string.app_name);
            final String mozilla = resources.getString(R.string.mozilla);
            return resources.getString(R.string.preference_mozilla_telemetry_summary, appName, mozilla);
        });

        setChecked(TelemetryWrapper.isTelemetryEnabled(getContext()));
        setOnPreferenceChangeListener((preference, newValue) -> {
            TelemetryWrapper.setTelemetryEnabled(getContext(), (Boolean) newValue);
            // we should use the value from UI (isChecked) instead of relying on SharePreference.
            FirebaseHelper.enableAnalytics(getContext().getApplicationContext(), isEnabled());
            notifyChanged();
            return true;
        });

    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView learnMore = (TextView) holder.findViewById(R.id.learnMore);

        learnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is a hardcoded link: if we ever end up needing more of these links, we should
                // move the link into an xml parameter, but there's no advantage to making it configurable now.
                final String url = SupportUtils.getSumoURLForTopic(getContext(), "usage-data");
                final String title = getTitle().toString();

                final Intent intent = InfoActivity.getIntentFor(getContext(), url, title);
                getContext().startActivity(intent);
                TelemetryWrapper.settingsLearnMoreClickEvent(getContext().getString(R.string.pref_key_telemetry));
            }
        });
    }
}
