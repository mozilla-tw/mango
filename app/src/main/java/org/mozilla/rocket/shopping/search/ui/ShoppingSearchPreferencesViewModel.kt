package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.domain.PreferencesShoppingSiteUseCase
import org.mozilla.rocket.shopping.search.domain.UpdatePreferenceShoppingSiteUseCase
import org.mozilla.rocket.shopping.search.ui.adapter.SiteViewHolder

class ShoppingSearchPreferencesViewModel(
    private val preferenceShoppingSiteUseCase: PreferencesShoppingSiteUseCase,
    private val updatePreferencesShoppingSiteUseCase: UpdatePreferenceShoppingSiteUseCase
) : ViewModel() {

    private lateinit var callback: ViewModelCallBack
    val sitesLiveData = MutableLiveData<MutableList<SiteViewHolder.PreferencesUiModel>>()

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
                handleAllSwitch(uiModelList)
                sitesLiveData.value = uiModelList
            }
        }
    }

    private fun handleAllSwitch(preferencesList: MutableList<SiteViewHolder.PreferencesUiModel>) {
        var toggleOnCounter = 0
        preferencesList.forEach {
            if (it.data.toggleOn) {
                toggleOnCounter++
            }
        }

        if (toggleOnCounter <= 2) {
            if (toggleOnCounter < 2) {
                preferencesList[0].data.toggleOn = true
                preferencesList[1].data.toggleOn = true
            }

            preferencesList.forEach {
                it.data.toggleEnable = true
                if (it.data.toggleOn) {
                    it.data.toggleEnable = false
                }
            }
        } else {
            preferencesList.forEach {
                it.data.toggleEnable = true
            }
        }
    }

    fun notifyItemMoved(fromPosition: Int, toPosition: Int) {
        sitesLiveData.value?.let {
            updatePreferencesShoppingSiteUseCase.update(it, fromPosition, toPosition)
        }

        // reset adapter list will stop drag action
        callback.notifyItemMoved(fromPosition, toPosition)
    }

    fun onItemSwitchChange(index: Int, isChecked: Boolean) {
        val preferencesList = sitesLiveData.value
        preferencesList?.let {
            it[index].data.toggleOn = isChecked
            handleAllSwitch(it)
        }
        sitesLiveData.value = preferencesList
    }

    fun setCallBack(callBack: ViewModelCallBack) {
        this.callback = callBack
    }

    interface ViewModelCallBack {
        fun notifyItemMoved(fromPosition: Int, toPosition: Int)
    }
}