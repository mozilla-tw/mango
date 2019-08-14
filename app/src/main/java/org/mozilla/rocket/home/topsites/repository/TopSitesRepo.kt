package org.mozilla.rocket.home.topsites.repository

import android.content.Context
import android.preference.PreferenceManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONException
import org.mozilla.focus.home.HomeFragment
import org.mozilla.focus.utils.TopSitesUtils
import org.mozilla.rocket.home.pinsite.PinSiteManager
import org.mozilla.rocket.home.topsites.Site

open class TopSitesRepo(
    private val appContext: Context,
    private val pinSiteManager: PinSiteManager
) {

    private val _topSites = MutableLiveData<List<Site>>()
    val topSites: LiveData<List<Site>>
        get() = _topSites

    init {
        _topSites.value = getDefaultTopSites()
    }

    private fun getDefaultTopSites(): List<Site> {
        // use different implementation to provide default top sites.
        val jsonString = getDefaultTopSitesJsonString()

        // if no default sites data in SharedPreferences, load data from assets.
        val jsonArray = if (jsonString != null) {
            try {
                JSONArray(jsonString)
            } catch (e: JSONException) {
                e.printStackTrace()
                null
            }
        } else {
            TopSitesUtils.getDefaultSitesJsonArrayFromAssets(appContext)
        }

        return TopSitesUtils.paresJsonToList(appContext, jsonArray).toRemovableSite(pinSiteManager)
    }

    // open for mocking during testing
    open fun getDefaultTopSitesJsonString(): String? {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
                .getString(HomeFragment.TOPSITES_PREF, null)
    }

    fun isPinEnabled(): Boolean = pinSiteManager.isEnabled()

    fun pin(site: Site) {
        pinSiteManager.pin(site.toSiteModel())
        // TODO: notify data changed
    }

    fun remove(site: Site) {
        // TODO:
//        if (site.getId() < 0) {
//            presenter.removeSite(site)
//            removeDefaultSites(site)
//            TopSitesUtils.saveDefaultSites(getContext(), this@HomeFragment.orginalDefaultSites)
//            refreshTopSites()
//            TelemetryWrapper.removeTopSite(true)
//        } else {
//            site.setViewCount(1)
//            BrowsingHistoryManager.getInstance().updateLastEntry(site, mTopSiteUpdateListener)
//            TelemetryWrapper.removeTopSite(false)
//        }
        pinSiteManager.unpinned(site.toSiteModel())
    }
}

private fun List<org.mozilla.focus.history.model.Site>.toRemovableSite(pinSiteManager: PinSiteManager): List<Site> =
        map { it.toRemovableSite(pinSiteManager) }

private fun org.mozilla.focus.history.model.Site.toRemovableSite(pinSiteManager: PinSiteManager): Site =
        Site.RemovableSite(
                id = id,
                title = title,
                url = url,
                iconUri = favIconUri,
                viewCount = viewCount,
                lastViewTimestamp = lastViewTimestamp,
                isDefault = isDefault,
                isPinned = pinSiteManager.isPinned(this)
        )

private fun Site.toSiteModel(): org.mozilla.focus.history.model.Site =
        org.mozilla.focus.history.model.Site(
                id,
                title,
                url,
                viewCount,
                lastViewTimestamp,
                iconUri
        ).apply {
            isDefault = when (this@toSiteModel) {
                is Site.FixedSite -> true
                is Site.RemovableSite -> this@toSiteModel.isDefault
            }
        }
