package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.shopping.search.domain.PreferencesShoppingSiteUseCase
import org.mozilla.rocket.shopping.search.ui.adapter.SiteViewHolder

class ShoppingSearchPreferencesViewModel(
    private val preferenceShoppingSiteUseCase: PreferencesShoppingSiteUseCase
) : ViewModel() {

    val sitesLiveData = MutableLiveData<MutableList<SiteViewHolder.PreferencesUiModel>>()
    val notifyItemMoved = SingleLiveEvent<Swap>()
    val switchCheckChange = SingleLiveEvent<SwitchCheck>()
    val resetLiveData = SingleLiveEvent<MutableList<SiteViewHolder.PreferencesUiModel>>()

    init {
        loadListData()
    }

    private fun loadListData() = viewModelScope.launch(Dispatchers.IO) {
        val preferencesSiteResult = preferenceShoppingSiteUseCase.invoke()
        if (preferencesSiteResult is Result.Success) {
            withContext(Dispatchers.Main) {
                val list = preferencesSiteResult.data.sortedBy {
                    it.order
                }
                val uiModelList = mutableListOf<SiteViewHolder.PreferencesUiModel>()
                list.forEach {
                    uiModelList.add(SiteViewHolder.PreferencesUiModel(it))
                }
                sitesLiveData.value = uiModelList
            }
        }
    }

    fun handleAllSwitch() {
        val list = preferenceShoppingSiteUseCase.swapList(sitesLiveData)
        list?.let {
            resetLiveData.value = it
        }
    }

    data class Swap(
        val fromPosition: Int,
        val toPosition: Int
    )

    data class SwitchCheck(
        val index: Int,
        val isChecked: Boolean
    )
}