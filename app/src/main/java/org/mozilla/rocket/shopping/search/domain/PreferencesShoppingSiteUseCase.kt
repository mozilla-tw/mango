package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository

class PreferencesShoppingSiteUseCase(val repository: ShoppingSearchSiteRepository) {

    suspend operator fun invoke(): Result<List<ShoppingSearchSiteRepository.PreferenceSite>> {
        return repository.fetchPreferenceSites()
    }
}