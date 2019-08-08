package org.mozilla.rocket.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.home.topsites.Site
import org.mozilla.rocket.home.topsites.SitePage
import org.mozilla.rocket.home.topsites.repository.TopSitesRepo

class HomeViewModel(
    private val settings: Settings,
    topSitesRepo: TopSitesRepo
) : ViewModel() {

    val sitePages: LiveData<List<SitePage>> = topSitesRepo.topSites.map { it.toSitePages() }

    val toggleBackgroundColor = SingleLiveEvent<Unit>()
    val resetBackgroundColor = SingleLiveEvent<Unit>()
    val topSiteClicked = SingleLiveEvent<Site>()

    private fun List<Site>.toSitePages() = chunked(TOP_SITES_PER_PAGE)
            .filterIndexed { index, _ -> index < TOP_SITES_MAX_PAGE_SIZE }
            .map { SitePage(it) }

    fun onBackgroundViewDoubleTap(): Boolean {
        // Not allowed double tap to switch theme when night mode is on
        if (settings.isNightModeEnable) return false

        toggleBackgroundColor.call()
        return true
    }

    fun onBackgroundViewLongPress() {
        // Not allowed long press to reset theme when night mode is on
        if (settings.isNightModeEnable) return

        resetBackgroundColor.call()
    }

    fun onShoppingButtonClicked() {
        // TODO:
    }

    fun onTopSiteClicked(site: Site) {
        topSiteClicked.value = site
    }

    companion object {
        private const val TOP_SITES_MAX_PAGE_SIZE = 2
        private const val TOP_SITES_PER_PAGE = 8
    }
}