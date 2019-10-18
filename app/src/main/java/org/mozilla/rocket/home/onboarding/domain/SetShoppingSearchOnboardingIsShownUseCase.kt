package org.mozilla.rocket.home.onboarding.domain

import org.mozilla.rocket.home.onboarding.data.HomeOnboardingRepository

class SetShoppingSearchOnboardingIsShownUseCase(private val repository: HomeOnboardingRepository) {
    operator fun invoke() {
        repository.setShoppingSearchOnboardingIsShown()
    }
}