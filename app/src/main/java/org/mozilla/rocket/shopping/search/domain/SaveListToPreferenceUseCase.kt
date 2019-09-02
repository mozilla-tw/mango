package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.ui.adapter.SiteViewHolder

class SaveListToPreferenceUseCase(private val repository: ShoppingSearchSiteRepository) {
    operator fun invoke(sitesList: MutableList<SiteViewHolder.PreferencesUiModel>) {
        repository.updatePreferenceSiteData(sitesList)
    }
}