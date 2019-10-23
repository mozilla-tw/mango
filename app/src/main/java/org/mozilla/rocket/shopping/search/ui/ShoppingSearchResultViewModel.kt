package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.switchMap
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase.ShoppingSearchSite
import org.mozilla.rocket.shopping.search.domain.SetSearchResultOnboardingIsShownUseCase
import org.mozilla.rocket.shopping.search.domain.ShouldEnableTurboModeUseCase
import org.mozilla.rocket.shopping.search.domain.ShouldShowSearchResultOnboardingUseCase

class ShoppingSearchResultViewModel(
    private val getShoppingSearchSites: GetShoppingSearchSitesUseCase,
    shouldEnableTurboMode: ShouldEnableTurboModeUseCase,
    shouldShowSearchResultOnboarding: ShouldShowSearchResultOnboardingUseCase,
    setSearchResultOnboardingIsShown: SetSearchResultOnboardingIsShownUseCase
) : ViewModel() {

    private val searchKeyword = MutableLiveData<String>()
    private val shoppingSearchSites: LiveData<List<ShoppingSearchSite>> = searchKeyword.switchMap { getShoppingSearchSites(it) }
    private var lastShoppingSearchSite: List<ShoppingSearchSite> = arrayListOf()
    val goPreferences = SingleLiveEvent<Unit>()
    val showOnboardingDialog = SingleLiveEvent<Unit>()

    val uiModel = MediatorLiveData<ShoppingSearchResultUiModel>().apply {
        addSource(shoppingSearchSites) {
            if (!lastShoppingSearchSite.equalsTo(it)) {
                lastShoppingSearchSite = it
                value = ShoppingSearchResultUiModel(it, shouldEnableTurboMode())
            }
        }
    }

    init {
        if (shouldShowSearchResultOnboarding()) {
            showOnboardingDialog.call()
            setSearchResultOnboardingIsShown()
        }
    }

    fun search(keyword: String) {
        searchKeyword.postValue(keyword)
    }

    data class ShoppingSearchResultUiModel(
        val shoppingSearchSiteList: List<ShoppingSearchSite>,
        val shouldEnableTurboMode: Boolean
    )
}

private fun <E> List<E>.equalsTo(other: List<E>): Boolean {
    if (this !== other) {
        if (this.size != other.size) {
            return false
        }
        val areNotEqual = this.asSequence()
            .zip(other.asSequence())
            .map { (fromThis, fromOther) -> fromThis == fromOther }
            .contains(false)
        if (areNotEqual) {
            return false
        }
    }
    return true
}
