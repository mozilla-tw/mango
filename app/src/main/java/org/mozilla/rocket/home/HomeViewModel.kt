package org.mozilla.rocket.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.TopSitesUtils
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.home.topsites.Site
import org.mozilla.rocket.home.topsites.SitePage

class HomeViewModel(
    private val settings: Settings
) : ViewModel() {

    val sitePages = MutableLiveData<List<SitePage>>()

    val toggleBackgroundColor = SingleLiveEvent<Unit>()
    val resetBackgroundColor = SingleLiveEvent<Unit>()
    val topSiteClicked = SingleLiveEvent<Site>()

    init {
        initFakeSites()
    }

    private fun initFakeSites() {
        sitePages.value = generateFakeSites().toSitePages()
    }

    private fun generateFakeSites(): List<Site> = listOf(
            Site.FixedSite(title = "Fixed 1", url = "https://m.facebook.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_facebook.png"),
            Site.FixedSite(title = "Fixed 2", url = "https://m.facebook.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_facebook.png"),
            Site.FixedSite(title = "Fixed 3", url = "https://m.facebook.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_facebook.png"),
            Site.FixedSite(title = "Fixed 4", url = "https://m.facebook.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_facebook.png"),
            Site.RemovableSite(title = "YouTube 5", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 6", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 7", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 8", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 9", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 10", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 11", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 12", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 13", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 14", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 15", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png"),
            Site.RemovableSite(title = "YouTube 16", url = "https://m.youtube.com/", iconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + "ic_youtube.png")
    )

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