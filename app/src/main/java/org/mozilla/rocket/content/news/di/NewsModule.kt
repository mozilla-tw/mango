package org.mozilla.rocket.content.news.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.lite.newspoint.RepositoryNewsPoint
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.news.data.NewsRepository
import org.mozilla.rocket.content.news.data.NewsSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRemoteDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceCategoriesUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceLanguageUseCase
import org.mozilla.rocket.content.news.ui.NewsSettingsViewModel
import org.mozilla.rocket.content.news.ui.NewsViewModel
import java.util.HashMap
import java.util.Locale
import javax.inject.Singleton

@Module
object NewsModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsRemoteDataSource() = NewsSettingsRemoteDataSource()

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsLocalDataSource(context: Context) = NewsSettingsLocalDataSource(context)

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsRepository(
        newsSettingsRemoteDataSource: NewsSettingsRemoteDataSource,
        newsSettingsLocalDataSource: NewsSettingsLocalDataSource
    ) = NewsSettingsRepository(newsSettingsRemoteDataSource, newsSettingsLocalDataSource)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsSettingsUseCase(newsSettingsRepository: NewsSettingsRepository) =
        LoadNewsSettingsUseCase(newsSettingsRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsLanguagesUseCase(newsSettingsRepository: NewsSettingsRepository) =
        LoadNewsLanguagesUseCase(newsSettingsRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetUserPreferenceLanguageUseCase(newsSettingsRepository: NewsSettingsRepository) =
        SetUserPreferenceLanguageUseCase(newsSettingsRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetUserPreferenceCategoriesUseCase(newsSettingsRepository: NewsSettingsRepository) =
        SetUserPreferenceCategoriesUseCase(newsSettingsRepository)

    @JvmStatic
    @Provides
    fun provideNewsViewModel(loadNewsSettingsUseCase: LoadNewsSettingsUseCase) =
        NewsViewModel(loadNewsSettingsUseCase)

    @JvmStatic
    @Provides
    fun provideNewsSettingsViewModel(
        loadNewsSettingsUseCase: LoadNewsSettingsUseCase,
        loadNewsLanguagesUseCase: LoadNewsLanguagesUseCase,
        setUserPreferenceLanguageUseCase: SetUserPreferenceLanguageUseCase,
        setUserPreferenceCategoriesUseCase: SetUserPreferenceCategoriesUseCase
    ) = NewsSettingsViewModel(loadNewsSettingsUseCase, loadNewsLanguagesUseCase, setUserPreferenceLanguageUseCase, setUserPreferenceCategoriesUseCase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsRepository(
        context: Context,
        configurations: HashMap<String, String>
    ): Repository<out NewsItem> {
        val url = String.format(
            Locale.getDefault(),
            configurations[NewsRepository.CONFIG_URL] ?: "",
            configurations[NewsRepository.CONFIG_CATEGORY],
            configurations[NewsRepository.CONFIG_LANGUAGE],
            "%d",
            "%d"
        )
        return RepositoryNewsPoint(context, url)
    }
}
