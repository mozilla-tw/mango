package org.mozilla.rocket.shopping.search.domain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository.PreferenceSite
import org.mozilla.rocket.shopping.search.ui.adapter.SiteViewHolder.PreferencesUiModel

class PreferencesShoppingSiteUseCase(val repository: ShoppingSearchSiteRepository) {

    suspend operator fun invoke(): LiveData<MutableList<PreferencesUiModel>> {
        val sitesLiveData = repository.fetchSites()
        return Transformations.switchMap(sitesLiveData) { siteList ->
            getPreferenceUiLiveData(siteList)
        }
    }

    private fun getPreferenceUiLiveData(list: Result<MutableList<PreferenceSite>>): MutableLiveData<MutableList<PreferencesUiModel>> {
        val preferenceSiteList = mutableListOf<PreferencesUiModel>()
        if (list is Result.Success) {
            list.data.forEach {
                preferenceSiteList.add(PreferencesUiModel(it))
            }
        }
        val liveData = MutableLiveData<MutableList<PreferencesUiModel>>()
        liveData.value = preferenceSiteList
        return liveData
    }
}