package org.mozilla.rocket.home.topsites.repository

import android.content.Context
import android.preference.PreferenceManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONException
import org.mozilla.focus.home.HomeFragment
import org.mozilla.focus.utils.TopSitesUtils
import org.mozilla.rocket.home.topsites.Site

open class TopSitesRepo(private val appContext: Context) {

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

        return TopSitesUtils.paresJsonToList(appContext, jsonArray).toRemovableSite()
    }

    // open for mocking during testing
    open fun getDefaultTopSitesJsonString(): String? {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
                .getString(HomeFragment.TOPSITES_PREF, null)
    }

    private fun List<org.mozilla.focus.history.model.Site>.toRemovableSite(): List<Site> = map { it.toRemovableSite() }

    private fun org.mozilla.focus.history.model.Site.toRemovableSite(): Site =
            Site.RemovableSite(
                id = id,
                title = title,
                url = url,
                iconUri = favIconUri,
                isDefault = false
            )
}