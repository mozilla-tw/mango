/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.InfoActivity;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SupportUtils;

// This class handles all click/actions users performed on a notification.
// This ensures that all telemetry works for action/click are in one place.
// The UI code will be responsible for telemetry work for displaying the notifications.
public class NotificationActionBroadcastReceiver extends BroadcastReceiver {


    private static final String TAG = "NotifyActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        final String action = intent.getAction();
        final Bundle bundle = intent.getExtras();
        if (bundle == null || action == null || !IntentUtils.ACTION_NOTIFICATION.equals(action)) {
            return;
        }
        Intent nexStep = null;

        if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_DELETE_FIREBASE_NOTIFICATION)) {
            String messageId = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_MESSAGE_ID, "");
            String link = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_LINK, "");
            TelemetryWrapper.dismissNotification(link, messageId);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_CLICK_FIREBASE_NOTIFICATION)) {
            nexStep = new Intent();
            nexStep.setClassName(context, AppConstants.LAUNCHER_ACTIVITY_ALIAS);
            nexStep.putExtra(FirebaseMessagingServiceWrapper.PUSH_OPEN_URL, intent.getStringExtra(IntentUtils.EXTRA_NOTIFICATION_OPEN_URL));
            nexStep.putExtra(FirebaseMessagingServiceWrapper.PUSH_COMMAND, intent.getStringExtra(IntentUtils.EXTRA_NOTIFICATION_COMMAND));
            nexStep.putExtra(FirebaseMessagingServiceWrapper.PUSH_DEEP_LINK, intent.getStringExtra(IntentUtils.EXTRA_NOTIFICATION_DEEP_LINK));

            String messageId = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_MESSAGE_ID, "");
            String link = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_LINK, "");
            TelemetryWrapper.openNotification(link, messageId, false);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_ACTION_RATE_STAR)) {

            IntentUtils.goToPlayStore(context);

            TelemetryWrapper.clickRateApp(TelemetryWrapper.Value.POSITIVE, TelemetryWrapper.Extra_Value.NOTIFICATION);

            NotificationManagerCompat.from(context).cancel(NotificationId.LOVE_FIREFOX);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_ACTION_FEEDBACK)) {

            nexStep = IntentUtils.createInternalOpenUrlIntent(context,
                    context.getString(R.string.rate_app_feedback_url), true);

            // Users set negative feedback, don't ask them to rate/feedback again.
            Settings.getInstance(context).setShareAppDialogDidShow();

            TelemetryWrapper.clickRateApp(TelemetryWrapper.Value.NEGATIVE, TelemetryWrapper.Extra_Value.NOTIFICATION);

            NotificationManagerCompat.from(context).cancel(NotificationId.LOVE_FIREFOX);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_CLICK_DEFAULT_BROWSER)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                nexStep = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            } else {
                final String fallbackTitle = context.getString(R.string.preference_default_browser) + "\uD83D\uDE4C";
                nexStep = InfoActivity.getIntentFor(context, SupportUtils.getSumoURLForTopic(context, "rocket-default"), fallbackTitle);
            }

            TelemetryWrapper.clickDefaultSettingNotification();

            NotificationManagerCompat.from(context).cancel(NotificationId.DEFAULT_BROWSER);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_CLICK_LOVE_FIREFOX)) {
            nexStep = new Intent();
            nexStep.setClassName(context, AppConstants.LAUNCHER_ACTIVITY_ALIAS);
            nexStep.putExtra(IntentUtils.EXTRA_SHOW_RATE_DIALOG, true);

            TelemetryWrapper.clickRateApp(null, TelemetryWrapper.Extra_Value.NOTIFICATION);

            NotificationManagerCompat.from(context).cancel(NotificationId.LOVE_FIREFOX);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_CLICK_PRIVACY_POLICY_UPDATE)) {
            nexStep = IntentUtils.createInternalOpenUrlIntent(context, SupportUtils.getPrivacyURL(), true);

            // TODO: telemetry

            NotificationManagerCompat.from(context).cancel(NotificationId.PRIVACY_POLICY_UPDATE);

        } else {
            Log.e(TAG, "Not a valid action");
        }

        bundle.clear();
        if (nexStep != null) {
            nexStep.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(nexStep);
        }
    }

}
