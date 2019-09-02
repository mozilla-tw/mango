package org.mozilla.rocket.shopping.search.data

import android.content.Context
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.ui.adapter.SiteViewHolder

class ShoppingSearchSiteRepository(appContext: Context) {

    private val preference = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val sitesLiveData = MutableLiveData<Result<MutableList<PreferenceSite>>>()
    private val mockPreferenceSiteList = mutableListOf(
        PreferenceSite("Lazada", "https://www.lazada.co.id", "https://www.lazada.co.id", isChecked = false, order = 5),
        PreferenceSite("Bukalapak", "https://www.bukalapak.com", "https://www.bukalapak.com", isChecked = false, order = 0),
        PreferenceSite("Tokopedia", "https://www.tokopedia.com/search?st=product&q=%s", "https://www.tokopedia.com", isChecked = false, order = 1),
        PreferenceSite("JD.ID", "https://www.jd.id/search?keywords=%s", "https://www.jd.id", isChecked = false, order = 2),
        PreferenceSite("Shopee", "https://shopee.co.id", "https://shopee.co.id", isChecked = false, order = 3),
        PreferenceSite("BliBli", "https://www.blibli.com", "https://www.blibli.com", isChecked = false, order = 4)
    )

    suspend fun fetchSites(): MutableLiveData<Result<MutableList<PreferenceSite>>> {
        val siteArrayString = preference.getString(KEY_SHOPPING_SEARCH_SITE, "")

        if (siteArrayString.isNullOrEmpty()) {
            sitesLiveData.postValue(Result.Success(mockPreferenceSiteList))
        } else {
            val list = mutableListOf<PreferenceSite>()
            val siteArray = JSONArray(siteArrayString)
            for (i in 0 until siteArray.length()) {
                list.add(PreferenceSite(siteArray.optJSONObject(i)))
            }
            sitesLiveData.postValue(Result.Success(list))
        }
        return sitesLiveData
    }

    fun updatePreferenceSiteData(siteList: MutableList<SiteViewHolder.PreferencesUiModel>) {
        val siteJsonArray = JSONArray()
        for (i in 0 until siteList.size) {
            val site = siteList[i]
            site.data.order = i
            siteJsonArray.put(site.data.toJson())
        }
        sitesLiveData.postValue(Result.Success(siteList.map {
            it.data
        }.toMutableList()))
        preference.edit().putString(KEY_SHOPPING_SEARCH_SITE, siteJsonArray.toString()).apply()
    }

    data class PreferenceSite(
        val title: String,
        val searchUrl: String,
        val displayUrl: String,
        var isChecked: Boolean,
        var order: Int
    ) {
        constructor(obj: JSONObject) : this(
            obj.optString("title"),
            obj.optString("searchUrl"),
            obj.optString("searchUrl"),
            obj.optBoolean("isChecked"),
            obj.optInt("order")
        )

        fun toJson(): JSONObject {
            val obj = JSONObject()
            obj.put("title", title)
            obj.put("searchUrl", searchUrl)
            obj.put("displayUrl", displayUrl)
            obj.put("isChecked", isChecked)
            obj.put("order", order)
            return obj
        }
    }

    data class Site(
        val title: String,
        val searchUrl: String
    )

    companion object {
        const val PREF_NAME = "shopping_search"
        const val KEY_SHOPPING_SEARCH_SITE = "shopping_search_site"
    }
}