package org.mozilla.rocket.shopping.search.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.shopping.search.data.KeywordSuggestionRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSearchLocalDataSource
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRemoteDataSource
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository
import org.mozilla.rocket.shopping.search.domain.FetchKeywordSuggestionUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.domain.SetSearchInputOnboardingIsShownUseCase
import org.mozilla.rocket.shopping.search.domain.SetSearchResultOnboardingIsShownUseCase
import org.mozilla.rocket.shopping.search.domain.ShouldShowSearchInputOnboardingUseCase
import org.mozilla.rocket.shopping.search.domain.ShouldShowSearchResultOnboardingUseCase
import org.mozilla.rocket.shopping.search.domain.UpdateShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchBottomBarViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchContentSwitchOnboardingViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchKeywordInputViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchPreferencesViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchResultViewModel
import javax.inject.Singleton

@Module
object ShoppingSearchModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideKeywordSuggestionRepository(appContext: Context): KeywordSuggestionRepository = KeywordSuggestionRepository(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideFetchKeywordSuggestionUseCase(repo: KeywordSuggestionRepository): FetchKeywordSuggestionUseCase =
        FetchKeywordSuggestionUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingSearchKeywordInputViewModel(
        fetchKeywordUseCase: FetchKeywordSuggestionUseCase,
        shouldShowSearchInputOnboardingUseCase: ShouldShowSearchInputOnboardingUseCase,
        setSearchInputOnboardingIsShownUseCase: SetSearchInputOnboardingIsShownUseCase
    ): ShoppingSearchKeywordInputViewModel =
        ShoppingSearchKeywordInputViewModel(fetchKeywordUseCase, shouldShowSearchInputOnboardingUseCase, setSearchInputOnboardingIsShownUseCase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchRemoteDataSource(): ShoppingSearchRemoteDataSource =
        ShoppingSearchRemoteDataSource()

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchLocalDataSource(context: Context): ShoppingSearchLocalDataSource =
        ShoppingSearchLocalDataSource(context)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchRepository(
        shoppingSearchRemoteDataSource: ShoppingSearchRemoteDataSource,
        shoppingSearchLocalDataSource: ShoppingSearchLocalDataSource
    ): ShoppingSearchRepository = ShoppingSearchRepository(shoppingSearchRemoteDataSource, shoppingSearchLocalDataSource)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSearchShoppingSiteUseCase(repo: ShoppingSearchRepository) = GetShoppingSearchSitesUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingSearchResultViewModel(
        getShoppingSearchSitesUseCase: GetShoppingSearchSitesUseCase,
        shouldShowSearchResultOnboardingUseCase: ShouldShowSearchResultOnboardingUseCase,
        setSearchResultOnboardingIsShownUseCase: SetSearchResultOnboardingIsShownUseCase
    ): ShoppingSearchResultViewModel =
        ShoppingSearchResultViewModel(getShoppingSearchSitesUseCase, shouldShowSearchResultOnboardingUseCase, setSearchResultOnboardingIsShownUseCase)

    @JvmStatic
    @Provides
    fun provideShoppingSearchBottomBarViewModel(): ShoppingSearchBottomBarViewModel =
        ShoppingSearchBottomBarViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun providePreferencesShoppingSiteUseCase(repo: ShoppingSearchRepository) = GetShoppingSitesUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSaveListToPreferenceUseCase(repo: ShoppingSearchRepository) = UpdateShoppingSitesUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingSearchPreferencesViewModel(usecase: GetShoppingSitesUseCase, saveUseCase: UpdateShoppingSitesUseCase): ShoppingSearchPreferencesViewModel =
        ShoppingSearchPreferencesViewModel(usecase, saveUseCase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldShowSearchResultOnboardingUseCase(repo: ShoppingSearchRepository): ShouldShowSearchResultOnboardingUseCase =
        ShouldShowSearchResultOnboardingUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingSearchOnboardingViewModel(): ShoppingSearchContentSwitchOnboardingViewModel =
        ShoppingSearchContentSwitchOnboardingViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetSearchResultOnboardingIsShownUseCase(repo: ShoppingSearchRepository): SetSearchResultOnboardingIsShownUseCase =
        SetSearchResultOnboardingIsShownUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldShowSearchInputOnboardingUseCase(repo: ShoppingSearchRepository): ShouldShowSearchInputOnboardingUseCase =
        ShouldShowSearchInputOnboardingUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetSearchInputOnboardingIsShownUseCase(repo: ShoppingSearchRepository): SetSearchInputOnboardingIsShownUseCase =
        SetSearchInputOnboardingIsShownUseCase(repo)
}