package org.mozilla.rocket.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import org.mozilla.rocket.chrome.BottomBarViewModel
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.MenuViewModel
import org.mozilla.rocket.chrome.PrivateBottomBarViewModel
import org.mozilla.rocket.content.games.ui.GamesViewModel
import org.mozilla.rocket.content.news.NewsViewModel
import org.mozilla.rocket.download.DownloadIndicatorViewModel
import org.mozilla.rocket.home.HomeViewModel
import org.mozilla.rocket.urlinput.QuickSearchViewModel
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KClass

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(ChromeViewModel::class)
    abstract fun bindChromeViewModel(viewModel: ChromeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DownloadIndicatorViewModel::class)
    abstract fun bindDownloadIndicatorViewModel(viewModel: DownloadIndicatorViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BottomBarViewModel::class)
    abstract fun bindBottomBarViewModel(viewModel: BottomBarViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PrivateBottomBarViewModel::class)
    abstract fun bindPrivateBottomBarViewModel(viewModel: PrivateBottomBarViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MenuViewModel::class)
    abstract fun bindMenuViewModel(viewModel: MenuViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(QuickSearchViewModel::class)
    abstract fun bindQuickSearchViewModel(viewModel: QuickSearchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(viewModel: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NewsViewModel::class)
    abstract fun bindNewsViewModel(viewModel: NewsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GamesViewModel::class)
    abstract fun bindGamesViewModel(viewModel: GamesViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: AppViewModelFactory): ViewModelProvider.Factory
}

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)

@Singleton
class AppViewModelFactory @Inject constructor(
    private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val creator = creators[modelClass]
            ?: creators.entries.firstOrNull { modelClass.isAssignableFrom(it.key) }?.value
            ?: throw IllegalArgumentException("unknown model class $modelClass")
        try {
            @Suppress("UNCHECKED_CAST")
            return creator.get() as T
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}