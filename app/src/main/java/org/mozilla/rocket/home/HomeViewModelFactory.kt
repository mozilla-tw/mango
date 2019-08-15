package org.mozilla.rocket.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase

class HomeViewModelFactory(
    private val settings: Settings,
    private val getTopSitesUseCase: GetTopSitesUseCase
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(settings, getTopSitesUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}