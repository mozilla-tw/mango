package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.domain.PreferencesShoppingSiteUseCase

class ShoppingSearchPreferencesViewModel(
    private val preferenceShoppingSiteUseCase: PreferencesShoppingSiteUseCase
) : ViewModel() {

    val sitesLiveData = MutableLiveData<ShoppingSearchPreferencesUiModel>()

    fun loadListData() = viewModelScope.launch(Dispatchers.IO) {
        val preferencesSiteResult = preferenceShoppingSiteUseCase.invoke()
        if (preferencesSiteResult is Result.Success) {
            withContext(Dispatchers.Main) {
                val list = preferencesSiteResult.data.sortedBy {
                    it.order
                }
                sitesLiveData.value = ShoppingSearchPreferencesUiModel(list)
            }
        }
    }

    class Factory(
        val repository: ShoppingSearchSiteRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShoppingSearchPreferencesViewModel::class.java)) {
                return ShoppingSearchPreferencesViewModel(PreferencesShoppingSiteUseCase(repository)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
        }
    }

    data class ShoppingSearchPreferencesUiModel(
        val preferenceList: List<ShoppingSearchSiteRepository.PreferenceSite>
    )
}