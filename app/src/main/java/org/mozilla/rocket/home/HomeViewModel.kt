package org.mozilla.rocket.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.SitePage

class HomeViewModel(
    private val settings: Settings,
    private val topSitesRepo: TopSitesRepo
) : ViewModel() {

    val sitePages: LiveData<List<SitePage>> = topSitesRepo.topSites.map { it.toSitePages() }
    val pinEnabled = MutableLiveData<Boolean>().apply { topSitesRepo.isPinEnabled() }

    val toggleBackgroundColor = SingleLiveEvent<Unit>()
    val resetBackgroundColor = SingleLiveEvent<Unit>()
    val topSiteClicked = SingleLiveEvent<Site>()
    val topSiteLongClicked = SingleLiveEvent<Site>()

    private fun List<Site>.toSitePages() = chunked(TOP_SITES_PER_PAGE)
            .filterIndexed { index, _ -> index < TOP_SITES_MAX_PAGE_SIZE }
            .map { SitePage(it) }

    fun updateTopSitesData() {
        topSitesRepo.updateTopSitesData()
    }

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

    fun onTopSiteClicked(site: Site, position: Int) {
        topSiteClicked.value = site
        val allowToLogTitle = when (site) {
            is Site.FixedSite -> true
            is Site.RemovableSite -> site.isDefault
        }
        val title = if (allowToLogTitle) site.title else ""
        TelemetryWrapper.clickTopSiteOn(position, title)
    }

    fun onTopSiteLongClicked(site: Site): Boolean =
            if (site is Site.RemovableSite) {
                topSiteLongClicked.value = site
                true
            } else {
                false
            }

    fun onPinTopSiteClicked(site: Site) {
        topSitesRepo.pin(site)
    }

    fun onRemoveTopSiteClicked(site: Site) {
        topSitesRepo.remove(site)
    }

    companion object {
        private const val TOP_SITES_MAX_PAGE_SIZE = 2
        private const val TOP_SITES_PER_PAGE = 8
    }
}