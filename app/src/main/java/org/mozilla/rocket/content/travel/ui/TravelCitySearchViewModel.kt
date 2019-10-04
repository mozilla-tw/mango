package org.mozilla.rocket.content.travel.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.domain.SearchCityUseCase

class TravelCitySearchViewModel(private val searchCityUseCase: SearchCityUseCase) : ViewModel() {

    private val _items = MutableLiveData<List<CitySearchResultUiModel>>()
    val items: LiveData<List<CitySearchResultUiModel>> = _items

    private var searchCityJob: Job? = null

    fun search(keyword: String) {
        if (searchCityJob?.isCompleted == false) {
            searchCityJob?.cancel()
        }

        searchCityJob = viewModelScope.launch {
            val result = searchCityUseCase(keyword)
            if (result is Result.Success) {
                _items.postValue(result.data)
            }

            // TODO: handle error
        }
    }
}