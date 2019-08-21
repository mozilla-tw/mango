package org.mozilla.focus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.VisibleForTesting;

import org.mozilla.rocket.home.topsites.data.PinSiteManager;
import org.mozilla.rocket.home.topsites.data.PinSiteManagerKt;

public class NewFeatureNotice {

    private static final String PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN = "firstrun_shown";
    private static final String PREF_KEY_INT_FEATURE_UPGRADE_VERSION = "firstrun_upgrade_version";

    private static final int MULTI_TAB_FROM_VERSION_1_0_TO_2_0 = 1;
    private static final int FIREBASE_FROM_VERSION_2_0_TO_2_1 = 2;
    private static final int LITE_FROM_VERSION_2_1_TO_4_0 = 3;
    private static final int LITE_FROM_VERSION_4_0_TO_1_1_4 = 4;

    private static NewFeatureNotice instance;

    private final SharedPreferences preferences;

    private final PinSiteManager pinSiteManager;

    public synchronized static NewFeatureNotice getInstance(Context context) {
        if (instance == null) {
            instance = new NewFeatureNotice(context.getApplicationContext());
        }
        return instance;
    }

    private NewFeatureNotice(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.pinSiteManager = PinSiteManagerKt.getPinSiteManager(context);
    }

    public boolean shouldShowLiteUpdate() {
        boolean showPinSite = pinSiteManager.isEnabled() && pinSiteManager.isFirstTimeEnable();
        return from21to40() || showPinSite;
    }

    public boolean from21to40() {
        return LITE_FROM_VERSION_2_1_TO_4_0 > getLastShownFeatureVersion();
    }

    public void setLiteUpdateDidShow() {
        setFirstRunDidShow();
        setLastShownFeatureVersion(LITE_FROM_VERSION_4_0_TO_1_1_4);
    }

    public boolean shouldShowPrivacyPolicyUpdate() {
        if (isNewlyInstalled()) {
            setPrivacyPolicyUpdateNoticeDidShow();
            return false;
        }

        return FIREBASE_FROM_VERSION_2_0_TO_2_1 == getLastShownFeatureVersion() + 1;
    }

    public void setPrivacyPolicyUpdateNoticeDidShow() {
        setLastShownFeatureVersion(FIREBASE_FROM_VERSION_2_0_TO_2_1);
    }

    public boolean hasShownFirstRun() {
        return preferences.getBoolean(PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN, false);
    }

    public void setFirstRunDidShow() {
        preferences.edit()
                .putBoolean(PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN, true)
                .apply();
    }

    @VisibleForTesting
    public void resetFirstRunDidShow() {
        preferences.edit()
                .putBoolean(PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN, false)
                .putInt(PREF_KEY_INT_FEATURE_UPGRADE_VERSION, 0)
                .apply();
    }

    private boolean isNewlyInstalled() {
        return !hasShownFirstRun() && (getLastShownFeatureVersion() == 0);
    }

    private void setLastShownFeatureVersion(int featureVersion) {
        if (getLastShownFeatureVersion() >= featureVersion) {
            return;
        }

        preferences.edit()
                .putInt(PREF_KEY_INT_FEATURE_UPGRADE_VERSION, featureVersion)
                .apply();
    }

    public int getLastShownFeatureVersion() {
        return preferences.getInt(PREF_KEY_INT_FEATURE_UPGRADE_VERSION, 0);
    }
}
