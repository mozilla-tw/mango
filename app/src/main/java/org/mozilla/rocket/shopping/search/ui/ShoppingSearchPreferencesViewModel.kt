package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.rocket.shopping.search.domain.PreferencesShoppingSiteUseCase
import org.mozilla.rocket.shopping.search.domain.SaveListToPreferenceUseCase
import org.mozilla.rocket.shopping.search.domain.UpdatePreferenceShoppingSiteUseCase
import org.mozilla.rocket.shopping.search.ui.adapter.SiteViewHolder.PreferencesUiModel

class ShoppingSearchPreferencesViewModel(
    private val preferenceShoppingSiteUseCase: PreferencesShoppingSiteUseCase,
    private val updatePreferencesShoppingSiteUseCase: UpdatePreferenceShoppingSiteUseCase,
    private val saveListToPreferenceUseCase: SaveListToPreferenceUseCase
) : ViewModel() {

    private lateinit var callback: ViewModelCallBack
    private val preferenceSiteList = mutableListOf<PreferencesUiModel>()
    var preferenceSitesLiveData = MediatorLiveData<MutableList<PreferencesUiModel>>()

    init {
        getLiveData()
    }

    private fun getLiveData() {
        viewModelScope.launch(Dispatchers.Main) {
            preferenceSitesLiveData.addSource(preferenceShoppingSiteUseCase.invoke()) {
                preferenceSitesLiveData.postValue(it)
            }
        }
    }

    fun setList(list: MutableList<PreferencesUiModel>) {
        preferenceSiteList.clear()
        preferenceSiteList.addAll(list)
        preferenceSiteList.sortBy {
            it.data.order
        }
        handleAllSwitch(preferenceSiteList)
    }

    private fun handleAllSwitch(list: MutableList<PreferencesUiModel>) {
        var toggleOnCounter = 0
        list.forEach {
            if (it.data.isChecked) {
                toggleOnCounter++
            }
        }

        if (toggleOnCounter <= 2) {
            if (toggleOnCounter < 2) {
                list[0].data.isChecked = true
                list[1].data.isChecked = true
            }

            list.forEach {
                it.isEnabled = true
                if (it.data.isChecked) {
                    it.isEnabled = false
                }
            }
        } else {
            list.forEach {
                it.isEnabled = true
            }
        }
    }

    fun notifyItemMoved(fromPosition: Int, toPosition: Int) {
        preferenceSiteList.let {
            updatePreferencesShoppingSiteUseCase.update(it, fromPosition, toPosition)
        }

        // reset adapter list will stop drag action
        callback.notifyItemMoved(fromPosition, toPosition)
    }

    fun onItemSwitchChange(index: Int, isChecked: Boolean) {
        preferenceSiteList.let {
            it[index].data.isChecked = isChecked
            handleAllSwitch(it)
        }
        saveListToPreferenceUseCase.invoke(preferenceSiteList)
    }

    fun setCallBack(callBack: ViewModelCallBack) {
        this.callback = callBack
    }

    fun notifyItemDropped() {
        saveListToPreferenceUseCase.invoke(preferenceSiteList)
    }

    interface ViewModelCallBack {
        fun notifyItemMoved(fromPosition: Int, toPosition: Int)
    }
}