package org.mozilla.rocket.shopping.search.domain

import androidx.lifecycle.MutableLiveData
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.ui.adapter.SiteViewHolder

class PreferencesShoppingSiteUseCase(val repository: ShoppingSearchSiteRepository) {

    suspend operator fun invoke(): Result<List<ShoppingSearchSiteRepository.PreferenceSite>> {
        return repository.fetchPreferenceSites()
    }

    fun swapList(sitesLiveData: MutableLiveData<MutableList<SiteViewHolder.PreferencesUiModel>>): MutableList<SiteViewHolder.PreferencesUiModel>? {
        var toggleOnCounter = 0
        val preferencesList = sitesLiveData.value
        preferencesList?.forEach {
            if (it.data.toggleOn) {
                toggleOnCounter++
            }
        }

        if (toggleOnCounter <= 2) {
            if (toggleOnCounter < 2) {
                preferencesList?.get(0)?.data?.toggleOn = true
                preferencesList?.get(1)?.data?.toggleOn = true
            }

            preferencesList?.forEach {
                it.data.toggleEnable = true
                if (it.data.toggleOn) {
                    it.data.toggleEnable = false
                }
            }
        } else {
            preferencesList?.forEach {
                it.data.toggleEnable = true
            }
        }
        return preferencesList
    }
}