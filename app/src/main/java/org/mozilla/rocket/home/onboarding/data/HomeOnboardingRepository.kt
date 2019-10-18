package org.mozilla.rocket.home.onboarding.data

import android.content.Context
import org.mozilla.strictmodeviolator.StrictModeViolation

class HomeOnboardingRepository(private val appContext: Context) {

    private val preference by lazy {
        StrictModeViolation.tempGrant({ builder ->
            builder.permitDiskReads()
        }, {
            appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        })
    }

    fun shouldShowShoppingSearchOnboarding() =
        preference.getBoolean(KEY_SHOPPING_SEARCH_ONBOARDING, true)

    fun setShoppingSearchOnboardingIsShown() =
        preference.edit().putBoolean(KEY_SHOPPING_SEARCH_ONBOARDING, false).apply()

    companion object {
        private const val PREF_NAME = "home_shopping_search"
        private const val KEY_SHOPPING_SEARCH_ONBOARDING = "shopping_search_onboarding"
    }
}