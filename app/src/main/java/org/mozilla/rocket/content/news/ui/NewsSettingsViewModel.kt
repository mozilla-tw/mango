package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.focus.utils.CharacterValidator
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceCategoriesUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceLanguageUseCase

class NewsSettingsViewModel(
    private val loadNewsSettings: LoadNewsSettingsUseCase,
    private val loadNewsLanguages: LoadNewsLanguagesUseCase,
    private val setUserPreferenceLanguage: SetUserPreferenceLanguageUseCase,
    private val setUserPreferenceCategories: SetUserPreferenceCategoriesUseCase
) : ViewModel() {

    private lateinit var preferenceLanguage: NewsLanguage
    private lateinit var categories: List<NewsCategory>
    private lateinit var newsLanguages: List<NewsLanguage>

    private val _uiModel = MutableLiveData<NewsSettingsUiModel>()
    val uiModel: LiveData<NewsSettingsUiModel>
        get() = _uiModel

    init {
        getNewsSettings()
    }

    fun updateUserPreferenceLanguage(language: NewsLanguage) = viewModelScope.launch(Dispatchers.Default) {
        setUserPreferenceLanguage(language)
        getNewsSettings()
    }

    fun updateUserPreferenceCategories(
        language: String,
        userPreferenceCategories: List<NewsCategory>
    ) = viewModelScope.launch(Dispatchers.Default) {
        setUserPreferenceCategories(language, userPreferenceCategories)
        withContext(Dispatchers.Main) {
            emitUiModel(preferenceLanguage, userPreferenceCategories, newsLanguages)
        }
    }

    private fun getNewsSettings() = viewModelScope.launch(Dispatchers.Default) {
        val newsSettingsResult = loadNewsSettings()
        if (newsSettingsResult is Result.Success) {
            val newsSettings = newsSettingsResult.data

            val languagesResult = loadNewsLanguages()
            if (languagesResult is Result.Success) {
                val supportLanguages = filterSupportedLanguages(languagesResult.data)
                withContext(Dispatchers.Main) {
                    preferenceLanguage = newsSettings.newsLanguage
                    categories = newsSettings.newsCategories
                    newsLanguages = supportLanguages
                    emitUiModel(preferenceLanguage, categories, newsLanguages)
                }
            }
        }
    }

    private fun filterSupportedLanguages(rawLanguages: List<NewsLanguage>): List<NewsLanguage> {
        val supportLanguages = ArrayList<NewsLanguage>()
        rawLanguages.let {
            val displayCharacterForNotSupportedCharacter = "\u2612"
            val characterValidator = CharacterValidator(displayCharacterForNotSupportedCharacter)
            supportLanguages.addAll(it.filterNot { item -> characterValidator.characterIsMissingInFont(item.name.substring(0, 1)) })

            try {
                supportLanguages.sortBy { item -> item.code.toInt() }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
        return supportLanguages
    }

    private fun emitUiModel(preferenceLanguage: NewsLanguage, categories: List<NewsCategory>, newsLanguages: List<NewsLanguage>) {
        _uiModel.value = NewsSettingsUiModel(preferenceLanguage, categories, newsLanguages)
    }
}

data class NewsSettingsUiModel(
    val preferenceLanguage: NewsLanguage,
    val categories: List<NewsCategory>,
    val allLanguages: List<NewsLanguage>
)