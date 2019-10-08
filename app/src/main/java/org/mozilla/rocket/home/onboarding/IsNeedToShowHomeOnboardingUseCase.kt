package org.mozilla.rocket.home.onboarding

import android.content.Context
import org.mozilla.focus.utils.NewFeatureNotice

class IsNeedToShowHomeOnboardingUseCase(private val context: Context) {

    operator fun invoke(): Boolean = !NewFeatureNotice.getInstance(context).hasHomePageOnboardingShown()
}