package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettings
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase

class NewsTabViewModel(private val loadNewsSettingsUseCase: LoadNewsSettingsUseCase) : ViewModel() {

    private val _uiModel = MutableLiveData<NewsTabUiModel>()
    val uiModel: LiveData<NewsTabUiModel>
        get() = _uiModel

    init {
        getNewsSettings()
    }

    fun getNewsSettings() = viewModelScope.launch(Dispatchers.Default) {
        val result = loadNewsSettingsUseCase()
        if (result is Result.Success) {
            withContext(Dispatchers.Main) { emitUiModel(result.data) }
        }
    }

    fun refresh() {
        _uiModel.value = NewsTabUiModel(
            Pair(LoadNewsLanguagesUseCase.DEFAULT_LANGUAGE, emptyList()),
            _uiModel.value?.hasSettingsMenu ?: false
        )
        getNewsSettings()
    }

    private fun emitUiModel(newsSettings: NewsSettings) {
        _uiModel.value = NewsTabUiModel(
            Pair(newsSettings.newsLanguage, newsSettings.newsCategories.filter { it.isSelected }),
            newsSettings.shouldEnableNewsSettings
        )
    }
}

data class NewsTabUiModel(
    val newsSettings: Pair<NewsLanguage, List<NewsCategory>>,
    val hasSettingsMenu: Boolean
)