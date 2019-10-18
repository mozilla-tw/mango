package org.mozilla.rocket.home.onboarding.domain

import org.mozilla.rocket.home.onboarding.data.HomeOnboardingRepository

class ShouldShowShoppingSearchOnboardingUseCase(private val repository: HomeOnboardingRepository) {
    operator fun invoke(): Boolean {
        return repository.shouldShowShoppingSearchOnboarding()
    }
}